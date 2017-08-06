(defproject colligere "0.1.0"
  :description "IPMI metrics streamed into riemann"
  :url "https://github.com/narkisr/colligere"
  :license  {:name "Apache License, Version 2.0" :url "http://www.apache.org/licenses/LICENSE-2.0.html"}
  :dependencies [
      [com.taoensso/timbre "4.7.4"]
      [riemann-clojure-client "0.4.5" ]
      [org.clojure/data.xml "0.0.8"]
      [org.clojure/data.zip "0.1.2"]
      [org.clojure/clojure "1.8.0"]
      [http-kit "2.2.0"]]

  :plugins [[jonase/eastwood "0.2.4"]
            [lein-tag "0.1.0"]
            [lein-ancient "0.6.7" :exclusions [org.clojure/clojure]]
            [lein-set-version "0.3.0"] [lein-gorilla "0.4.0"]
            [lein-cljfmt "0.5.6"]]

    :main colligere.core
    :aot  [colligere.core]
  )
