(ns com.lemondronor.turboshrimp.h264j
  "AR.Drone video decoder using h264j."
  (:import (com.twilight.h264.decoder AVFrame AVPacket H264Context
                                      H264Decoder MpegEncContext)
           (com.twilight.h264.player FrameUtils)
           (java.awt.image BufferedImage)))


(defn- copy-byte-array-to-int-array [^bytes ba ^ints ia]
  (doall (for [i (range (count ba))]
           (aset-int ia i (bit-and 0xFF (aget ba i))))))


(defn- convert [^MpegEncContext context ^AVFrame picture ^AVPacket packet]
  (let [^ints got-picture (int-array [0])
        len (.avcodec_decode_video2 context picture got-picture packet)]
    (if (or (<= len 0)
            (not (first got-picture)))
      (do
        (when (<= len 0)
          (throw (ex-info
                  (str "Unable to decode frame; len= " len)
                  {})))
        (when (not (first got-picture))
          (throw (ex-info
                  (str "Unable to decode frame; got-picture= " got-picture)
                  {})))
        false)
      true)))


(defn- decode-frame [decoder frame]
  (let [^bytes payload (:payload frame)
        ^AVPacket packet (:packet decoder)]
    (copy-byte-array-to-int-array payload (:input-buffer decoder))
    (set! (.size packet) (count payload))
    (if (convert (:context decoder) (:picture decoder) packet)
      (let [picture ^AVFrame (.displayPicture
                              ^H264Context (.priv_data
                                            ^MpegEncContext (:context decoder)))
            w (.imageWidth picture)
            h (.imageHeight picture)
            buffer-size (* w h)
            buffer (int-array buffer-size)]
        (FrameUtils/YUV2RGB picture buffer)
        (let [^BufferedImage image (BufferedImage.
                                    w h BufferedImage/TYPE_INT_RGB)]
          (.setRGB image 0 0 w h buffer 0 w)
          image))
      nil)))


(def ^:private input-buffer-size 65535)


(defrecord Decoder [input-buffer packet codec context picture])


(defn decoder []
  (let [^AVPacket packet (AVPacket.)
        codec (H264Decoder.)
        context (MpegEncContext/avcodec_alloc_context)
        picture (AVFrame/avcodec_alloc_frame)
        input-buffer (int-array
                      (+ input-buffer-size
                         MpegEncContext/FF_INPUT_BUFFER_PADDING_SIZE))]
    (.av_init_packet packet)
    (set! (.data_base packet) input-buffer)
    (set! (.data_offset packet) 0)
    (when (< (.avcodec_open context codec) 0)
      (throw (ex-info "Error opening H264 codec" {})))
    (let [decoder (map->Decoder
                   {:input-buffer input-buffer
                    :packet packet
                    :codec codec
                    :context context
                    :picture picture})]
      (fn [frame] (decode-frame decoder frame)))))
