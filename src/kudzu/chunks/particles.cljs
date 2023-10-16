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
       :outputs {particlePos vec2}
       :uniforms {particleTex ~glsl-texture-type
                  radius float}
       :main ((=int agentIndex (/ gl_VertexID "6"))
              (=int corner (% gl_VertexID "6"))

              (=ivec2 texSize (textureSize particleTex "0"))

              (=vec2 texPos
                     (/ (+ 0.5 (vec2 (% agentIndex texSize.x)
                                     (/ agentIndex texSize.x)))
                        (vec2 texSize)))

              (~vec-type particleColor (texture particleTex texPos))
              (= particlePos (/ (vec2 particleColor.xy) ~texture-max))

              (= gl_Position
                 (vec4 (- (* (+ particlePos
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
       :inputs {particlePos vec2}
       :outputs {fragColor ~output-type}
       :main ((=vec2 pos (/ gl_FragCoord.xy resolution))
              (=float dist (distance pos particlePos))
              (:if (> dist radius)
                   "discard")
              (= fragColor (~output-type ~texture-max 0 0 ~texture-max)))})))

(def particle-vert-3d-source-u32
  '{:uniforms {time float
               radius float
               perspective mat4
               particleTex usampler2D
               cubeDistance float}
    :outputs {squarePos vec2
              vertexPos vec3}
    :main
    ((=int particleIndex (/ gl_VertexID "6"))
     (=int corner (% gl_VertexID "6"))

     (= squarePos
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

     (=ivec2 texSize (textureSize particleTex "0"))

     (=uvec4 particleTexColor
             (texelFetch particleTex
                         (ivec2 (% particleIndex texSize.x)
                                (/ particleIndex texSize.x))
                         "0"))
     (=vec3 particlePos
            (-> particleTexColor
                .xyz
                vec3
                (/ ~(dec (Math/pow 2 32)))
                (* 2)
                (- 1)))

     (= vertexPos
        (vec3 (+ particlePos.xy
                 (* radius
                    squarePos))
              (- particlePos.z
                 (+ 1 cubeDistance))))

     (= gl_Position (* (vec4 vertexPos 1)
                       perspective))
     (= gl_Position (/ gl_Position gl_Position.w)))})

(def particle-frag-3d-source-u32
  '{:uniforms {resolution vec2
               radius float
               lightPos vec3
               ambientLight float}
    :inputs {vertexPos vec3
             squarePos vec2}
    :outputs {fragColor vec4}
    :main
    ((=float horizontalDist (length squarePos))
     (:if (> horizontalDist 1) "discard")
     (=float depthDist (sqrt (- 1 (* horizontalDist horizontalDist))))
     (=vec3 surfacePos
            (- vertexPos
               (vec3 0 0 (* radius depthDist))))
     (=vec3 surfaceNormal (vec3 squarePos depthDist))
     (=vec3 lightDiff (normalize (- lightPos surfacePos)))
     (=float lightFactor (mix ambientLight
                              1
                              (max 0 (dot surfaceNormal lightDiff))))
     (= fragColor (vec4 lightFactor 1)))})
