(ns kudzu.compiler
  (:require [clojure.string :refer [join]]
            [kudzu.tools :refer [clj-name->glsl]]
            [kudzu.validation :refer [validate-uniforms
                                      validate-name
                                      validate-structs
                                      validate-defines
                                      validate-in-outs
                                      validate-precision
                                      validate-functions
                                      throw-str]]
            [kudzu.sorting :refer [sort-fns
                                   sort-structs]]))

(defn num->glsl [num]
  (apply str
         (reverse
          (some #(when (not= (first %) \0)
                   %)
                (iterate rest
                         (reverse
                          (.toFixed num 20)))))))

(defn type->glsl [type-expression]
  (if (vector? type-expression)
    (str (clj-name->glsl (first type-expression))
         "["
         (second type-expression)
         "]")
    (clj-name->glsl type-expression)))

(def infix-ops
  '#{+ - / * % < > != == <= >= || && & "^^" "^" | << >>})

(def modifying-assigners
  '#{+= *= -= "/=" "^=" "%=" |= &= <<= >>=})

(defn expression->glsl [expression]
  (cond
    (symbol? expression) (clj-name->glsl expression)

    (number? expression) (num->glsl expression)

    (string? expression) expression

    (boolean? expression) (str expression)

    (vector? expression)
    (if (= (count expression) 2)
      ; array accessor
      (let [[array-name array-index] expression]
        (str (expression->glsl array-name)
             "["
             (expression->glsl array-index)
             "]"))
      ; array literal
      (let [[array-type & init-values] expression]
        (str (type->glsl array-type)
             "[]("
             (join "," (map expression->glsl init-values))
             ")")))

    (seq? expression)
    (let [[f & args] expression]
      (cond
        (= '++ f) (str (expression->glsl (first args)) "++")
        (= '-- f) (str (expression->glsl (first args)) "--")

        (and (= f '-) (= (count args) 1))
        (str "-" (expression->glsl (first args)))

        (and (= f '/) (= (count args) 1))
        (str "(1./" (expression->glsl (first args)) ")")

        (modifying-assigners f)
        (str (clj-name->glsl (first args))
             " "
             f
             " "
             (expression->glsl (second args)))

        (infix-ops f)
        (str "("
             (join (str " " f " ")
                   (map expression->glsl args))
             ")")

        (= 'if f)
        (let [[conditional true-expression false-expression] args]
          (str "("
               (expression->glsl conditional)
               " ? "
               (expression->glsl true-expression)
               " : "
               (expression->glsl false-expression)
               ")"))

        (= '= f)
        (if (= (count args) 2)
          ; assignment
          (str (expression->glsl (first args))
               " = "
               (expression->glsl (second args)))
          ; 3 args, initialization with detached type
          (let [[variable-type variable-name variable-value] args]
            (str (type->glsl variable-type)
                 " "
                 (expression->glsl variable-name)
                 (when (some? variable-value)
                   (str " = " (expression->glsl variable-value))))))

        (and (symbol? f) (= "=" (first (str f))))
        (let [[variable-name variable-value] args]
          (str (expression->glsl (symbol (subs (str f) 1)))
               " "
               (expression->glsl variable-name)
               (when (some? variable-value)
                 (str " = " (expression->glsl variable-value)))))

        (and (symbol? f) (= "." (first (str f))))
        (str (expression->glsl (first args))
             (clj-name->glsl f))

        :else (str (clj-name->glsl f)
                   "("
                   (join ", "
                         (map expression->glsl args))
                   ")")))

    :else (throw-str (str "KUDZU: Can't parse expression: " expression))))

(defn is-statement-block? [statement]
  (and (seq? statement)
       (#{"if" :if
          "when" :when
          "else" :else
          "else-if" "else if" "elseif" "elif" :else-if :elseif :elif
          "while" :while
          "for" :for
          "block" :block}
        (first statement))))

(defn statement->lines [statement]
  (if (is-statement-block? statement)
    (let [[statement-type & statement-args] statement]
      (if (#{:if "if"} statement-type)
        (if (not= 3 (count statement-args))
          (throw-str
           (str "KUDZU: Invalid number of argments to :if\n\nArguments given:"
                (apply str (interleave (repeat "\n") statement-args))))
          (let [[conditional & clauses] statement-args
                [true-lines false-lines]
                (map #(map (partial str "  ")
                           (if (and (seq? %)
                                    (= (first %) :block))
                             (mapcat statement->lines (rest %))
                             (statement->lines %)))
                     clauses)]
            (concat (list (str "if ("
                               (expression->glsl conditional)
                               ") {\n"))
                    true-lines
                    (list "}\n  else {\n")
                    false-lines
                    (list "}\n"))))
        (let [[block-start consumed-statement-args]
              (cond
                (#{:when "when"} statement-type)
                [(str "if ("
                      (expression->glsl (first statement-args))
                      ") {")
                 1]

                (#{:while "while"} statement-type)
                [(str "while ("
                      (expression->glsl (first statement-args))
                      ") {")
                 1]

                (#{:else "else"} statement-type)
                ["else {" 0]

                (#{:else-if :elseif :elif "else if" "else-if" "elseif" "elif"}
                 statement-type)
                [(str "else if ("
                      (expression->glsl (first statement-args))
                      ") {")
                 1]

                (#{:for "for"} statement-type)
                (if (vector? (first statement-args))
                  (let [loop-definition (first statement-args)
                        [binding-name & loop-args] loop-definition]
                    (validate-name binding-name
                                   (str " for loop " loop-definition))
                    (let [[initial-value max-value increment-value]
                          (case (count loop-args)
                            1 [0 (first loop-args)]
                            2 loop-args
                            3 loop-args
                            (throw-str (str "KUDZU: Invalid for loop definition "
                                        loop-definition)))
                          parse-value (fn [value]
                                        (if (number? value)
                                          (str value)
                                          (expression->glsl value)))]
                      [(str "for (int "
                            (clj-name->glsl binding-name)
                            " = "
                            (parse-value initial-value)
                            "; "
                            (clj-name->glsl binding-name)
                            " < "
                            (parse-value max-value)
                            "; "
                            (clj-name->glsl binding-name)
                            (if increment-value
                              (str " += " (parse-value increment-value))
                              "++")
                            ") {")
                       1]))
                  [(str "for ("
                        (join "; "
                              (map expression->glsl
                                   (take 3 statement-args)))
                        ") {")
                   3])

                (#{:block "block"} statement-type)
                ["{" 0])]
          (concat (list (str block-start "\n"))
                  (map (partial str "  ")
                       (mapcat statement->lines
                               (drop consumed-statement-args statement-args)))
                  (list "}\n")))))
    (if (and (seq? statement)
             (= (first statement) 'do))
      (mapcat statement->lines
              (rest statement))
      (try (list (str (expression->glsl statement) ";\n"))
           #?(:cljs (catch :default e
                      (throw (ex-info (str "KUDZU: Error while compiling statement "
                                           statement)
                                      e))))
           #?(:clj (catch Exception e 
                     (throw (Exception. (ex-info (str "KUDZU: Error while compiling statement " statement)
                                                 e)))))))))

(defn precision->glsl [[glsl-type type-precision]]
  (str "precision " type-precision " " glsl-type ";\n"))

(defn uniform->glsl [[uniform-name uniform-type]]
  (str "uniform "
       (type->glsl uniform-type)
       " "
       (clj-name->glsl uniform-name)
       ";\n"))

(defn in-out->glsl [layout qualifiers in-or-out [in-out-name in-out-type]]
  (str (when layout
         (when-let [layout-pos (layout in-out-name)]
           (str "layout(location = " layout-pos ") ")))
       (when qualifiers
         (when-let [qualifier (qualifiers in-out-name)]
           (str qualifier " ")))
       in-or-out
       " "
       (type->glsl in-out-type)
       " "
       (clj-name->glsl in-out-name)
       ";\n"))

(defn struct->glsl [[struct-name struct-definition]]
  (str "struct "
       (clj-name->glsl struct-name)
       " {\n"
       (apply str
              (map (fn [[field-name field-type]]
                     (str " "
                          (type->glsl field-type)
                          " "
                          (clj-name->glsl field-name)
                          ";\n"))
                   (partition 2 struct-definition)))
       "};\n"))

(defn define->glsl [[define-pattern define-replacement]]
  (str "#define "
       (expression->glsl define-pattern)
       " "
       (expression->glsl define-replacement)
       "\n"))

(defn function-body->glsl [fn-body returns?]
  (let [statement-strings (vec (mapcat statement->lines fn-body))]
    (apply str
           (map (partial str "  ")
                (cond-> statement-strings
                  returns? (update (dec (count statement-strings))
                                   (partial str "return ")))))))

(defn function->glsl [[fn-name fn-definition]]
  (if (seq? fn-definition)
    (let [[return-type signature & body] fn-definition]
      (str (type->glsl return-type)
           " "
           (clj-name->glsl fn-name)
           "("
           (join ", "
                 (map (fn [[arg-name arg-type]]
                        (str (type->glsl arg-type)
                             " "
                             (clj-name->glsl arg-name)))
                      (partition 2 signature)))
           ")\n{\n"
           (function-body->glsl body (not= return-type 'void))
           "}\n"))
    (apply str
           (map #(function->glsl [fn-name %])
                fn-definition))))

(defn processed-kudzu->glsl [{:keys [precision
                                     uniforms
                                     layout
                                     qualifiers
                                     inputs
                                     outputs
                                     structs
                                     defines
                                     functions
                                     global]}]
  (validate-uniforms uniforms)
  (validate-structs structs)
  (validate-precision precision)
  (validate-in-outs inputs outputs layout qualifiers)
  (validate-defines defines)
  (validate-functions functions)
  (apply str
         (flatten
          ["#version 300 es\n"
           (map precision->glsl precision)
           (map uniform->glsl uniforms)
           (map (partial in-out->glsl layout qualifiers "in") inputs)
           (map (partial in-out->glsl layout qualifiers "out") outputs)
           (map struct->glsl (sort-structs structs))
           (map define->glsl defines)
           (mapcat statement->lines global)
           (map function->glsl (sort-fns functions))])))
