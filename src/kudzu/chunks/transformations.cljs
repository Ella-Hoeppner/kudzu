(ns kudzu.chunks.transformations)

(def x-rotation-matrix-chunk
  '{:functions
    {xRotationMatrix
     (mat3
      [angle float]
      (=float c (cos angle))
      (=float s (sin angle))
      (mat3 1 0 0
            0 c (- s)
            0 s (- c)))}})

(def y-rotation-matrix-chunk
  '{:functions
    {yRotationMatrix
     (mat3
      [angle float]
      (=float c (cos angle))
      (=float s (sin angle))
      (mat3 c 0 s
            0 1 0
            (- s) 0 c))}})

(def z-rotation-matrix-chunk
  '{:functions
    {zRotationMatrix
     (mat3
      [angle float]
      (=float c (cos angle))
      (=float s (sin angle))
      (mat3 c s 0
            (- s) c 0
            0 0 1))}})

(def axis-rotation-chunk
  '{:functions
    {axisRotationMatrix
     (mat3
      [axis vec3
       angle float]
      (= axis (normalize axis))
      (=float s (sin angle))
      (=float c (cos angle))
      (=float -c (- 1 c))

      (=float -cxy (* -c axis.x axis.y))
      (=float -czx (* -c axis.z axis.x))
      (=float -cyz (* -c axis.y axis.z))

      (=float zs (* axis.z s))
      (=float ys (* axis.y s))
      (=float xs (* axis.x s))

      (mat3 (+ (* -c axis.x axis.x) c)
            (- -cxy zs)
            (+ -czx ys)

            (+ -cxy zs)
            (+ (* -c axis.y axis.y) c)
            (- -cyz xs)
            
            (- -czx ys)
            (+ -cyz xs)
            (+ (* -c axis.z axis.z) c)))}})
