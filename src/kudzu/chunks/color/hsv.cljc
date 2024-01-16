(ns kudzu.chunks.color.hsv)

; derived from https://www.shadertoy.com/view/4dKcWK
(def rgb->hsv-chunk
  '{:functions {rgb->hsv
                (vec3
                 [rgb vec3]
                 (=vec4 p (if (< rgb.g rgb.b)
                            (vec4 rgb.bg -1 0.666666666)
                            (vec4 rgb.gb 0 -0.333333333)))
                 (=vec4 q (if (< rgb.r p.x)
                            (vec4 p.xyw rgb.r)
                            (vec4 rgb.r p.yzx)))
                 (=float c (- q.x (min q.w q.y)))
                 (=float h (abs (+ (/ (- q.w q.y)
                                      (+ 0.0000001
                                         (* 6 c)))
                                   q.z)))
                 (=vec3 hcv (vec3 h c q.x))
                 (=float s (/ hcv.y (+ hcv.z 0.0000001)))
                 (vec3 hcv.x s hcv.z))}})

; derived from https://www.shadertoy.com/view/MsS3Wc
(def hsv->rgb-chunk
  '{:functions {hsv->rgb
                (vec3
                 [color vec3]
                 (=vec3 rgb (clamp (- (abs (- (mod (+ (* color.x 6)
                                                      (vec3 0 4 2))
                                                   6)
                                              3))
                                      1)
                                   0
                                   1))
                 (* color.z (mix (vec3 1)
                                 rgb
                                 color.y)))}})

; derived from https://www.shadertoy.com/view/MsS3Wc
(def cubic-hsv->rgb-chunk
  '{:functions {hsv->rgb
                (vec3
                 [color vec3]
                 (=vec3 rgb (clamp (- (abs (- (mod (+ (* color.x 6)
                                                      (vec3 0 4 2))
                                                   6)
                                              3))
                                      1)
                                   0
                                   1))
                 (= rgb (* rgb rgb (- 3 (* 2 rgb))))
                 (* color.z (mix (vec3 1)
                                 rgb
                                 color.y)))}})

; derived from @Frizzil's comment on https://www.shadertoy.com/view/MsS3Wc
(def quintic-hsv->rgb-chunk
  '{:functions {hsv->rgb
                (vec3
                 [color vec3]
                 (=vec3 rgb (clamp (- (abs (- (mod (+ (* color.x 6)
                                                      (vec3 0 4 2))
                                                   6)
                                              3))
                                      1)
                                   0
                                   1))
                 (= rgb (* rgb
                           rgb
                           rgb
                           (+ 10 (* rgb (- (* rgb 6) 15)))))
                 (* color.z (mix (vec3 1)
                                 rgb
                                 color.y)))}})

; derived from https://www.shadertoy.com/view/MsS3Wc
(def cosine-hsv->rgb-chunk
  '{:functions {hsv->rgb
                (vec3
                 [color vec3]
                 (=vec3 rgb (clamp (- (abs (- (mod (+ (* color.x 6)
                                                      (vec3 0 4 2))
                                                   6)
                                              3))
                                      1)
                                   0
                                   1))
                 (= rgb (+ 0.5 (* -0.5 (cos (* rgb 3.14159265359)))))
                 (* color.z (mix (vec3 1)
                                 rgb
                                 color.y)))}})
