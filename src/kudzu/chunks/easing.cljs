(ns kudzu.chunks.easing
  (:require [kudzu.tools :refer [unquotable]]))

; From Inigo Quilez's "Remapping Functions" article
; https://iquilezles.org/articles/functions/

(def cubic-identity
  '{:functions
    {cubic-identity
     (float
      [x float
       m float
       n float]
      (:if (> x m)
           (return x))
      (=float a (- (* 2 n)
                   m))
      (=float b (- (* 2 m)
                   (* 3 n)))
      (=float t (/ x m))
      (+ (* (+ (* a t)
               b)
            t
            t)
         n))}})

(def sqrt-identity-chunk
  '{:functions
    {square-identity
     (float
      [x float
       n float]
      (sqrt (+ (* x x)
               n)))}})

(def smooth-unit-identity-chunk
  '{:functions
    {smooth-identity
     (float
      [x float]
      (* x x (- 2 x)))}})

(def smoothstep-integral-chunk
  '{:functions
    {integral-smoothstep
     (float
      [x float
       t float]
      (:if (> x t)
           (return (- x (/ t 2))))
      (* x
         x
         x
         (/ (/ (- 1 (/ (* x 0.5)
                       t))
               t)
            t)))}})

(def exponential-impulse-chunk
  '{:functions
    {expo-impulse
     (float
      [x float
       k float]
      (* k x (exp (- 1 (* k x)))))}})

(def polynomial-impulse-chunk
  '{:functions
    {poly-impulse
     [(float
       [k float
        x float]
       (/ (* 2
             (sqrt k)
             x)
          (+ 1
             (* k x x))))
      (float
       [k float
        n float
        x float]
       (/ (* (/ n (- n 1))
             (pow (* (- n 1) k)
                  (/ n))
             x)
          (+ 1
             (* k
                (pow x n)))))]}})

(def sustained-impulse-chunk
  '{:functions
    {sustain-impulse
     (float
      [x float
       f float
       k float]
      (=float s (max (- x f) 0))
      (min (/ (* x x)
              (* f f))
           (+ 1
              (* (/ 2 f)
                 s
                 (exp (* "-k"
                         s))))))}})

(def cubic-pulse-chunk
  '{:functions
    {cubic-pulse
     (float
      [c float
       w float
       x float]
      (= x (abs (- x c)))
      (:if (> x w)
           (return 0))
      (= x (/ x w))
      (- 1
         (* x
            x
            (- 3
               (* 2 x)))))}})

(def exponential-step-chunk
  '{:functions
    {exp-step
     (float
      [x float
       k float
       n float]
      (exp (* "-k"
              (pow x n))))}})

(def gain-chunk
  '{:functions
    {gain
     (float
      [x float
       k float]
      (=bool lt? (< x 0.5))
      (=float a (* 0.5
                   (pow (* 2
                           (if lt?
                             x
                             (- 1
                                x)))
                        k)))
      (if lt?
        a
        (- 1 a)))}})

(def parabola-chunk
  '{:functions
    {parabola
     (float
      [x float
       k float]
      (pow (* 4 x (- 1 x))
           k))}})

(def power-curve-chunk
  '{:functions
    {power-curve
     (float
      [x float
       a float
       b float]
      (=float k (/ (pow (+ a b)
                        (+ a b))
                   (* (pow a a)
                      (pow b b))))
      (* k
         (* (pow x a)
            (pow (- 1 x)
                 b))))}})

(def sinc-chunk
  (unquotable
   '{:functions
     {sinc
      (float
       [x float
        k float]
       (=float a (* ~Math/PI
                    (- (* k x)
                       1)))
       (/ (sin a)
          abs))}}))

(def quadratic-falloff-chunk
  '{:functions
    {falloff
     (float
      [x float
       m float]
      (= x (/ (* (+ x 1)
                 (+ x 1))))
      (= m (/ (* (+ m 1)
                 (+ m 1))))
      (/ (- x m)
         (- 1 m)))}})

