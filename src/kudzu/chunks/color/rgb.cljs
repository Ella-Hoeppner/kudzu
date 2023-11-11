(ns kudzu.chunks.color.rgb)

(def lrgb-srgb-chunk
  '{:functions
    {lrgb->srgb
     [(float
       [c float]
       (if (>= 0.0031308 c)
         (* 12.92 c)
         (- (* 1.055 (pow c 0.4166666666666667))
            0.055)))
      (vec3
       [rgb vec3]
       (vec3 (lrgb->srgb rgb.r)
             (lrgb->srgb rgb.g)
             (lrgb->srgb rgb.b)))]
     srgb->lrgb
     [(float
       [c float]
       (if (< 0.04045 c)
         (pow (/ (+ c 0.055) 1.055)
              2.4)
         (/ c 12.92)))
      (vec3
       [rgb vec3]
       (vec3 (srgb->lrgb rgb.r)
             (srgb->lrgb rgb.g)
             (srgb->lrgb rgb.b)))]}})
