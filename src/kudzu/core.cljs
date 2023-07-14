(ns kudzu.core
  (:require [kudzu.compiler :refer [processed-kudzu->glsl]]
            [clojure.walk :refer [prewalk-replace
                                  prewalk]]
            [kudzu.macros :refer [default-macros]]
            [kudzu.validation :refer [validate-kudzu-keys]]))

(defn combine-chunks [& chunks]
  (let [merged-functions
        (apply (partial merge-with
                        (fn [body-1 body-2]
                          (vec (concat
                                (if (vector? body-1) body-1 (list body-1))
                                (if (vector? body-2) body-2 (list body-2))))))
               (map :functions chunks))
        merged-global (apply concat (map :global chunks))]
    (cond-> (apply (partial merge-with merge) chunks)
      merged-functions (assoc :functions merged-functions)
      (seq merged-global) (assoc :global merged-global))))

(defn apply-macros [{:keys [macros] :as shader} & [exclude-defaults?]]
  (let [chunks (atom nil)
        new-shader
        (apply combine-chunks
               (concat
                (list (prewalk
                       (fn [subexp]
                         (if (seq? subexp)
                           (let [f (first subexp)
                                 macro-fn (or (when macros
                                                (macros f))
                                              (when-not exclude-defaults?
                                                (default-macros f)))]
                             (if macro-fn
                               (let [macro-result (apply macro-fn
                                                         (rest subexp))]
                                 (if (map? macro-result)
                                   (do (swap! chunks
                                              conj
                                              (:chunk macro-result))
                                       (:expression macro-result))
                                   macro-result))
                               subexp))
                           subexp))
                       shader))
                @chunks))]
    (if (= new-shader shader)
      new-shader
      (apply-macros new-shader exclude-defaults?))))

(defn gensym-replace [replacements expression]
  (prewalk-replace
   (into {}
         (map (fn [k]
                [k (gensym (symbol k))])
              replacements))
   expression))

(defn strip-redefines [shader]
  (update
   shader
   :functions
   (fn [functions]
     (into {}
           (map (fn [[fn-name fn-body]]
                  [fn-name
                   (if (vector? fn-body)
                     (vec (distinct fn-body))
                     fn-body)])
                functions)))))

(defn preprocess [{:keys [constants main] :as shader}]
  (-> shader
      (cond-> main (update :functions
                           assoc
                           'main
                           (conj main [] 'void)))
      (cond->> constants (prewalk-replace constants))
      apply-macros
      (dissoc :main :macros :constants)
      strip-redefines))

(defn kudzu->glsl
  ([shader]
   (let [processed-shader (preprocess shader)]
     (validate-kudzu-keys processed-shader)
     (processed-kudzu->glsl processed-shader)))
  ([first-chunk & other-chunks]
   (kudzu->glsl (apply combine-chunks (cons first-chunk other-chunks)))))
