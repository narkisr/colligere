(ns colligere.core
  (:gen-class true)
  (:require 
    [taoensso.timbre :as timbre :refer [debug  info  warn  error  fatal  report]]
    [org.httpkit.timer :as timer]
    [riemann.client :as r]
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
        {:keys [body status error]}  @(http/post (ipmi host) {:headers headers :form-params form-data :insecure? true}) ]
    (if (or error (not (= status 200))) 
      (warn error)
      (xml-zip (parse-str body)))))

(defn norm-name [m] 
  (-> m :NAME (replace " " "-") lower-case keyword))

(defn server-health [host]
  (when-let [health (get-health host)]
    (map #(attr % (juxt norm-name :READING)) (xml-> health :IPMI :SENSOR_INFO :SENSOR))))

(defn state [] 
  (map #(some->> % server-health flatten (apply hash-map) (merge {:host (:host %)})) (conf :hosts)))


(defn client [] (r/tcp-client {:host (get-in conf [:riemann :host])}))

(def memo-client (memoize client))

(defn metric [m host] {:service host :state "running" :metric m :tags ["ipmi"]})

(defn send-metrics []
  (let [c (memo-client)]
    (doseq [{:keys [host cpu-temp]} (state) 
          :when host
          :let [cpu-unhex (Integer/parseInt (replace cpu-temp "c000" "") 16) m (metric cpu-unhex host)] ]
    (debug m)
    (-> c (r/send-event m) (deref 5000 ::timeout)))))

(defn schedule []
   (loop [] 
     (timer/schedule-task (conf :poll) (send-metrics))
     (Thread/sleep (conf :poll)) 
     (recur)))

(defn -main [& args]
  (schedule))

