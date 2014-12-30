(ns showvideo
  "Example of decoding AR.Drone video with asynchronous display and
  latency reduction."
  (:require [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [com.lemondronor.turboshrimp.h264j :as decode]
            [com.lemondronor.turboshrimp.pave :as pave]
            [com.lemonodor.gflags :as gflags]
            [com.lemonodor.xio :as xio])
  (:import (java.awt Graphics)
           (java.awt.image BufferedImage)
           (java.net Socket)
           (javax.swing JFrame JPanel))
  (:gen-class))


(gflags/define-boolean "file"
  false
  "Read an input file instead of connecting to the drone.")

(gflags/define-float "fps"
  30.0
  "Sets the desired frames-per-second when reading an input file.")

(gflags/define-boolean "reduce-latency"
  true
  "Turns on latency reduction.")


(defn display-image
  "Renders an image to a view."
  [^JPanel view ^BufferedImage image]
  (.drawImage (.getGraphics view) image 0 0 view))


(def drone-hostname "192.168.1.1")
(def drone-video-port 5555)


(defn drone-video-input-stream
  "Connects to a drone's video feed.  Returns an InputStream."
  [hostname]
  (println "Connecting to drone at" hostname)
  (let [is (.getInputStream (Socket. hostname drone-video-port))]
    (println "Connected.")
    is))


(defn get-input-stream
  "Returns an InputStream of video data.

   Depending on the command-line flags specified, either connects to a
   drone or opens a file."
  [args]
  (if (gflags/flags :file)
    (if (seq args)
      (io/input-stream (first args))
      ;; If we weren't given a path, read from stdin.
      System/in)
    (drone-video-input-stream (if (seq args) (first args) drone-hostname))))


(defn -main [& args]
  (let [args (gflags/parse-flags (cons nil args))
        ^JFrame window (JFrame. "Drone video")
        ^JPanel view (JPanel.)
        frame-queue (pave/make-frame-queue
                     :reduce-latency? (gflags/flags :reduce-latency))
        decoder (decode/decoder)
        is (get-input-stream args)
        display-thread-done (promise)
        frame-count (atom 0)
        frame-delay (if (gflags/flags :file)
                      (/ 1000.0 (gflags/flags :fps))
                      nil)]
    (.setBounds window 0 0 640 360)
    (.add (.getContentPane window) view)
    (.setVisible window true)
    (.addShutdownHook
     (Runtime/getRuntime)
     (Thread.
      (fn []
        (println "Showed" @frame-count "frames, dropped"
                 @(:num-dropped-frames frame-queue) "frames."))))
    ;; The rendering thread pulls frames from the queue, decodes them,
    ;; and renders them.
    (doto
        (Thread.
         (fn []
           (let [frame (pave/pull-frame frame-queue 100)]
             (if frame
               (do
                 (swap! frame-count inc)
                 (->> frame
                      decoder
                      (display-image view))
                 (recur))
               (deliver display-thread-done true)))))
      (.setDaemon true)
      (.start))
    ;; Decode PaVE frames from the input and push them onto the frame
    ;; queue.
    (loop [frame (pave/read-frame is)]
      (if frame
        (do
          (pave/queue-frame frame-queue frame)
          (when frame-delay
            (Thread/sleep frame-delay))
          (recur (pave/read-frame is)))
        (do
          @display-thread-done
          (System/exit 0))))))
