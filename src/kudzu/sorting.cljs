(ns kudzu.sorting
  (:require [clojure.walk :refer [walk]]
            [clojure.set :refer [union intersection]]))

(defn inner-symbols [form]
  (walk (fn [s]
          (if (coll? s)
            (if (map? s)
              (inner-symbols (vals s))
              (inner-symbols s))
            (if (symbol? s)
              #{s}
              #{})))
        #(apply union %)
        form))

(defn sort-expressions [expressions dependencies]
  (loop [remaining-names (set (keys expressions))
         sorted-expressions []]
    (if (empty? remaining-names)
      (seq sorted-expressions)
      (let [next-expression-name (some #(when (empty?
                                               (intersection remaining-names
                                                             (dependencies %)))
                                          %)
                                       remaining-names)]
        (if next-expression-name
          (recur (disj remaining-names next-expression-name)
                 (conj sorted-expressions
                       [next-expression-name
                        (expressions next-expression-name)]))
          (throw (ex-info "KUDZU: Cyclic dependency detected"
                          {:functions (str remaining-names)})))))))

(defn sort-fns [functions]
  (sort-expressions
   functions
   (into {}
         (mapv (fn [[fn-name fn-content]]
                 [fn-name
                  (disj (intersection (set (keys functions))
                                      (inner-symbols fn-content))
                        fn-name)])
               functions))))

(defn sort-structs [structs]
  (sort-expressions
   structs
   (into {}
         (mapv (fn [[struct-name struct-content]]
                 [struct-name
                  (intersection
                   (set (keys structs))
                   (inner-symbols struct-content))])
               structs))))
