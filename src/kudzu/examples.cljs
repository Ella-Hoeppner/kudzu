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

            ;conditional blocks are denoted by a keyword
            (:if false
                 (= color (vec3 1)))
            (:else-if false
                      (= color (vec3 0.5)))
            (:else
             (= color (vec3 0)))

            ;ternaries are also supported
            (= color (if true
                       (vec3 1)
                       (vec3 0))))}))

(def loop-example 
  (kudzu->glsl
   '{:main
     ((=vec3 color)
      ;there are two ways to express for-loops

      ;the first is with syntax similar to Clojure's for fn
      ;a binding vector that takes a name and an end value    
      (:for [x 10]
            (* color x))
      ;you can also add an initial value
      (:for [y 0 10]
            (* color y))
      ;and a step value
      (:for [z 0 10 2]
            (* color z))

      ;the second is more C-like, when fine-grained control is needed
      (:for (=uint i 0) (<= i 0xFF) (>> i 3)
            (*= color y))

      ;while loops are similar, taking a single expression
      (:while true
              (= color (rand))
              "break")

      ;kudzu also supports naked blocks
      (:block
       (=float unreachable 0))
      
      ;this throws an error
      (++ unreachable))}))

(def threading-example
  (kudzu->glsl
   '{:uniforms {resolution vec2}
     :main
     ((=vec2 pos (/ gl_FragCoord resolution))

      ;Most of Clojure's threading macros are supported 
      ;and can be used within one another
      (= pos (-> pos
                 (* 100)
                 noise))
      (= pos (->> pos
                  (texture tex)
                  .rgb
                  vec3))

      ;though in the case of modifying a variable, there are modifier versions
      (=-> pos
           (* 100)
           noise)
      (=->> pos
            (texture tex)
            .rgb
            vec3)

      ;thread-as 
      (= fragColor (as-> pos p
                     (texture tex p)
                     (.rgb p)
                     (mod p (vec3 0.5))
                     (vec4 p 1))))}))

(def struct-example 
  ;structs are defined in a hash-map
  ;where the key is the name of the struct
  ;and the value is a vector of the struct's fields and their types
  (kudzu->glsl
   '{:structs
     {Ray
      [pos vec3
       dir vec3]

      ;structs can reference other structs
      Record
      [pos vec3
       material Material]
      ;and can be added in any order as they're sorted at compile time
      Material
      [albedo vec3]}
     :main
     (;structs type will always be '=Name'
      ;they can be constructed by calling the (capitalized) name
      ;with the fields as arguments
      (=Ray ray (Ray (vec3 0) (vec3 0.5)))

      ;fields can be accesed with dot syntax
      (=vec3 pos ray.pos)
      (=vec3 dir ray.dir)

      ;even when the field is a struct
      (=vec3 color record.material.albedo)
      (=vec3 color (.albedo (.material record)))

      ;dot syntax can also be used in place of a function call
      (=vec3 pos (.pos ray))
      (=vec3 dir (.dir ray)))
     :functions 
     ;structs are passed to functions lke any other type
     {ray-at 
      (Ray
       [ray Ray t float]
       (Ray (+ ray.pos
               (* ray.dir t))
            ray.dir))}}))

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
