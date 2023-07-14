(ns kudzu.chunks.color)

(def hsl-to-rgb-chunk
  '{:functions {hsl2rgb
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

; derived from https://www.shadertoy.com/view/4dKcWK
(def rgb-to-hsv-chunk
  '{:functions {rgb2hsv
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
(def hsv-to-rgb-chunk
  '{:functions {hsv2rgb
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
(def cubic-hsv-to-rgb-chunk
  '{:functions {hsv2rgb
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
(def quintic-hsv-to-rgb-chunk
  '{:functions {hsv2rgb
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
(def cosine-hsv-to-rgb-chunk
  '{:functions {hsv2rgb
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

; based on https://www.shadertoy.com/view/ttcyRS
(def mix-oklab-chunk
  '{:functions {mixOklab
                [(vec3
                  [rgb1 vec3
                   rgb2 vec3
                   p float]
                  (=mat3 kCONEtoLMS
                         (mat3 0.4121656120 0.2118591070 0.0883097947
                               0.5362752080 0.6807189584 0.2818474174
                               0.0514575653 0.1074065790 0.6302613616))
                  (=mat3 kLMStoCONE
                         (mat3 4.0767245293 -1.2681437731 -0.0041119885
                               -3.3072168827 2.6093323231 -0.7034763098
                               0.2307590544 -0.3411344290 1.7068625689))
                  (=vec3 lms1 (pow (* kCONEtoLMS rgb1) (vec3 0.333333333)))
                  (=vec3 lms2 (pow (* kCONEtoLMS rgb2) (vec3 0.333333333)))
                  (=vec3 lms (mix lms1 lms2 p))
                  (* kLMStoCONE (* lms lms lms)))
                 (vec3
                  [rgb1 vec3
                   rgb2 vec3
                   p float
                   mid-brighten-factor float]
                  (=mat3 kCONEtoLMS
                         (mat3 0.4121656120 0.2118591070 0.0883097947
                               0.5362752080 0.6807189584 0.2818474174
                               0.0514575653 0.1074065790 0.6302613616))
                  (=mat3 kLMStoCONE
                         (mat3 4.0767245293 -1.2681437731 -0.0041119885
                               -3.3072168827 2.6093323231 -0.7034763098
                               0.2307590544 -0.3411344290 1.7068625689))
                  (=vec3 lms1 (pow (* kCONEtoLMS rgb1) (vec3 0.333333333)))
                  (=vec3 lms2 (pow (* kCONEtoLMS rgb2) (vec3 0.333333333)))
                  (=vec3 lms (mix lms1 lms2 p))
                  (*= lms (+ 1 (* mid-brighten-factor p (- 1 p))))
                  (* kLMStoCONE (* lms lms lms)))]}})
