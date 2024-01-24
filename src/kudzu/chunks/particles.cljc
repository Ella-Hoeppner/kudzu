(ns kudzu.chunks.particles
  (:require [kudzu.tools :refer [unquotable]]))

(defn particle-vert-source [texture-type]
  (unquotable
   (let [glsl-texture-type (if (= texture-type
                                  :f8)
                             'sampler2D
                             'usampler2D)
         vec-type (if (= texture-type
                         :f8)
                    '=vec4
                    '=uvec4)
         texture-max (texture-type {:f8 1
                                    :u16 '(float "0x0000FFFFu")
                                    :u32 '(float "0xFFFFFFFFu")})]
     '{:precision {float highp
                   int highp
                   ~glsl-texture-type highp}
       :outputs {particle-pos vec2}
       :uniforms {particle-tex ~glsl-texture-type
                  radius float}
       :main ((=int agentIndex (/ gl_VertexID "6"))
              (=int corner (% gl_VertexID "6"))

              (=ivec2 tex-size (textureSize particle-tex "0"))

              (=vec2 texPos
                     (/ (+ 0.5 (vec2 (% agentIndex tex-size.x)
                                     (/ agentIndex tex-size.x)))
                        (vec2 tex-size)))

              (~vec-type particle-color (texture particle-tex texPos))
              (= particle-pos (/ (vec2 particle-color.xy) ~texture-max))

              (= gl_Position
                 (vec4 (- (* (+ particle-pos
                                (* radius
                                   (- (* 2
                                         (if (|| (== corner "0")
                                                 (== corner "3"))
                                           (vec2 0 1)
                                           (if (|| (== corner "1")
                                                   (== corner "4"))
                                             (vec2 1 0)
                                             (if (== corner "2")
                                               (vec2 0 0)
                                               (vec2 1 1)))))
                                      1)))
                             2)
                          1)
                       0
                       1)))})))

(defn particle-frag-source [texture-type]
  (unquotable
   (let [texture-max (texture-type {:f8 1
                                    :u16 '(float "0x0000FFFFu")
                                    :u32 '(float "0xFFFFFFFFu")})
         output-type (if (= texture-type
                            :f8)
                       'vec4
                       'uvec4)]
     '{:precision {float highp
                   int highp}
       :uniforms {radius float
                  resolution float}
       :inputs {particle-pos vec2}
       :outputs {frag-color ~output-type}
       :main ((=vec2 pos (/ gl_FragCoord.xy resolution))
              (=float dist (distance pos particle-pos))
              (:when (> dist radius)
                     "discard")
              (= frag-color (~output-type ~texture-max 0 0 ~texture-max)))})))

(def particle-vert-3d-source-u32
  '{:uniforms {time float
               radius float
               perspective mat4
               particle-tex usampler2D
               cube-distance float}
    :outputs {square-pos vec2
              vertex-pos vec3}
    :main
    ((=int particle-index (/ gl_VertexID "6"))
     (=int corner (% gl_VertexID "6"))

     (= square-pos
        (vec2 (if (|| (== corner "0")
                      (== corner "3")
                      (== corner "2"))
                -1
                1)
              (if (|| (== corner "1")
                      (== corner "4")
                      (== corner "2"))
                -1
                1)))

     (=ivec2 tex-size (textureSize particle-tex "0"))

     (=uvec4 particle-tex-color
             (texelFetch particle-tex
                         (ivec2 (% particle-index tex-size.x)
                                (/ particle-index tex-size.x))
                         "0"))
     (=vec3 particle-pos
            (-> particle-tex-color
                .xyz
                vec3
                (/ ~(dec (Math/pow 2 32)))
                (* 2)
                (- 1)))

     (= vertex-pos
        (vec3 (+ particle-pos.xy
                 (* radius
                    square-pos))
              (- particle-pos.z
                 (+ 1 cube-distance))))

     (= gl_Position (* (vec4 vertex-pos 1)
                       perspective))
     (= gl_Position (/ gl_Position gl_Position.w)))})

(def particle-frag-3d-source-u32
  '{:uniforms {resolution vec2
               radius float
               light-pos vec3
               ambient-light float}
    :inputs {vertex-pos vec3
             square-pos vec2}
    :outputs {frag-color vec4}
    :main
    ((=float horizontalDist (length square-pos))
     (:when (> horizontalDist 1) "discard")
     (=float depth-dist (sqrt (- 1 (* horizontalDist horizontalDist))))
     (=vec3 surface-pos
            (- vertex-pos
               (vec3 0 0 (* radius depth-dist))))
     (=vec3 surface-normal (vec3 square-pos depth-dist))
     (=vec3 light-diff (normalize (- light-pos surface-pos)))
     (=float light-factor (mix ambient-light
                              1
                              (max 0 (dot surface-normal light-diff))))
     (= frag-color (vec4 light-factor 1)))})
