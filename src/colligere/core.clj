(ns colligere.core
  (:require [org.httpkit.client :as http]) 
  )

(def conf)

(def login "https://" (conf :host) "/cgi/login.cgi")
(def ipmi "https://" (conf :host) "/cgi/ipmi.cgi")


(defn get-sid []
  (let [{:keys [headers]} @(http/post login {:form-params auth :insecure? true})]
    (second (re-find #",SID\=([a-z]*);" (:set-cookie headers)))))

(defn server-health []
  (let [sid (get-sid)
        headers {"Cookie" (str "SID=" sid ";" " mainpage=health; subpage=servh_sensor")
                 "Accept" "application/json" }
        form-data {"SENSOR_INFO.XML" "(1,ff)"}
        res  @(http/post ipmi {:headers headers :form-params form-data :insecure? true})
        ]
    (clojure.pprint/pprint res)
   )
  )

(server-health)
