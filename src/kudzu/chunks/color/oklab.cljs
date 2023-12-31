(ns kudzu.chunks.color.oklab
  (:require [kudzu.core :refer [combine-chunks]]
            [kudzu.tools :refer [unquotable]]
            [kudzu.chunks.color.rgb :refer [lrgb-chunk]]))

; most chunks here are based on https://www.shadertoy.com/view/7sVGD1

(def oklab-chunk
  (unquotable
   '{:functions
     {cube-root
      (float
       [x float]
       (* (sign x)
          (pow (abs x)
               ~(/ 3))))
      lrgb->oklab
      (vec3
       [c vec3]
       (=float l (+ (* 0.4122214708 c.r)
                    (* 0.5363325363 c.g)
                    (* 0.0514459929 c.b)))
       (=float m (+ (* 0.2119034982 c.r)
                    (* 0.6806995451 c.g)
                    (* 0.1073969566 c.b)))
       (=float s (+ (* 0.0883024619 c.r)
                    (* 0.2817188376 c.g)
                    (* 0.6299787005 c.b)))

       (=float l2 (cube-root l))
       (=float m2 (cube-root m))
       (=float s2 (cube-root s))

       (vec3 (+ (* 0.2104542553 l2)
                (* 0.7936177850 m2)
                (* -0.0040720468 s2))
             (+ (* 1.9779984951 l2)
                (* -2.4285922050 m2)
                (* 0.4505937099 s2))
             (+ (* 0.0259040371 l2)
                (* 0.7827717662 m2)
                (* -0.8086757660 s2))))

      oklab->lrgb
      (vec3
       [c vec3]
       (=float l2
               (+ c.x
                  (* 0.3963377774 c.y)
                  (* 0.2158037573 c.z)))
       (=float m2 (+ c.x
                     (* -0.1055613458 c.y)
                     (* -0.0638541728 c.z)))
       (=float s2 (+ c.x
                     (* -0.0894841775 c.y)
                     (* -1.2914855480 c.z)))

       (=float l (* l2 l2 l2))
       (=float m (* m2 m2 m2))
       (=float s (* s2 s2 s2))

       (vec3 (+ (* l 4.0767416621)
                (* m -3.3077115913)
                (* s 0.2309699292))
             (+ (* l -1.2684380046)
                (* m 2.6097574011)
                (* s -0.3413193965))
             (+ (* l -0.0041960863)
                (* m -0.7034186147)
                (* s 1.7076147010))))}}))

(def mix-oklab-chunk
  (combine-chunks
   oklab-chunk
   '{:functions
     {mix-oklab
      (vec3
       [rgb1 vec3
        rgb2 vec3
        p float]
       (oklab->lrgb (mix (lrgb->oklab rgb1)
                         (lrgb->oklab rgb2)
                         p)))}}))

(def okhsl-okhsv-shared-chunk
  (combine-chunks
   oklab-chunk
   lrgb-chunk
   (unquotable
    '{:functions
      {compute-max-saturation
       (float
        [a float
         b float]
        (=float k0 nil)
        (=float k1 nil)
        (=float k2 nil)
        (=float k3 nil)
        (=float k4 nil)
        (=float wl nil)
        (=float wm nil)
        (=float ws nil)

        (:if (> (- (* a -1.88170328)
                   (* b 0.80936493))
                1)
             (:block
              (= k0 1.19086277)
              (= k1 1.76576728)
              (= k2 0.59662641)
              (= k3 0.75515197)
              (= k4 0.56771245)
              (= wl 4.0767416621)
              (= wm -3.3077115913)
              (= ws 0.2309699292))
             (:if (> (- (* a 1.81444104)
                        (* b 1.19445276))
                     1)
                  (:block
                   (= k0 0.73956515)
                   (= k1 -0.45954404)
                   (= k2 0.08285427)
                   (= k3 0.12541070)
                   (= k4 0.14503204)
                   (= wl -1.2684380046)
                   (= wm 2.6097574011)
                   (= ws -0.3413193965))
                  (:block
                   (= k0 1.35733652)
                   (= k1 -0.00915799)
                   (= k2 -1.15130210)
                   (= k3 -0.50559606)
                   (= k4 0.00692167)
                   (= wl -0.0041960863)
                   (= wm -0.7034186147)
                   (= ws 1.7076147010))))

        (=float S (+ k0
                     (* k1 a)
                     (* k2 b)
                     (* k3 a a)
                     (* k4 a b)))

        (=float k-l (+ (* a 0.3963377774)
                       (* b 0.2158037573)))
        (=float k-m (+ (* a -0.1055613458)
                       (* b -0.0638541728)))
        (=float k-s (+ (* a -0.0894841775)
                       (* b -1.2914855480)))

        (=float l2 (+ 1 (* S k-l)))
        (=float m2 (+ 1 (* S k-m)))
        (=float s2 (+ 1 (* S k-s)))

        (=float l (* l2 l2 l2))
        (=float m (* m2 m2 m2))
        (=float s (* s2 s2 s2))

        (=float l-dS (* 3 k-l l2 l2))
        (=float m-dS (* 3 k-m m2 m2))
        (=float s-dS (* 3 k-s s2 s2))

        (=float l-dS2 (* 6 k-l k-l l2))
        (=float m-dS2 (* 6 k-m k-m m2))
        (=float s-dS2 (* 6 k-s k-s s2))

        (=float f (+ (* wl l)
                     (* wm m)
                     (* ws s)))
        (=float f1
                (+ (* wl l-dS)
                   (* wm m-dS)
                   (* ws s-dS)))
        (=float f2
                (+ (* wl l-dS2)
                   (* wm m-dS)
                   (* ws s-dS)))

        (- S (/ (* f f1)
                (- (* f1 f1)
                   (* 0.5 f f2)))))

       find-cusp
       (vec2
        [a float
         b float]
        (=float S-cusp (compute-max-saturation a b))
        (=vec3 rgb-at-max (oklab->lrgb (vec3 1
                                             (* S-cusp a)
                                             (* S-cusp b))))
        (=float L-cusp (cube-root (/ (mmax rgb-at-max.r rgb-at-max.g rgb-at-max.b))))
        (=float C-cusp (* L-cusp S-cusp))
        (vec2 L-cusp C-cusp))

       find-gamut-intersection
       [(float
         [a float
          b float
          L1 float
          C1 float
          L0 float
          cusp vec2]
         (=float t nil)
         (:when (<= (- (* (- L1 L0) cusp.y)
                       (* (- cusp.x L0) C1))
                    0)
                (= t (/ (* cusp.y L0)
                        (+ (* C1 cusp.x)
                           (* cusp.y (- L0 L1))))))
         (:else
          (= t (/ (* cusp.y (- L0 1))
                  (+ (* C1
                        (- cusp.x 1))
                     (* cusp.y
                        (- L0 L1)))))
          (=float dL (- L1 L0))
          (=float dC C1)
          (=float k-l (+ (* a 0.3963377774)
                         (* b 0.2158037573)))
          (=float k-m (+ (* a -0.1055613458)
                         (* b -0.0638541728)))
          (=float k-s (+ (* a -0.0894841775)
                         (* b -1.2914855480)))
          (=float l-dt (+ dL (* dC k-l)))
          (=float m-dt (+ dL (* dC k-m)))
          (=float s-dt (+ dL (* dC k-s)))

          (=float L (+ (* L0 (- 1 t))
                       (* t L1)))
          (=float C (* t C1))

          (=float l_ (+ L (* C k-l)))
          (=float m_ (+ L (* C k-m)))
          (=float s_ (+ L (* C k-s)))

          (=float l (* l_ l_ l_))
          (=float m (* m_ m_ m_))
          (=float s (* s_ s_ s_))

          (=float ldt (* 3 l-dt l_ l_))
          (=float mdt (* 3 m-dt m_ m_))
          (=float sdt (* 3 s-dt s_ s_))

          (=float ldt2 (* 6 l-dt l-dt l_))
          (=float mdt2 (* 6 m-dt m-dt m_))
          (=float sdt2 (* 6 s-dt s-dt s_))

          (=float r (+ (* 4.0767416621 l)
                       (* -3.3077115913 m)
                       (* 0.2309699292 s)
                       -1))
          (=float r1 (+ (* 4.0767416621 ldt)
                        (* -3.3077115913 mdt)
                        (* 0.2309699292 sdt)))
          (=float r2 (+ (* 4.0767416621 ldt2)
                        (* -3.3077115913 mdt2)
                        (* 0.2309699292 sdt2)))

          (=float u-r (/ r1 (- (* r1 r1)
                               (* 0.5 r r2))))
          (=float t-r (* (- r) u-r))

          (=float g (+ (* -1.2684380046 l)
                       (* 2.6097574011 m)
                       (* -0.3413193965 s)
                       -1))
          (=float g1 (+ (* -1.2684380046 ldt)
                        (* 2.6097574011 mdt)
                        (* -0.3413193965 sdt)))
          (=float g2 (+ (* -1.2684380046 ldt2)
                        (* 2.6097574011 mdt2)
                        (* -0.3413193965 sdt2)))

          (=float u-g (/ g1 (- (* g1 g1)
                               (* 0.5 g g2))))
          (=float t-g (* (- g) u-g))

          (=float b (+ (* -0.0041960863 l)
                       (* -0.7034186147 m)
                       (* 1.7076147010 s)
                       -1))
          (=float b1 (+ (* -0.0041960863 ldt)
                        (* -0.7034186147 mdt)
                        (* 1.7076147010 sdt)))
          (=float b2 (+ (* -0.0041960863 ldt2)
                        (* -0.7034186147 mdt2)
                        (* 1.7076147010 sdt2)))

          (=float u-b (/ b1 (- (* b1 b1)
                               (* 0.5 b b2))))
          (=float t-b (* (- b) u-b))

          (= t-r (if (>= u-r 0) t-r 10000))
          (= t-g (if (>= u-g 0) t-g 10000))
          (= t-b (if (>= u-b 0) t-b 10000))

          (+= t (mmin t-r t-g t-b)))

         t)
        (float
         [a float
          b float
          L1 float
          C1 float
          L0 float]
         (find-gamut-intersection a b L1 C1 L0 (find-cusp a b)))]

       gamut-clip-preserve-chroma
       (vec3
        [rgb vec3]
        (:when (&& (< rgb.r 1)
                   (> rgb.r 0)
                   (< rgb.g 1)
                   (> rgb.g 0)
                   (< rgb.b 1)
                   (> rgb.b 0))
               (return rgb))
        (=vec3 lab (lrgb->oklab rgb))
        (=float L lab.x)
        (=float eps 0.00001)
        (=float C (max eps
                       (sqrt (+ (* lab.y lab.y)
                                (* lab.z lab.z)))))
        (=float a (/ lab.y C))
        (=float b (/ lab.z C))

        (=float L0 (clamp L 0 1))

        (=float t (find-gamut-intersection a b L C L0))

        (=float L-clipped (+ (* L0 (- 1 t))
                             (* t L)))
        (=float C-clipped (* t C))

        (oklab->lrgb (vec3 L-clipped
                           (* a C-clipped)
                           (* b C-clipped))))

       gamut-clip-project-to-0-5
       (vec3
        [rgb vec3]
        (:when (&& (< rgb.r 1)
                   (> rgb.r 0)
                   (< rgb.g 1)
                   (> rgb.g 0)
                   (< rgb.b 1)
                   (> rgb.b 0))
               (return rgb))

        (=vec3 lab (lrgb->oklab rgb))
        (=float L lab.x)
        (=float eps 0.00001)
        (=float C (max eps
                       (sqrt (+ (* lab.y lab.y)
                                (* lab.z lab.z)))))
        (=float a (/ lab.y C))
        (=float b (/ lab.z C))

        (=float L0 0.5)

        (=float t (find-gamut-intersection a b L C L0))

        (=float L-clipped (+ (* L0 (- 1 t))
                             (* t L)))
        (=float C-clipped (* t C))

        (oklab->lrgb (vec3 L-clipped
                           (* a C-clipped)
                           (* b C-clipped))))

       gamut-clip-project-to-L-cusp
       (vec3
        [rgb vec3]
        (:when (&& (< rgb.r 1)
                   (> rgb.r 0)
                   (< rgb.g 1)
                   (> rgb.g 0)
                   (< rgb.b 1)
                   (> rgb.b 0))
               (return rgb))

        (=vec3 lab (lrgb->oklab rgb))
        (=float L lab.x)
        (=float eps 0.00001)
        (=float C (max eps
                       (sqrt (+ (* lab.y lab.y)
                                (* lab.z lab.z)))))
        (=float a (/ lab.y C))
        (=float b (/ lab.z C))

        (=vec2 cusp (find-cusp a b))
        (=float L0 cusp.x)

        (=float t (find-gamut-intersection a b L C L0 cusp))

        (=float L-clipped (+ (* L0 (- 1 t))
                             (* t L)))
        (=float C-clipped (* t C))

        (oklab->lrgb (vec3 L-clipped
                           (* a C-clipped)
                           (* b C-clipped))))

       gamut-clip-adaptive-L0-0-5
       (vec3
        [rgb vec3
         alpha float]
        (:when (&& (< rgb.r 1)
                   (> rgb.r 0)
                   (< rgb.g 1)
                   (> rgb.g 0)
                   (< rgb.b 1)
                   (> rgb.b 0))
               (return rgb))

        (=vec3 lab (lrgb->oklab rgb))
        (=float L lab.x)
        (=float eps 0.00001)
        (=float C (max eps
                       (sqrt (+ (* lab.y lab.y)
                                (* lab.z lab.z)))))
        (=float a (/ lab.y C))
        (=float b (/ lab.z C))

        (=float Ld (- L 0.5))
        (=float e1 (+ 0.5
                      (abs Ld)
                      (* alpha C)))
        (=float L0 (* 0.5
                      (+ 1
                         (* (sign Ld)
                            (- e1 (sqrt (- (* e1 e1)
                                           (* 2 (abs Ld)))))))))

        (=float t (find-gamut-intersection a b L C L0))

        (=float L-clipped (+ (* L0 (- 1 t))
                             (* t L)))
        (=float C-clipped (* t C))

        (oklab->lrgb (vec3 L-clipped
                           (* a C-clipped)
                           (* b C-clipped))))

       gamut-clip-adaptive-L0-L-cusp
       (vec3
        [rgb vec3
         alpha float]
        (:when (&& (< rgb.r 1)
                   (> rgb.r 0)
                   (< rgb.g 1)
                   (> rgb.g 0)
                   (< rgb.b 1)
                   (> rgb.b 0))
               (return rgb))

        (=vec3 lab (lrgb->oklab rgb))
        (=float L lab.x)
        (=float eps 0.00001)
        (=float C (max eps
                       (sqrt (+ (* lab.y lab.y)
                                (* lab.z lab.z)))))
        (=float a (/ lab.y C))
        (=float b (/ lab.z C))

        (=vec2 cusp (find-cusp a b))
        (=float Ld (- L cusp.x))
        (=float k (* 2 (if (> Ld 0) (- 1 cusp.x) cusp.x)))
        (=float e1 (+ (* 0.5 k)
                      (abs Ld)
                      (/ (* alpha C) k)))
        (=float L0 (+ cusp.x (* 0.5
                                (sign Ld)
                                (- e1
                                   (sqrt (- (* e1 e1)
                                            (* 2 k (abs Ld))))))))

        (=float t (find-gamut-intersection a b L C L0 cusp))

        (=float L-clipped (+ (* L0 (- 1 t))
                             (* t L)))
        (=float C-clipped (* t C))

        (oklab->lrgb (vec3 L-clipped
                           (* a C-clipped)
                           (* b C-clipped))))

       toe
       (float
        [x float]
        (=float k1 0.206)
        (=float k2 0.03)
        (=float k3 ~(/ 1.206 1.03))

        (=float a (- (* k3 x) k1))

        (* 0.5
           (+ (* k3 x)
              (- k1)
              (sqrt (+ (* a a)
                       (* 4 k2 k3 x))))))

       toe-inv
       (float
        [x float]
        (=float k1 0.206)
        (=float k2 0.03)
        (=float k3 ~(/ 1.206 1.03))

        (/ (+ (* x x)
              (* k1 x))
           (* k3 (+ x k2))))

       to-ST
       (vec2
        [cusp vec2]
        (=float L cusp.x)
        (=float C cusp.y)
        (vec2 (/ C L)
              (/ C (- 1 L))))

       get-ST-mid
       (vec2
        [a float
         b float]
        (vec2 (+ 0.11516993
                 (/ (+ 7.44778970
                       (* 4.15901240 b)
                       (* a
                          (+ -2.19557347
                             (* 1.75198401 b)
                             (* a (+ -2.13704948
                                     (* -10.02301043 b)
                                     (* a
                                        (+ -4.24894561
                                           (* 5.38770819 b)
                                           (* 4.69891013 a))))))))))
              (+ 0.11239642
                 (/ (+ 1.61320320
                       (* -0.68124379 b)
                       (* a
                          (+ 0.40370612
                             (* 0.90148123 b)
                             (* a
                                (+ -0.27087943
                                   (* 0.61223990 b)
                                   (* a
                                      (+ 0.00299215
                                         (* -0.45399568 b)
                                         (* -0.14661872 a))))))))))))

       get-Cs
       (vec3
        [L float
         a float
         b float]
        (=vec2 cusp (find-cusp a b))

        (=float C-max (find-gamut-intersection a b L 1 L cusp))
        (=vec2 ST-max (to-ST cusp))

        (=float k (/ C-max
                     (min (* L ST-max.x)
                          (* (- 1 L) ST-max.y))))

        (=float C-mid nil)
        (:block (=vec2 ST-mid (get-ST-mid a b))
                (=float C-a (* L ST-mid.x))
                (=float C-b (* (- 1 L) ST-mid.y))
                (=float C-a-squared (* C-a C-a))
                (=float C-b-squared (* C-b C-b))
                (= C-mid (* 0.9
                            k
                            (sqrt (sqrt (/ (+ (/ (* C-a-squared C-a-squared))
                                              (/ (* C-b-squared C-b-squared)))))))))
        (=float C-0 nil)
        (:block (=float C-a (* L 0.4))
                (=float C-b (* (- 1 L) 0.8))
                (= C-0 (sqrt (/ (+ (/ (* C-a C-a))
                                   (/ (* C-b C-b)))))))

        (vec3 C-0 C-mid C-max))}})))

(def okhsl-chunk
  (unquotable
   (combine-chunks
    okhsl-okhsv-shared-chunk
    '{:functions
      {okhsl->oklab
       (vec3
        [hsl vec3]
        (=float h hsl.x)
        (=float s hsl.y)
        (=float l hsl.z)

        (:when (== l 1) (return (vec3 1)))
        (:else-if (== l 0) (return (vec3 0)))

        (=float a (cos (* ~(* Math/PI 2) h)))
        (=float b (sin (* ~(* Math/PI 2) h)))
        (=float L (toe-inv l))

        (=vec3 cs (get-Cs L a b))
        (=float C-0 cs.x)
        (=float C-mid cs.y)
        (=float C-max cs.z)

        (=float mid 0.8)
        (=float mid-inv 1.25)

        (=float C nil)
        (=float t nil)
        (=float k0 nil)
        (=float k1 nil)
        (=float k2 nil)

        (:if (< s mid)
             (:block
              (= t (* mid-inv s))
              (= k1 (* mid C-0))
              (= k2 (- 1 (/ k1 C-mid)))

              (= C (/ (* t k1)
                      (- 1 (* k2 t)))))
             (:block
              (= t (/ (- s mid)
                      (- 1 mid)))
              (= k0 C-mid)
              (= k1 (/ (* (- 1 mid)
                          C-mid
                          C-mid
                          mid-inv
                          mid-inv)
                       C-0))
              (= k2 (- 1 (/ k1 (- C-max C-mid))))

              (= C (+ k0
                      (/ (* t k1)
                         (- 1 (* k2 t)))))))

        (vec3 L (* C a) (* C b)))

       oklab->okhsl ; untested
       (vec3
        [rgb vec3]
        (=vec3 lab (lrgb->oklab rgb))
        (=float C (sqrt (+ (* lab.y lab.y)
                           (* lab.z lab.z))))
        (=float a (/ lab.y C))
        (=float b (/ lab.z C))

        (=float L lab.x)
        (=float h (+ 0.5 (/ (atan (- lab.z) (- lab.y))
                            ~(* Math/PI 2))))

        (=vec3 cs (get-Cs L a b))
        (=float C-0 cs.x)
        (=float C-mid cs.y)
        (=float C-max cs.z)

        (=float mid 0.8)
        (=float mid-inv 1.25)

        (=float s nil)
        (:if (< C C-mid)
             (:block
              (=float k1 (* mid C-0))
              (=float k2 (- 1 (/ k1 C-mid)))
              (=float t (/ C (+ k1 (* k2 C))))
              (= s (* t mid)))
             (:block
              (=float k0 C-mid)
              (=float k1 (/ (* (- 1 mid)
                               C-mid
                               C-mid
                               mid-inv
                               mid-inv)
                            C-0))
              (=float k2 (- 1 (/ k1 (- C-max C-mid))))

              (=float t (/ (- C k0)
                           (+ k1 (* k2 (- C k0)))))
              (= s (+ mid (* t (- 1 mid))))))

        (vec3 h s (toe L)))

       okhsl->lrgb
       (vec3
        [hsl vec3]
        (-> hsl okhsl->oklab oklab->lrgb))

       lrgb->okhsl
       (vec3
        [rgb vec3]
        (-> rgb lrgb->oklab oklab->okhsl))}})))

(def okhsv-chunk
  (unquotable
   (combine-chunks
    okhsl-okhsv-shared-chunk
    '{:functions
      {okhsv->oklab
       (vec3
        [hsv vec3]
        (=float h hsv.x)
        (=float s hsv.y)
        (=float v hsv.z)

        (=float a (cos (* ~(* Math/PI 2) h)))
        (=float b (sin (* ~(* Math/PI 2) h)))

        (=vec2 cusp (find-cusp a b))
        (=vec2 st-max (to-ST cusp))
        (=float s-max st-max.x)
        (=float t-max st-max.y)
        (=float s0 0.5)
        (=float k (- 1 (/ s0 s-max)))

        (=float denom (+ s0 (* t-max (- 1 (* k s)))))
        (=float l-v (- 1 (/ (* s s0)
                            denom)))
        (=float c-v (/ (* s t-max s0)
                       denom))

        (=float l (* v l-v))
        (=float c (* v c-v))

        (=float l-vt (toe-inv l-v))
        (=float c-vt (/ (* c-v l-vt)
                        l-v))

        (=float l-new (toe-inv l))
        (*= c (/ l-new l))
        (= l l-new)

        (=vec3 rgb-scale (oklab->lrgb (vec3 l-vt
                                            (* a c-vt)
                                            (* b c-vt))))
        (=float scale-l (cube-root (/ (mmax rgb-scale.r rgb-scale.g rgb-scale.b 0))))

        (*= l scale-l)
        (*= c scale-l)

        (vec3 l (* c a) (* c b)))

       oklab->okhsv ; untested
       (vec3
        [lab vec3]
        (=float c (sqrt (+ (* lab.y lab.y)
                           (* lab.z lab.z))))
        (=float a (/ lab.y c))
        (=float b (/ lab.z c))

        (=float l lab.x)
        (=float h (+ 0.5 (/ (atan (- lab.z) (- lab.y))
                            ~(* Math/PI 2))))

        (=vec2 cusp (find-cusp a b))
        (=vec2 st-max (to-ST cusp))
        (=float s-max st-max.x)
        (=float t-max st-max.y)
        (=float s0 0.5)
        (=float k (- 1 (/ s0 s-max)))

        (=float t (/ t-max (+ c (* l t-max))))
        (=float l-v (* t l))
        (=float c-v (* t c))

        (=float l-vt (toe-inv l-v))
        (=float c-vt (* c-v (/ l-vt l-v)))

        (=vec3 rgb-scale (oklab->lrgb (vec3 l-vt
                                            (* a c-vt)
                                            (* b c-vt))))
        (=float scale-l (cube-root (/ (mmax rgb-scale.r rgb-scale.g rgb-scale.b 0))))

        (=-> l (/ scale-l))
        (=-> c (/ scale-l))

        (*= c (/ (toe l) l))
        (=-> l toe)

        (=float v (/ l l-v))
        (=float s (/ (* c-v (+ s0 t-max))
                     (* t-max (+ s0 (* k c-v)))))

        (vec3 h s v))

       okhsv->lrgb
       (vec3
        [hsv vec3]
        (-> hsv okhsv->oklab oklab->lrgb))

       lrgb->okhsv
       (vec3
        [rgb vec3]
        (-> rgb lrgb->oklab oklab->okhsv))}})))
