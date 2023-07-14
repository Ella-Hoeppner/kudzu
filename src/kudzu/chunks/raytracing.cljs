(ns kudzu.chunks.raytracing
  (:require [util :as u]
            [clojure.walk :refer [postwalk-replace]]
            [kudzu.core :refer [combine-chunks]]))

(def ray-chunk
  '{:structs {Ray [pos vec3
                   dir vec3]}})

(def plane-intersection-chunk
  (combine-chunks ray-chunk
                  '{:functions {findPlaneIntersection
                                (float
                                 [ray Ray
                                  planeRay Ray]
                                 (/ (dot (- planeRay.pos ray.pos) planeRay.dir)
                                    (dot ray.dir planeRay.dir)))}}))

(def sphere-intersection-chunk
  (combine-chunks ray-chunk
                  '{:functions
                    {findSphereIntersections
                     (vec2
                      [ray Ray
                       sphereCenter vec3
                       sphereRadius float]
                      (=vec3 offset (- ray.pos sphereCenter))
                      (=float halfB (dot offset ray.dir))
                      (=float c (- (dot offset offset)
                                   (* sphereRadius sphereRadius)))
                      (=float discriminant (- (* halfB halfB) c))
                      (:if (> discriminant 0)
                           (=float discriminantSqrt (sqrt discriminant))
                           (return (- 0
                                      (vec2 (+ halfB discriminantSqrt)
                                            (- halfB discriminantSqrt)))))
                      (vec2 0))}}))

; based on https://iquilezles.org/articles/boxfunctions/
(def box-intersection-chunk
  (combine-chunks
   ray-chunk
   '{:structs {BoxIntersection [hit bool
                                frontDist float
                                backDist float
                                frontNorm vec3]}
     :functions {findBoxIntersection
                 (BoxIntersection
                  [ray Ray
                   pos vec3
                   size vec3]
                  (=vec3 m (/ 1 ray.dir))
                  (=vec3 n (* m (- ray.pos pos)))
                  (=vec3 k (* (abs m) size))
                  (=vec3 t1 (- 0 (+ n k)))
                  (=vec3 t2 (- k n))

                  (=float tN (max (max t1.x t1.y) t1.z))
                  (=float tF (min (min t2.x t2.y) t2.z))
                  (:if (|| (> tN tF)
                           (< tF 0))
                       (return (BoxIntersection "false"
                                                0
                                                0
                                                (vec3 0))))
                  (BoxIntersection "true"
                                   tN
                                   tF
                                   (- 0
                                      (* (sign ray.dir)
                                         (step t1.yzx t1.xyz)
                                         (step t1.zxy t1.xyz)))))}}))

(def plane-sdf-chunk
  (combine-chunks ray-chunk
                  '{:functions {sdPlane
                                (float
                                 [pos vec3
                                  planeRay Ray]
                                 (dot (- planeRay.pos pos) planeRay.dir))}}))

(def raymarch-chunk
  (combine-chunks
   ray-chunk
   {:macros
    {'raymarch
     (fn [sdf-name
          ray
          maxDistance
          & [{:keys [step-factor
                     max-steps
                     termination-threshold]
              :or {step-factor 1
                   max-steps 1024
                   termination-threshold 0.0001}}]]
       (let [fn-name (gensym 'march)]
         (u/unquotable
          '{:chunk
            {:functions
             {~fn-name
              (float
               [ray Ray
                maxDistance float]
               (=float t 0)
               (:for (=int i "0") (< i ~(str max-steps)) (++ i)
                     (=float distanceEstimate
                             (~sdf-name (+ ray.pos (* t ray.dir))))
                     (:if (< (abs distanceEstimate)
                             ~termination-threshold)
                          (return t))
                     (+= t (* distanceEstimate ~step-factor))
                     (:if (> t maxDistance) "break"))
               -1)}}
            :expression ~(list fn-name ray maxDistance)})))}}))

(defn create-sdf-normal-chunk [sdf-name & [{:keys [sample-distance
                                                   fn-name]
                                            :or {sample-distance 0.001
                                                 fn-name 'sdfNorm}}]]
  (postwalk-replace
   {:sample-distance sample-distance
    :sdf-name sdf-name
    :fn-name fn-name}
   '{:functions
     {:fn-name
      (vec3
       [pos vec3]
       (normalize
        (vec3 (- (:sdf-name (+ pos (vec3 :sample-distance 0 0)))
                 (:sdf-name (- pos (vec3 :sample-distance 0 0))))
              (- (:sdf-name (+ pos (vec3 0 :sample-distance 0)))
                 (:sdf-name (- pos (vec3 0 :sample-distance 0))))
              (- (:sdf-name (+ pos (vec3 0 0 :sample-distance)))
                 (:sdf-name (- pos (vec3 0 0 :sample-distance)))))))}}))

(def perspective-camera-chunk
  (combine-chunks ray-chunk
                  '{:functions
                    {cameraRay
                     (Ray
                      [screenPos vec2
                       camTarget vec3
                       camPos vec3
                       focalDist float]
                      (=vec3 camDir (normalize (- camTarget camPos)))
                      (=vec3 camRight (normalize (cross camDir (vec3 0 1 0))))
                      (=vec3 camUp (cross camRight camDir))

                      (=vec3 filmPos (+ (* camDir focalDist)
                                        (* (- screenPos.x 0.5) camRight)
                                        (* (- screenPos.y 0.5) camUp)))

                      (Ray camPos (normalize filmPos)))}}))

(defn get-voxel-intersection-chunk
  [& [{:keys [max-voxel-steps
              return-type
              default-return-expression
              hit-expression]
       :or {max-voxel-steps 1000000
            return-type 'VoxelIntersection
            default-return-expression '(VoxelIntersection "false"
                                                          (ivec3 "0")
                                                          (vec3 0)
                                                          (vec3 0))
            hit-expression '((return voxelIntersection))}}]]
  (combine-chunks
   ray-chunk
   (postwalk-replace
    {:max-voxel-steps (str max-voxel-steps)
     :return-type return-type
     :default-return-expression default-return-expression
     :voxel-hit-expression
     (concat
      (list :if
            '(voxelFilled voxelCoords)
            '(=VoxelIntersection voxelIntersection
                                 (VoxelIntersection "true"
                                                    voxelCoords
                                                    (+ ray.pos
                                                       (* ray.dir dist))
                                                    norm)))
      hit-expression)}
    '{:structs {VoxelIntersection [hit bool
                                   gridPos ivec3
                                   pos vec3
                                   norm vec3]}
      :functions
      {findVoxelIntersection
       (:return-type
        [ray Ray
         maxDist float]
        (=ivec3 voxelCoords (ivec3 (floor ray.pos)))
        (=vec3 innerCoords (fract ray.pos))

        (=ivec3 step (ivec3 (sign ray.dir)))
        (=vec3 delta (/ (vec3 step) ray.dir))

        (=vec3 tMax (* delta
                       (vec3 (if (> ray.dir.x "0.0")
                               (- "1.0" innerCoords.x)
                               innerCoords.x)
                             (if (> ray.dir.y "0.0")
                               (- "1.0" innerCoords.y)
                               innerCoords.y)
                             (if (> ray.dir.z "0.0")
                               (- "1.0" innerCoords.z)
                               innerCoords.z))))

        (=vec3 norm (vec3 0))
        (=int maxVoxelSteps :max-voxel-steps)
        ("for(int i=0;i<maxVoxelSteps;i++)"
         (=vec3 t
                (min (/ (- (vec3 voxelCoords) ray.pos) ray.dir)
                     (/ (- (vec3 (+ (vec3 voxelCoords) 1)) ray.pos) ray.dir)))
         (=float dist (max (max t.x t.y) t.z))
         (:if (>= dist maxDist) (return :default-return-expression))
         :voxel-hit-expression
         (:if (< tMax.x tMax.y)
              (:if (< tMax.z tMax.x)
                   (+= tMax.z delta.z)
                   (+= voxelCoords.z step.z)
                   (= norm (vec3 0 0 (- "0.0" (float step.z)))))
              (:else
               (+= tMax.x delta.x)
               (+= voxelCoords.x step.x)
               (= norm (vec3 (- "0.0" (float step.x)) 0 0))))
         (:else (:if (< tMax.z tMax.y)
                     (+= tMax.z delta.z)
                     (+= voxelCoords.z step.z)
                     (= norm (vec3 0 0 (- "0.0" (float step.z)))))
                (:else
                 (+= tMax.y delta.y)
                 (+= voxelCoords.y step.y)
                 (= norm (vec3 0 (- "0.0" (float step.y)) 0)))))
        :default-return-expression)}})))

(defn get-column-intersection-chunk [return-type
                                     default-return-expression
                                     hit-expression
                                     & [max-steps]]
  (combine-chunks
   ray-chunk
   (postwalk-replace
    {:max-steps (str (or max-steps 1000000))
     :return-type return-type
     :default-return-expression default-return-expression
     :column-hit-expression
     (concat
      '(:if (columnFilled gridCoords))
      hit-expression)}
    '{:functions
      {findColumnIntersection
       (:return-type
        [ray Ray
         maxDist float]

        (=vec2 rayPos ray.pos.xy)
        (=vec2 rayDir (normalize ray.dir.xy))

        (=ivec2 gridCoords (ivec2 (floor rayPos)))
        (=vec2 innerCoords (fract rayPos))

        (=ivec2 step (ivec2 (sign rayDir)))
        (=vec2 delta (/ (vec2 step) rayDir))

        (=vec2 tMax (* delta
                       (vec2 (if (> rayDir.x "0.0")
                               (- "1.0" innerCoords.x)
                               innerCoords.x)
                             (if (> rayDir.y "0.0")
                               (- "1.0" innerCoords.y)
                               innerCoords.y))))

        (=vec3 norm (vec3 0))
        (=int maxSteps :max-steps)
        ("for(int i=0;i<maxSteps;i++)"
         (=vec2 t
                (min (/ (- (vec2 gridCoords) rayPos) rayDir)
                     (/ (- (vec2 (+ (vec2 gridCoords) 1)) rayPos) rayDir)))
         (=float dist (max t.x t.y))
         (:if (>= dist maxDist) (return :default-return-expression))
         :column-hit-expression
         (:if (< tMax.x tMax.y)
              (+= tMax.x delta.x)
              (+= gridCoords.x step.x)
              (= norm (vec3 (- "0.0" (float step.x)) 0 0)))
         (:else (+= tMax.y delta.y)
                (+= gridCoords.y step.y)
                (= norm (vec3 0 (- "0.0" (float step.y)) 0))))
        :default-return-expression)}})))

; based on https://iquilezles.org/articles/intersectors/
(def capsule-intersection-chunk
  '{:functions
    {findCapsuleDist
     (float
      [ray Ray
       point1 vec3
       point2 vec3
       radius float]
      (=vec3 diff (- point2 point1))
      (=vec3 offset (- ray.pos point1))

      (=float baba (dot diff diff))
      (=float bard (dot diff ray.dir))
      (=float baoa (dot diff offset))
      (=float rdoa (dot ray.dir offset))
      (=float oaoa (dot offset offset))

      (=float a (- baba (* bard bard)))
      (=float b (- (* baba rdoa) (* baoa bard)))
      (=float c (- (* baba oaoa)
                   (+ (* baoa baoa)
                      (* radius radius baba))))
      (=float h (- (* b b) (* a c)))
      (:if (>= h 0)
           (=float t (/ (- 0 (+ b (sqrt h))) a))
           (=float y (+ baoa (* t bard)))
           (:if (&& (> y 0) (< y baba)) (return t))
           (=vec3 oc (if (<= y 0)
                       offset
                       (- ray.pos point2)))
           (= b (dot ray.dir oc))
           (= c (- (dot oc oc) (* radius radius)))
           (= h (- (* b b) c))
           (:if (> h 0) (return (- 0 (+ b (sqrt h))))))
      -1)
     capsuleNorm
     (vec3
      [pos vec3
       point1 vec3
       point2 vec3
       radius float]
      (=vec3 diff (- point2 point1))
      (=vec3 offset (- pos point1))
      (=float h (clamp (/ (dot offset diff)
                          (dot diff diff))
                       0
                       1))
      (/ (- offset (* h diff))
         radius))}})

; based on https://iquilezles.org/articles/intersectors/
(def cylinder-intersection-chunk
  '{:structs
    {CylinderIntersection [hit bool
                           dist float
                           norm vec3]}
    :functions
    {findCylinderIntersection
     (CylinderIntersection
      [ray Ray
       point1 vec3
       point2 vec3
       radius float]
      (=vec3 diff (- point2 point1))
      (=vec3 offset (- ray.pos point1))

      (=float baba (dot diff diff))
      (=float bard (dot diff ray.dir))
      (=float baoc (dot diff offset))

      (=float k2 (- baba (* bard bard)))
      (=float k1 (- (* baba (dot offset ray.dir))
                    (* baoc bard)))
      (=float k0 (- (* baba (dot offset offset))
                    (+ (* baoc baoc)
                       (* radius
                          radius
                          baba))))

      (=float h (- (* k1 k1) (* k2 k0)))
      (:if (< h 0)
           (return (CylinderIntersection "false"
                                         0
                                         (vec3 0))))
      (= h (sqrt h))
      (=float t (/ (- 0 (+ k1 h)) k2))

      (=float y (+ baoc (* t bard)))
      (:if (&& (>= t 0) (> y 0) (< y baba))
           (return (CylinderIntersection "true"
                                         t
                                         (/ (- (+ offset (* t ray.dir))
                                               (/ (* diff y) baba))
                                            radius))))

      (= t (/ (- (if (< y 0) 0 baba) baoc) bard))
      (:if (&& (>= t 0)
               (< (abs (+ k1 (* k2 t))) h))
           (return (CylinderIntersection "true"
                                         t
                                         (/ (* diff (sign y))
                                            (sqrt baba)))))

      (CylinderIntersection "false"
                            0
                            (vec3 0)))}})
