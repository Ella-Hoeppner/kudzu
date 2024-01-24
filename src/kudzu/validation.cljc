(ns kudzu.validation
  (:require [kudzu.tools :refer [clj-name->glsl]]
            [clojure.set :refer [union
                                 difference]]))

(defn throw-str [s]
  (throw #?(:cljs s)
         #?(:clj (Exception. s))))

(defn validate-kudzu-keys [shader]
  (when-let [unrecognized-keys
             (seq (difference (set (keys shader))
                              #{:precision
                                :uniforms
                                :layout
                                :qualifiers
                                :inputs
                                :outputs
                                :structs
                                :defines
                                :functions
                                :global
                                :constants
                                :main}))]
    (throw-str (str "KUDZU: Unrecognized keys in kudzu map "
                unrecognized-keys))))

(defn name-valid? [name]
  (or (symbol? name) (string? name)))

(defn validate-name [name & [context]]
  (when-not (name-valid? name)
    (throw-str (str "KUDZU: Invalid name " name " in " context))))

(defn int-valid? [int-str]
  (and (str int-str)
       (= int-str (str #?(:cljs (js/parseInt int-str))
                       #?(:clj (parse-long int-str))))))

(defn type-valid? [t]
  (or (symbol? t)
      (string? t)
      (and (vector? t)
           (= (count t) 2)
           (type-valid? (first t))
           (int-valid? (second t)))))

(defn validate-type [t context]
  (when-not (type-valid? t)
    (throw-str (str "KUDZU: Invalid type " t context))))

(defn validate-name-type-pairs [name-type-pairs & [context]]
  (doseq [[name t] name-type-pairs]
    (validate-name name context)
    (validate-type t (str " for " name context))))

(defn validate-uniforms [uniforms]
  (validate-name-type-pairs uniforms " in uniforms"))

(defn validate-structs [structs]
  (doseq [[name struct-definition] structs]
    (validate-name name "structs")
    (when-not (and (vector? struct-definition)
                   (even? (count struct-definition)))
      (throw-str (str "KUDZU: Invalid struct definition for "
                      name
                      ": "
                      struct-definition)))
    (validate-name-type-pairs (partition 2 struct-definition)
                              (str "in struct " name))))

(defn validate-precision [precision]
  (doseq [[type-name specifier] precision]
    (validate-type type-name " in precision")
    (when-not ('#{highp mediump lowp "highp" "mediump" "lowp"} specifier)
      (throw-str (str "KUDZU: Invalid precision specifier for "
                      type-name
                      ": "
                      specifier)))))

(defn validate-in-outs [inputs outputs layout qualifiers]
  (validate-name-type-pairs inputs " in inputs")
  (validate-name-type-pairs inputs " in outputs")
  (doseq [[modifier-map modifier-name]
          [[qualifiers "qualifiers"]
           [layout "layout"]]]
    (when (seq (difference
                (set (map clj-name->glsl (keys modifier-map)))
                (union (set (map clj-name->glsl (keys inputs)))
                       (set (map clj-name->glsl (keys outputs))))))
      (throw-str (str "KUDZU: Unrecognized keys in " modifier-name " "
                      (seq (difference
                            (set (keys modifier-map))
                            (union (set (keys inputs))
                                   (set (keys outputs)))))))))
  (doseq [[qualifier-name qualifier] qualifiers]
    (when-not (name-valid? qualifier)
      (throw-str (str "KUDZU: Invalid qualifier for "
                      qualifier-name
                      ": "
                      qualifier)))))

(defn find-invalid-subexpression [expression]
  (if (or (vector? expression)
          (seq? expression))
    (some identity (map find-invalid-subexpression expression))
    (when-not (or (string? expression)
                  (symbol? expression)
                  (number? expression)
                  (keyword? expression)
                  (boolean? expression))
      expression)))

(defn validate-defines [defines]
  (doseq [[pattern replacement] defines]
    (when (find-invalid-subexpression pattern)
      (throw-str (str "KUDZU: Invalid pattern in define: " pattern)))
    (when (find-invalid-subexpression replacement)
      (throw-str (str "KUDZU: Invalid replacement in define: " replacement)))))

(defn validate-function-body [[return-type args-and-types & statements]
                              fn-name
                              & [multi-body]]
  (when-not (type-valid? return-type)
    (throw-str (str "KUDZU: Invalid return type for function "
                    fn-name
                    ": "
                    return-type)))
  (validate-name-type-pairs (partition 2 args-and-types)
                            (str " in function " fn-name))
  (when-let [invalid-subexpression (find-invalid-subexpression statements)]
    (throw-str (str "KUDZU: Invalid function body for "
                fn-name
                (when multi-body (str "(signature: "
                                      return-type
                                      " "
                                      args-and-types
                                      ")"))
                "\n Invalid expression: "
                invalid-subexpression))))

(defn validate-functions [functions]
  (doseq [[fn-name fn-body] functions]
    (when-not (name-valid? fn-name)
      (throw-str (str "KUDZU: Invalid function name: " fn-name)))
    (cond
      (seq? fn-body)
      (validate-function-body fn-body fn-name)

      (vector? fn-body)
      (doseq [sub-body fn-body]
        (validate-function-body sub-body fn-name true))

      :else (throw-str (str "KUDZU: Invalid function body for " fn-name)))))
