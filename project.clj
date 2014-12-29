(defproject com.lemondronor/turboshrimp-h264j "0.2.0"
  :description (str "An AR.Drone video deocder for the turboshrimp library "
                    "that uses h264j.")
  :url "https://github.com/wiseman/turboshrimp-h264j"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[com.twilight/h264 "0.0.1"]
                 [org.clojure/clojure "1.6.0"]]
  :profiles {:test
             {:dependencies [[com.lemondronor/turboshrimp "0.3.1"]]
              :resource-paths ["test-resources"]}})
