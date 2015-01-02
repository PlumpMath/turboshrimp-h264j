(defproject com.lemondronor/turboshrimp-h264j "0.2.2-SNAPSHOT"
  :description "An AR.Drone video decoder for the turboshrimp library that uses the h264j H.264 decoder."
  :url "https://github.com/wiseman/turboshrimp-h264j"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :deploy-repositories [["releases" :clojars]]
  :dependencies [[com.twilight/h264 "0.0.1"]
                 [org.clojure/clojure "1.6.0"]]
  :profiles {:dev
             {:dependencies [[com.lemondronor/turboshrimp "0.3.2"]
                             [com.lemonodor/gflags "0.7.1"]]
              :resource-paths ["test-resources"]
              :plugins [[lein-cloverage "1.0.2"]]
              :source-paths ["examples"]}})
