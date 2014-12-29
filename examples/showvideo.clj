(ns showvideo
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [com.lemondronor.turboshrimp.h264j :as decode]
            [com.lemondronor.turboshrimp.pave :as pave]
            [com.lemonodor.gflags :as gflags]
            [com.lemonodor.xio :as xio])
  (:import (java.awt Graphics)
           (java.net Socket)
           (javax.swing JFrame JPanel))
  (:gen-class))


(gflags/define-boolean "file"
  false
  "Read an input file.")

(gflags/define-float "fps"
  30.0
  "Sets the desired frames-per-second when reading an input file.")

(gflags/define-boolean "reduce-latency"
  true
  "Turns on latency reduction.")


(defn display-frame [decoder ^JPanel view frame]
  (let [^BufferedImage img (decoder frame)]
    (when img
      (.drawImage (.getGraphics view) img 0 0 view))))


(def drone-hostname "192.168.1.1")
(def drone-video-port 5555)


(defn drone-video-input-stream [hostname]
  (println "Connecting to drone at" hostname)
  (.getInputStream (Socket. hostname drone-video-port)))


(defn get-input-stream [args]
  (println (gflags/flags))
  (if (gflags/flags :file)
    (if (seq args)
      (io/input-stream (first args))
      System/in)
    (drone-video-input-stream (if (seq args) (first args) drone-hostname))))


(defn -main [& args]
  (println args)
  (let [args (gflags/parse-flags (cons nil args))
        ^JFrame window (JFrame. "Drone video")
        ^JPanel view (JPanel.)
        lrq (pave/make-frame-queue
             :reduce-latency? (gflags/flags :reduce-latency))
        decoder (decode/decoder)
        is (get-input-stream args)
        frame-count (atom 0)
        frame-delay (if (gflags/flags :file)
                      (/ 1000.0 (gflags/flags :fps))
                      nil)]
    (.setBounds window 0 0 640 360)
    (.add (.getContentPane window) view)
    (.setVisible window true)
    (.start
     (Thread.
      (fn []
        (let [frame (pave/pull-frame lrq 100)]
          (if frame
            (do
              (swap! frame-count inc)
              (display-frame decoder view frame)
              (recur))
            (println "Showed" @frame-count "frames, dropped"
                     @(:num-dropped-frames lrq) "frames."))))))
    (loop [frame (pave/read-frame is)]
      (if frame
        (do
          (pave/queue-frame lrq frame)
          (when frame-delay
            (Thread/sleep frame-delay))
          (recur (pave/read-frame is)))
        (do
          (Thread/sleep 100000))))))
