(ns colligere.core
  (:require 
    [clojure.string :refer (join lower-case replace)]
    [clojure.data.xml :refer (parse-str)] 
    [clojure.zip :refer (xml-zip)]
    [org.httpkit.client :as http]
    [clojure.data.zip.xml :refer  [xml-> xml1-> attr attr= text]]))

(def conf (read-string  (slurp "config.edn")))

(defn login [host] (str "https://" host "/cgi/login.cgi"))
(defn ipmi [host] (str "https://" host "/cgi/ipmi.cgi"))


(defn get-sid [host auth]
  (let [{:keys [headers :as resp]} @(http/post (login host) {:form-params auth :insecure? true})]
    (second (re-find #",SID\=([a-z]*);" (:set-cookie headers)))))

(defn get-health [{:keys [host auth]}]
  (let [sid (get-sid host auth)
        headers {"Cookie" (str "SID=" sid ";" " mainpage=health; subpage=servh_sensor")
                 "Accept" "application/json" }
        form-data {"SENSOR_INFO.XML" "(1,ff)"}
        {:keys [body]}  @(http/post (ipmi host) {:headers headers :form-params form-data :insecure? true})
        ]
    (xml-zip (parse-str body))))

(defn norm-name [m] 
  (-> m :NAME (replace " " "-") lower-case keyword))

(defn server-health [host]
  (map #(attr % (juxt norm-name :READING)) (xml-> (get-health host) :IPMI :SENSOR_INFO :SENSOR)))

(defn state [] (map server-health (conf :hosts)))


