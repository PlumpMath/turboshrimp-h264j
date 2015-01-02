(ns timedecoding
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [com.lemondronor.turboshrimp.h264j :as decode]
            [com.lemondronor.turboshrimp.pave :as pave])
  (:gen-class))


(defn read-frames [is]
  (loop [frames '()]
    (let [f (pave/read-frame is)]
      (if f
        (recur (cons f frames))
        (reverse frames)))))


(defn -main [& args]
  (println "Reading frames...")
  (let [frames (-> (first args)
                   io/input-stream
                   read-frames)
        decoder (decode/decoder)]
    (println "Starting decoding...")
    (let [start-time (System/currentTimeMillis)]
      (doseq [f frames]
        (decoder f))
      (let [end-time (System/currentTimeMillis)
            dur (/ (- end-time start-time) 1000.0)]
        (println "Decoded" (count frames) "frames in"
                 dur "seconds:" (/ (count frames) dur) "fps")))))
