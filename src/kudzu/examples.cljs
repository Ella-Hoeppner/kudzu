(ns kudzu.examples
  (:require [kudzu.core :refer [kudzu->glsl]]
            [kudzu.tools :refer [unquotable]]))

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

(def syntax-example
  (kudzu->glsl 
   '{:main 
     ;variables are declared with an '=type' syntax
     ((=float x 1)
      ;int literals must be strings
      (=int y "1")
      ;and unsigned ints are the same but with a 'u' suffix
      (=uint z "1u")

      ;reasignment
      (= x 2)

      ;modifiers
      (++ x)
      (*= x 2)

      ;'/=', "^=" and '%=' modifiers must be strings
      ("/=" x 2)

      ;forward declarations are supported
      (=float a)
      (= a 1)

      ;vectors use a constuctor function in addition to a type prefix
      (=vec4 v (vec4 1 2 3 4))
      ;mixing types is supported when coercion is possible
      (=ivec2 iv (ivec2 "1" 2))
      ;glsl swizzle syntax works with vec variables
      (=vec2 swizz (vec2 v2.y v2.x))
      ;Clojure syntax is used for anonymous values
      (=vec2 swizz-two (.xy (fn-that-returns-vec3)))
      ;.rgba and .stpq are also supported
      (=vec3 color (.rgb (texture tex (.st position))))

      ;Clojure bools are converted to GLSL bools
      ;Clojure question mark syntax can be used
      (=bool b? true)
      ;all the usual boolean operators are supported
      (=bool and? (&& true false))
      (=bool not? (! false))

      ;bitwise operators are supported
      (=uint bit-and (& 0xFF 0x0F))
      (=uint bit-shift-right (>> 0xFF 4))
      (>>= bit-and "3")

      ;matrices are declared similarly to vectors
      (=mat4 m4 (mat4 1 2 3 4
                      5 6 7 8
                      9 10 11 12
                      13 14 15 16)) 
      ;and indexed like arrays
      (=vec4 col [m4 "0"])
      (=float val [[m4 "0"] "0"])
      
      ;outputs are assigned, not declared
      (= fragColor (vec4 1 0 0 1)))}))
(js/console.log syntax-example)

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

      ; The second syntax is more C-like, when finer-grained
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

(def layout-example
  (kudzu->glsl
    ;if you need multiple outputs for a single shader
    ;you can specify the layout like so
   '{:outputs {Color vec4 
               Direction vec3
               Weight float}
     :layout {Color 0
               Direction 1
               Weight 2}
     :main ((= Color (vec4 1))
            (= Direction (vec3 0))
            (= Weight 1))}))

(def defines-and-constants-example
  ;kudzu supports defining constant variables, 
  ;which are replaced with their literal values at compile time
  ;as well as C-style defines, which are inserted by the precompiler
  (kudzu->glsl
   '{:constants {:PI 3.141592
                 two 2
                 ;kudzu expressions can also used 
                 :zero (vec3 0)}
     :defines {(mul x y)
               (* x y)}
     :main ((=float x :PI)
            (=float tau (mul :PI two))
            (:if (all (equal :zero (vec3 0)))
                 (= x tau)))}))

(def array-example 
  (kudzu->glsl  
   '{:uniforms {;declaring an array uniform
                positions [vec3 "5"]}
     :main (;arrays are indexed with an integer literal
            (=vec3 first [positions "0"])
            ;array literal syntax 
            (= [vec3 "4"]
               rest
               [vec3
                [positions "1"]
                [positions "2"]
                [positions "3"]
                [positions "4"]]))}))

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
