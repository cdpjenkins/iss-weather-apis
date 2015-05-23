(defproject iss-weather-apis "0.1.0-SNAPSHOT"
  :description "A simple demo showing the weather directly beneath the
                International Space Station in real time"
  :url "http://example.com/FIXME"

  :dependencies [;; clj
                 [org.clojure/clojure "1.6.0"]
                 [clj-http "1.1.1"]
                 [compojure "1.3.4"]
                 [ring/ring-jetty-adapter "1.3.2"]
                 [org.clojure/data.json "0.2.6"]
                 [hiccup "1.0.5"]

                 ;; cljs
                 [org.clojure/clojurescript "0.0-3211"]
                 [domina "1.0.3"]
                 [hiccups "0.3.0"]
                 [cljs-ajax "0.3.11"]
                 [cljsjs/openlayers "3.3.0-0"]

                 ;; Both
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]

                 ;; Grrrr
                 [org.clojure/tools.nrepl "0.2.10"]]

  :node-dependencies [[source-map-support "0.2.8"]]

  :plugins [[lein-cljsbuild "1.0.4"]
            [lein-npm "0.4.0"]
            [lein-ring "0.9.3"]
            [cider/cider-nrepl "0.9.0-SNAPSHOT"]]

  :ring {:handler iss-weather-apis.web/app
         :uberwar-name "iss-weather.war"
         :nrepl {:start? true}}


  :source-paths ["src"]

  :clean-targets ["out" "out-adv"]

  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["cljs_src"]
              :compiler {
                :main iss-weather-apis.core
                :output-to "resources/public/generated/iss_weather_apis.js"
                :output-dir "resources/public/generated"
                :optimizations :advanced
                :cache-analysis true
                :pretty-print true
                :closure-extra-annotations #{"api" "observable"}
                :source-map true}}
             {:id "release"
              :source-paths ["cljs_src"]
              :compiler {
                :main iss-weather-apis.core
                :output-to "out-adv/iss_weather_apis.min.js"
                :output-dir "out-adv"
                :optimizations :advanced
                :pretty-print false
                :closure-extra-annotations #{"api" "observable"}}}]}
  :min-lein-version "2.0.0"

  :main iss-weather-apis.web
  :aot :all
  :uberjar-name "iss-weather-apis.jar")
