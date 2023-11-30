(ns kudzu.chunks.postprocessing
  (:require [clojure.walk :refer [postwalk-replace]]
            [kudzu.core :refer [combine-chunks]]))

(def gaussian-chunk
  '{:functions
    {gaussian (float
               [offset vec2
                sigma float]
               (=vec2 scaledOffset (/ offset sigma))
               (/ (exp (* -0.5 (dot scaledOffset scaledOffset)))
                  (* 6.28 (pow sigma 2))))}})

(defn plus-neighborhood [radius & [skip-factor]]
  (conj (set
         (mapcat (comp (fn [r]
                         (list [0 r]
                               [r 0]
                               [0 (- r)]
                               [(- r) 0]))
                       (partial * (or skip-factor 1)))
                 (range 1 (inc radius))))
        [0 0]))

(defn star-neighborhood [radius & [skip-factor]]
  (conj (set
         (mapcat (comp (fn [r]
                         (list [0 r]
                               [r r]
                               [r 0]
                               [r (- r)]
                               [0 (- r)]
                               [(- r) (- r)]
                               [(- r) 0]
                               [(- r) r]))
                       (partial * (or skip-factor 1)))
                 (range 1 (inc radius))))
        [0 0]))

(defn square-neighborhood [radius & [skip-factor]]
  (set
   (map (partial mapv (partial * (or skip-factor 1)))
        (for [x (range (- radius) (inc radius))
              y (range (- radius) (inc radius))]
          [x y]))))

(defn prescaled-gaussian-sample-expression [value-expression neighborhood sigma]
  (let [factors (map (fn [[x y]]
                       (Math/exp
                        (/ (- (+ (* x x) (* y y)))
                           (* 2 sigma sigma))))
                     neighborhood)
        factor-sum (apply + factors)]
    (conj (map (fn [[x y] factor]
                 (list '*
                       (/ factor factor-sum)
                       (postwalk-replace
                        {:x x
                         :y y}
                        value-expression)))
               neighborhood
               factors)
          '+)))

; TODO: convert to a macro
(defn create-gaussian-sample-chunk [texture-type neighborhood]
  (combine-chunks
   gaussian-chunk
   (postwalk-replace
    {:sampler-type (if (= texture-type :f8)
                     'sampler2D
                     'usampler2D)}
    {:functions
     {'gaussianSample
      (concat
       '(vec4
         [tex :sampler-type
          pos vec2
          offsetFactor vec2
          sigma float]
         (=vec4 sampleSum (vec4 0))
         (=float factorSum 0)
         (=vec2 offset (vec2 0))
         (=float factor 0))
       (mapcat (fn [[x y]]
                 (postwalk-replace
                  {:x x
                   :y y}
                  '((= offset (vec2 :x :y))
                    (= factor (gaussian offset sigma))
                    (+= factorSum factor)
                    (+= sampleSum
                        (* factor
                           (vec4
                            (texture tex
                                     (+ pos
                                        (* offsetFactor offset)))))))))
               neighborhood)
       '((/ sampleSum factorSum)))}})))

; TODO: convert to a macro
(defn get-bloom-chunk [texture-type neighborhood sigma]
  (postwalk-replace
   {:divisor (.toFixed ({:f8 1
                         :u8 (dec (Math/pow 2 8))
                         :u16 (dec (Math/pow 2 16))
                         :u32 (dec (Math/pow 2 32))} texture-type)
                       1)
    :sampler-type (if (= texture-type :f8)
                    'sampler2D
                    'usampler2D)
    :gaussian-expresion (prescaled-gaussian-sample-expression
                         '(vec4 (texture tex (+ pos (* (vec2 :x :y) step))))
                         neighborhood
                         sigma)}
   '{:functions
     {bloom
      (vec4
       [tex :sampler-type
        pos vec2
        step float
        intensity float]
       (=vec4 sum :gaussian-expresion)
       (vec4 (/ (+ (* sum intensity)
                   (vec4 (texture tex pos)))
                :divisor)))}}))
