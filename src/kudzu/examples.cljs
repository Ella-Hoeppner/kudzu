(ns kudzu.examples
  (:require [kudzu.core :refer [kudzu->glsl]]))

(def example-frag-glsl
  (kudzu->glsl
   '{:outputs {frag-color vec4}
     :uniforms {resolution vec2}
     :main
     ((= frag-color
         (vec4 (/ gl_FragCoord.xy resolution)
               0
               1)))}))

(def example-vert-glsl
  (kudzu->glsl
   '{:inputs {vertex-pos vec2}
     :uniforms {projection-matrix mat4}
     :main
     ((= gl_Position
         (* projection-matrix
            (vec4 position 0 1))))}))

(def conditional-example 
  (kudzu->glsl
   '{:main ((=vec3 color)

            ; Conditional blocks are denoted by a keyword.
            (:if false
                 (= color (vec3 1)))
            (:else-if false
                      (= color (vec3 0.5)))
            (:else
             (= color (vec3 0)))

            ; Ternaries are also supported.
            (= color (if true
                       (vec3 1)
                       (vec3 0))))}))

(def loop-example 
  (kudzu->glsl
   '{:main
     ((=vec3 color)
      ; There are two ways to express for-loops in kudzu.

      ; The first is with syntax similar to Clojure's "for"
      ; a binding vector that takes a name and an end value.
      (:for [x 10]
            (* color x))
      
      ; You can also add an initial value,
      (:for [y 0 10]
            (* color y))
      ; and a step value.
      (:for [z 0 10 2]
            (* color z))

      ; The second syntax is more C-like, for when fine-grained
      ; control is needed.
      (:for (=uint i 0) (<= i 0xFF) (>> i 3)
            (*= color y))

      ; while loops are similar, taking a single expression.
      (:while (< x 0.5)
              (+= x 0.1))
      (:while true
              (= color (rand))
              "break")

      ; Kudzu also supports naked blocks, for local bindings
      ; that fall out of scope once the block is finished.
      (:block
       (=float unreachable 0))
      
      ; This would throw an error, as "unreachable" is no
      ; longer in scope.
      (++ unreachable))}))

(def threading-example
  (kudzu->glsl
   '{:uniforms {resolution vec2}
     :main
     ((=vec2 pos (/ gl_FragCoord resolution))

      ; Most of Clojure's threading macros are supported. 
      (= pos (-> pos
                 (* 100)
                 noise))
      (= pos (->> pos
                  (texture tex)
                  .rgb
                  vec3))
      (= fragColor (as-> pos p
                     (texture tex p)
                     (.rgb p)
                     (mod p (vec3 0.5))
                     (vec4 p 1)))

      ; In the case of modifying a variable, there are modifier versions.
      (=-> pos
           (* 100)
           noise)
      (=->> pos
            (texture tex)
            .rgb
            vec3))}))

(def struct-example 
  ; Structs are defined in a hash-map, where the key is the name of
  ; the struct, and the value is a vector of the struct's fields and
  ; their types.
  (kudzu->glsl
   '{:structs
     {Ray
      [pos vec3
       dir vec3]

      ; Structs can reference other structs,
      Record
      [pos vec3
       material Material]
      
      ; and can be defined in any order, as they're sorted at compile time.
      Material
      [albedo vec3]}
     :functions
     ; Structs can be passed to functions lke any other type.
     {ray-at
      (Ray
       [ray Ray
        t float]
       (Ray (+ ray.pos
               (* ray.dir t))
            ray.dir))}
     :main
     (; structs are declared with the "=Name" syntax, like other types.
      ; They can be constructed by calling the name with values for 
      ; their fields as arguments. Keep in mind that struct names
      ; are case-sensitive.
      (=Ray ray (Ray (vec3 0) (vec3 0.5)))

      ; Fields can be accesed with dot syntax,
      (=vec3 pos ray.pos)
      (=vec3 dir ray.dir)

      (=vec3 pos (.pos ray))
      (=vec3 dir (.dir ray))

      ; including when the field is itself a struct.
      (=vec3 color record.material.albedo)
      (=vec3 color (.albedo (.material record))))}))

(def hollow-demos-link
  "https://github.com/Ella-Hoeppner/hollow/tree/main/src/hollow/demos")

(defn p-element [content]
  (let [e (js/document.createElement "p")]
    (set! e.innerHTML content)
    e))

(defn textarea-element [content]
  (let [e (js/document.createElement "textarea")]
    (set! e.textContent content)
    (set! e.cols 80)
    (set! e.rows (inc (count (filter #{"\n"} (seq content)))))
    e))

(defn init []
  (js/window.addEventListener
   "load"
   (fn []
     (doseq [element
             [(p-element "Simple fragment shader:")
              (textarea-element example-frag-glsl)
              (p-element "Simple vertex shader:")
              (textarea-element example-vert-glsl)
              (p-element (str "For more examples, see the <a href="
                              hollow-demos-link
                              ">hollow demos</a>."))]]
       (js/document.body.appendChild element)))))
