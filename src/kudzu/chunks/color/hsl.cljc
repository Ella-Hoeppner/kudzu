(ns kudzu.chunks.color.hsl)

(def hsl->rgb-chunk
  '{:functions {hsl->rgb
                (vec3
                 [color vec3]
                 (=vec3 chroma
                        (clamp (- 2
                                  (abs
                                   (- (mod (+ (* color.x 6)
                                              (vec3 3 1 5))
                                           6)
                                      3)))
                               0
                               1))
                 (mix (mix (vec3 0)
                           (mix (vec3 0.5) chroma color.y)
                           (clamp (* color.z 2) 0 1))
                      (vec3 1)
                      (clamp (- (* color.z 2) 1)
                             0
                             1)))}})
