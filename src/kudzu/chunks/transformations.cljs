(ns kudzu.chunks.transformations)

(def x-rotation-matrix-chunk
  '{:functions
    {x-rotation-matrix
     (mat3
      [angle float]
      (=float c (cos angle))
      (=float s (sin angle))
      (mat3 1 0 0
            0 c (- s)
            0 s c))}})

(def y-rotation-matrix-chunk
  '{:functions
    {y-rotation-matrix
     (mat3
      [angle float]
      (=float c (cos angle))
      (=float s (sin angle))
      (mat3 c 0 s
            0 1 0
            (- s) 0 c))}})

(def z-rotation-matrix-chunk
  '{:functions
    {z-rotation-matrix
     (mat3
      [angle float]
      (=float c (cos angle))
      (=float s (sin angle))
      (mat3 c s 0
            (- s) c 0
            0 0 1))}})

(def axis-rotation-chunk
  '{:functions
    {axis-rotation-matrix
     (mat3
      [axis vec3
       angle float]
      (= axis (normalize axis))
      (=float s (sin angle))
      (=float c (cos angle))
      (=float neg-c (- 1 c))

      (=float neg-cxy (* neg-c axis.x axis.y))
      (=float neg-czx (* neg-c axis.z axis.x))
      (=float neg-cyz (* neg-c axis.y axis.z))

      (=float zs (* axis.z s))
      (=float ys (* axis.y s))
      (=float xs (* axis.x s))

      (mat3 (+ (* neg-c axis.x axis.x) c)
            (- neg-cxy zs)
            (+ neg-czx ys)

            (+ neg-cxy zs)
            (+ (* neg-c axis.y axis.y) c)
            (- neg-cyz xs)

            (- neg-czx ys)
            (+ neg-cyz xs)
            (+ (* neg-c axis.z axis.z) c)))}})
