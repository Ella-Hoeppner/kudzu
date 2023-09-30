(ns kudzu.chunks.color.oklab)

; based on https://www.shadertoy.com/view/ttcyRS
(def mix-oklab-chunk
  '{:functions {mix-oklab
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
