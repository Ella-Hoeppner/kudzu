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
