(defproject canary "0.0.1-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [uswitch/lambada "0.1.2"]
                 [cheshire "5.8.1"]]
  :main canary.core
  :target-path "target/"
  :uberjar-name "canary.jar"
  :profiles {:uberjar {:aot :all}})
