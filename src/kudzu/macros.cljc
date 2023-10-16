(ns kudzu.macros
  (:require [kudzu.tools :refer [unquotable]]
            [clojure.walk :refer [prewalk-replace]]))

(defn thread-first [x & forms]
  (loop [x x
         forms forms]
    (if forms
      (let [form (first forms)
            threaded (if (seq? form)
                       (concat (list (first form) x)
                               (next form))
                       (list form x))]
        (recur threaded (next forms)))
      x)))

(defn thread-last [x & forms]
  (loop [x x
         forms forms]
    (if forms
      (let [form (first forms)
            threaded (if (seq? form)
                       (concat (list (first form))
                               (next form)
                               (list x))
                       (list form x))]
        (recur threaded (next forms)))
      x)))

(defn thread-as [expr name & forms]
  (reduce (fn [result form]
            (prewalk-replace {name result} form))
          expr
          forms))

(defn pixel-pos [& [bi-or-uni resolution]]
  (unquotable
   (let [bi? (cond (or (nil? bi-or-uni)
                       (= bi-or-uni :bi))
                   true

                   (= bi-or-uni :uni)
                   false

                   :else (throw (str "KUDZU: Invalid option \""
                                     bi-or-uni
                                     "\" for pixel-pos")))
         fn-name (symbol (str "get-pixel-pos-" (if bi? "bi" "uni")))]
     {:chunk (merge
              '{:functions
                {~fn-name
                 (vec2
                  [res vec2]
                  (=float min-dim (min res.x res.y))
                  (=vec2 pos (/ (- gl_FragCoord.xy
                                   (* 0.5 (- res min-dim)))
                                min-dim))
                  ~(if bi? '(uni->bi pos) 'pos))}}
              (when (nil? resolution)
                '{:uniforms {resolution vec2}}))
      :expression (list fn-name
                        (cond (nil? resolution)
                              'resolution

                              (number? resolution)
                              (list 'vec2
                                    resolution)

                              :else
                              resolution))})))

(defn grab-pixel [tex-name & offset-params]
  (let [offset (when offset-params
                 (case (count offset-params)
                   1 offset-params
                   2 (cons 'ivec2 offset-params)
                   (throw (str "KUDZU: Invalid offset params \""
                               (apply str
                                      (interleave (repeat ", ")
                                                  offset-params))
                               "\" for grab-pixel"))))]
    (list 'texelFetch
          tex-name
          (if offset
            (list '+ '(ivec2 gl_FragCoord.xy) offset)
            '(ivec2 gl_FragCoord.xy))
          "0")))

(def default-macros
  {'-> thread-first
   '->> thread-last
   'as-> thread-as
   '=-> (fn [var-name & forms]
          (list '=
                var-name
                (concat (list '->
                              var-name)
                        forms)))
   '=->> (fn [var-name & forms]
           (list '=
                 var-name
                 (concat (list '->>
                               var-name)
                         forms)))
   'bi->uni #(list '* 0.5 (list '+ 1 %))
   'uni->bi #(list '- (list '* 2 %) 1)
   'pixel-pos pixel-pos
   'grab-pixel grab-pixel})
