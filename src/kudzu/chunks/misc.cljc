(ns kudzu.chunks.misc
  (:require [kudzu.tools :refer [generalize-float-functions
                                 unquotable]]
            [clojure.walk :refer [postwalk
                                  postwalk-replace]]))

(def trivial-vert-source
  '{:precision {float lowp}
    :inputs {vertPos vec4}
    :main ((= gl_Position vertPos))})

(defn identity-frag-source [texture-type]
  (postwalk-replace
   (let [float-tex? (= texture-type :f8)]
     {:sampler-type (if float-tex? 'sampler2D 'usampler2D)
      :pixel-type (if float-tex? 'vec4 'uvec4)})
   '{:precision {float highp
                 sampler2D highp
                 int highp
                 usampler2D highp}
     :uniforms {tex :sampler-type
                resolution vec2}
     :outputs {frag-color :pixel-type}
     :main ((= frag-color
               (texture tex
                        (/ gl_FragCoord.xy resolution))))}))

(def rescale-chunk
  '{:functions
    {rescale
     (float
      [old-min float
       old-max float
       new-min float
       new-max float
       x float]
      (+ new-min
         (* (- new-max new-min)
            (/ (- x old-min)
               (- old-max old-min)))))}})

(def sigmoid-chunk
  (generalize-float-functions
   '{:functions {sigmoid (float
                          [x float]
                          (/ 1 (+ 1 (exp (- 0 x)))))}}))

(def sympow-chunk
  '{:functions
    {sympow
     (float
      [x float
       power float]
      (* (sign x)
         (pow (abs x)
              power)))}})

(def smoothstair-chunk
  '{:functions
    {smoothstair
     (float
      [x float
       steps float
       steepness float]
      (*= x steps)
      (=float c (- (/ 2 (- 1 steepness)) 1))
      (=float p (mod x 1))
      (/ (+ (floor x)
            (if (< p 0.5)
              (/ (pow p c)
                 (pow 0.5 (- c 1)))
              (- 1
                 (/ (pow (- 1 p) c)
                    (pow 0.5 (- c 1))))))
         steps))}})

(def bilinear-usampler-chunk
  '{:functions
    {texture-bilinear
     (vec4
      [tex usampler2D
       pos vec2]
      (=vec2 tex-size (vec2 (textureSize tex "0")))
      (=vec2 tex-coords (- (* pos tex-size) 0.5))
      (=vec2 grid-coords (+ (floor tex-coords) 0.5))
      (=vec2 tween-coords (fract tex-coords))
      (mix (mix (vec4 (texture tex (/ grid-coords tex-size)))
                (vec4 (texture tex (/ (+ grid-coords (vec2 1 0)) tex-size)))
                tween-coords.x)
           (mix (vec4 (texture tex (/ (+ grid-coords (vec2 0 1)) tex-size)))
                (vec4 (texture tex (/ (+ grid-coords (vec2 1 1)) tex-size)))
                tween-coords.x)
           tween-coords.y))}})

(def paretto-transform-chunk
  '{:functions {paretto
                (float
                 [value float
                  shape float
                  scale float]
                 (/ (pow (* shape scale) shape)
                    (pow value (+ shape 1))))}})

(def wave-chunk
  (generalize-float-functions
   (unquotable
    '{:functions
      {osc (float [x float] (sin (* x ~(* 2 Math/PI))))
       saw (float [x float] (- (mod (* 2 (+ x 0.5)) 2) 1))
       tri (float [x float] 
                  (- (abs (- (* 2 (mod (- 0.5 (* 2 x)) 2)) 2)) 1))}})))

(def gradient-chunk
  (unquotable
   {:macros {'find-gradient
             (fn [dimensions function-name sample-distance pos]
               (let [gradient-fn-name (gensym 'gradient)
                     dimension-type (case dimensions
                                      1 'float
                                      2 'vec2
                                      3 'vec3
                                      4 'vec4)]
                 {:chunk
                  {:functions
                   {gradient-fn-name
                    '(~dimension-type
                      [x ~dimension-type]
                      (/ ~(if (= dimensions 1)
                            '(- (~function-name (+ x ~sample-distance))
                                (~function-name (- x ~sample-distance)))
                            (cons
                             dimension-type
                             (map (fn [dim]
                                    '(- (~function-name
                                         (+ x
                                            ~(cons dimension-type
                                                   (take dimensions
                                                         (concat
                                                          (repeat dim 0)
                                                          (list sample-distance)
                                                          (repeat 0))))))
                                        (~function-name
                                         (- x
                                            ~(cons dimension-type
                                                   (take dimensions
                                                         (concat
                                                          (repeat dim 0)
                                                          (list sample-distance)
                                                          (repeat 0))))))))
                                  (range dimensions))))
                         (* ~sample-distance 2)))}}
                  :expression (list gradient-fn-name
                                    pos)}))}}))
