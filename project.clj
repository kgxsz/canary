(defproject canary "0.0.1-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [com.amazonaws/aws-lambda-java-core "1.0.0"]
                 [com.taoensso/faraday "1.9.0"]
                 [metosin/muuntaja "0.6.4"]
                 [clj-time "0.15.0"]
                 [danlentz/clj-uuid "0.1.7"]
                 [medley "1.2.0"]]
  :main canary.handler
  :target-path "target/"
  :uberjar-name "canary.jar"
  :source-paths ["src"]
  :profiles {:uberjar {:aot :all}})
