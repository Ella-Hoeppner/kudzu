(ns kudzu.chunks.sdf
  (:require [kudzu.chunks.raytracing :refer [ray-chunk]]
            [kudzu.core :refer [combine-chunks]]
            [clojure.walk :refer [postwalk-replace]]))

; SDFs based on https://iquilezles.org/articles/distfunctions/
(def sphere-sdf-chunk
  '{:functions {sd-sphere
                (float
                 [pos vec3
                  radius float]
                 (- (length pos) radius))}})

(def box-sdf-chunk
  '{:functions {sdf-box
                (float
                 [pos vec3
                  dims vec3]
                 (=vec3 q (- (abs pos) dims))
                 (+ (length (max q 0))
                    (min (max q.x (max q.y q.z)) 0)))}})

(def box-frame-sdf-chunk
  '{:functions {sd-box-frame
                (float
                 [pos vec3
                  dims vec3
                  thickness float]
                 (= pos (- (abs pos) dims))
                 (=vec3 q (- (abs (+ pos thickness)) thickness))
                 (min
                  (min (+ (length (max (vec3 pos.x q.y q.z) 0))
                          (min (max pos.x (max q.y q.z)) 0))
                       (+ (length (max (vec3 q.x pos.y q.z) 0))
                          (min (max q.x (max pos.y q.z)) 0)))
                  (+ (length (max (vec3 q.x q.y pos.z) 0))
                     (min (max q.x (max q.y pos.z)) 0))))}})

(def torus-sdf-chunk
  '{:functions {sd-torus (float
                          [pos vec3
                           radius float
                           thickness float]
                          (=vec2 q (vec2 (- (length pos.xz) radius)
                                         pos.y))
                          (- (length q) thickness))}})

(def capped-torus-sdf-chunk
  '{:functions {sd-capped-torus
                (float
                 [pos vec3
                  sc vec2
                  ra float
                  rb float]
                 (= pos.x (abs pos.x))

                 (=float k (if (> (* sc.y pos.x)
                                  (* sc.x pos.y))
                             (dot pos.xy sc)
                             (length pos.xy)))

                 (- (sqrt (- (+ (dot pos pos)
                                (* ra ra))
                             (* 2 ra k))) rb))}})

(def link-sdf-chunk
  '{:functions {sd-link
                (float
                 [pos vec3
                  le float
                  r1 float
                  r2 float]
                 (=vec3 q (vec3 pos.x
                                (max (- (abs pos.y) le)
                                     0)
                                pos.z))

                 (- (length (- (vec2 (- (length q.xy)
                                        r1) q.z)
                               r2))))}})

(def capped-cylinder-sdf-chunk
  '{:functions
    {sd-cylinder
     (float
      [pos vec3
       a vec3
       b vec3
       radius float]

      (=vec3 ba (- b a))
      (=vec3 pa (- pos a))
      (=float baba (dot ba ba))
      (=float paba (dot pa ba))
      (=float x (- (length
                    (- (* pa baba)
                       (* ba paba)))
                   (* radius baba)))
      (=float y (- (abs (- paba (* baba 0.5)))
                   (* baba 0.5)))
      (=float x2 (* x x))
      (=float y2 (* y y baba))
      (=float d (if (< (max x y) 0)
                  (* -1 (min x2 y2))
                  (+ (if (> x 0) x2 0)
                     (if (> y 0) y2 0))))

      (/ (* (sign d)
            (sqrt (abs d)))
         baba))}})

; angle = sin/cos  of angle
(def cone-sdf-chunk
  '{:functions {sd-cone (float
                         [pos vec3
                          angle vec2
                          height float]
                         (=vec2 q (* height (vec2 (/ angle.x angle.y) -1)))

                         (=vec2 w (vec2 (length pos.xz) pos.y))
                         (=vec2 a (- w (*  q (clamp (/ (dot w q)
                                                       (dot q q))
                                                    0 1))))
                         (=vec2 b (- w (* q (vec2 (clamp (/ w.x q.x) 0 1) 1))))

                         (=float k (sign q.y))
                         (=float d (min (dot a a) (dot b b)))
                         (=float s (max (* k (- (* w.x  q.y) (* w.y q.x)))
                                        (* k (-  w.y q.y))))

                         (* (sqrt d) (sign s)))}})

(def hex-prism-sdf-chunk
  '{:functions {sd-hexagonal-prism
                (float
                 [pos vec3
                  h vec2]
                 (=vec3 k (vec3 -0.8660254 0.5 0.57735))
                 (-= pos.xy (* 2 (min (dot k.xy  pos.xy) 0) k.xy))

                 (=vec2 d (vec2 (* (length (- pos.xy
                                              (vec2 (clamp pos.x
                                                           (* (- 0 k.z) h.x)
                                                           (* k.z h.x))
                                                    h.x)))
                                   (sign (- pos.y h.x)))
                                (- pos.z h.y)))

                 (+ (min (max d.x d.y) 0)
                    (length (max d 0))))}})

(def capsule-sdf-chunk
  '{:functions {sd-capsule
                (float
                 [pos vec3
                  a vec3
                  b vec3
                  r float]
                 (=vec3 pa (- pos a))
                 (=vec3 ba (- b a))

                 (=float h (clamp (/ (dot pa ba) (dot ba ba)) 0 1))

                 (- (length (- pa (* ba h))) r))}})


;TODO fix this, cannot figure out what's wrong w it -Fay
#_(def octahedron-sdf-chunk
    '{:functions {sd-octahedron
                  (float
                   [pos vec3
                    s float]
                   (= pos (abs pos))

                   (=float m (- (+ pos.x pos.y pos.z) s))
                   (=vec3 q (vec3 0))
                   ("if" (< (* pos.x 3) m)
                         (= q pos))
                   ("else if" (< (* pos.y 3) m)
                              (= q pos.yzx))
                   ("else if" (< (* pos.z 3) m)
                              (= q pos.zxy))
                   ("else" (return (* m 0.57735027)))

                   (=float k (clamp (* 0.5 (- q.z (+ q.y s)))
                                    0
                                    s))
                   (length (vec3 q.x
                                 (- q.y (+ s k))
                                 (- q.z k))))}})

(def pyramid-sdf-chunk '{:functions
                         {sd-pyramid
                          (float
                           [pos vec3
                            h float]
                           (=float m2 (+ (* h h) 0.25))

                           (= pos.xz (abs pos.xz))
                           (= pos.xz (if (> pos.z pos.x)
                                       pos.zx
                                       pos.xz))
                           (-= pos.xz 0.5)

                           (=vec3 q  (vec3 pos.z
                                           (- (* h pos.y)
                                              (* 0.5 pos.x))
                                           (+ (* h pos.x)
                                              (* 0.5 pos.y))))

                           (=float s (max (* -1 q.x) 0))
                           (=float t (clamp (/ (- q.y (* 0.5 pos.z))
                                               (+ m2 0.25))
                                            0 1))

                           (=float a (+ (* m2
                                           (+ q.x s)
                                           (+ q.x s))
                                        (* q.y q.y)))
                           (=float b (+ (* m2
                                           (+ q.x (* 0.5 t))
                                           (+ q.x (* 0.5 t)))
                                        (* (- q.y (* m2 t))
                                           (- q.y (* m2 t)))))

                           (=float d2 (if (> (min q.y (- (* -1 q.x m2)
                                                         (* q.y 0.5)))
                                             0)
                                        0
                                        (min a b)))

                           (sqrt (* (/ (+ d2 (* q.z q.z)) m2)
                                    (sign (max q.z (- 0 pos.y))))))}})


; todo: replace with a macro
#_(defn get-multi-dimension-elongate-chunk [sdf-fn-name]
  (postwalk-replace
   {:fn-name sdf-fn-name}
   '{:functions {opElongate
                 (float
                  [pos vec3
                   shapePos vec3
                   h vec3]
                  (= pos (- pos shapePos))
                  (=vec3 q (- pos (clamp p
                                         (* -1 h)
                                         h)))

                  (:fn-name q  #_(args here)))}}))

; sdf operations
(def smooth-union-chunk
  '{:functions
    {smoothUnion
     (float
      [d1 float
       d2 float
       k float]
      (=float h (clamp (+ (/ (* 0.5 (- d2 d1)) k) 0.5) 0 1))
      (- (mix d2 d1 h) (* k h (- 1 h))))}})

(def smooth-intersectioon-chunk
  '{:functions
    {smoothIntersection
     (float
      [d1 float
       d2 float
       k float]
      (=float h (clamp (- (/ (* 0.5 (- d2 d1)) k) 0.5) 0 1))
      (+ (mix d2 d1 h) (* k h (- 1 h))))}})

(def smooth-subtraction-chunk
  '{:functions
    {smoothSubtraction
     (float
      [d1 float
       d2 float
       k float]
      (=float h (clamp (- (/ (* 0.5 (+ d2 d1)) k) 0.5) 0 1))
      (+ (mix d2 d1 h) (* k h (- 1 h))))}})

; transformations
(def onion-chunk
  '{:functions
    {onion
     (float
      [d float
       h float]
      (- (abs d) h))}})

(def twist-x-chunk
  '{:functions
    {twistX
     (float
      [pos vec3
       k float]
      (=float c (cos (* k pos.x)))
      (=float s (sin (* k pos.x)))
      (=mat2 m (mat2 c (- 0 s) s c))
      (=vec3 q (vec3 (* m pos.yz) pos.x))
      q)}})

(def twist-y-chunk
  '{:functions {twistY
                (float
                 [pos vec3
                  k float]
                 (=float c (cos (* k pos.y)))
                 (=float s (sin (* k pos.y)))
                 (=mat2 m (mat2 c (- 0 s) s c))
                 (=vec3 q (vec3 (* m pos.xz) pos.y))
                 q)}})
