(ns iss-weather-apis.web
  (:use     [compojure.core])
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.util.response :as resp]
            [ring.middleware.resource :as resource]
            [clj-http.client :as client]
            [ring.adapter.jetty :only [run-jetty]]
            [clojure.data.json :as json]
            [hiccup.core :as h]
            [hiccup.element :as element]
            [hiccup.page :as page])
  (:gen-class))

(defn proxy-get
  "Proxy a GET to a URL. We need this because cross-domain AJAX requests are painful."
  [url]
  (println "GET " url)
  (let [response (client/get url)]
    (if (= 200 (:status response))
      (-> (:body response)
           (json/read-str)
           (pr-str)
           (resp/response)
           (resp/content-type "application/edn"))
      (-> "That didn't work"
           (resp/response)
           (resp/status (:status response))))))

(defn get-json
  "Do a GET request to the URL and parse the JSON that is returned"
  [url]
  (let [response (client/get url)]
    (if (= 200 (:status response))
      (-> (:body response)
          (json/read-str))
      (throw (RuntimeException. (str "HTTP return code: " (:status response)))))))

(defn get-iss-and-weather
  "Returns a map that contains the ISS position and the weather on the 
   surface beneath the ISS"
  []
  (let [position (get-json "http://api.open-notify.org/iss-now.json")
             latitude (get-in position ["iss_position" "latitude"])
             longitude (get-in position ["iss_position" "longitude"])
             weather (get-json (str "http://api.openweathermap.org/data/2.5/weather?"
                                    "lat=" latitude
                                    "&lon=" longitude))]
         (merge position weather)))


(defn cached [f cache-time]
  (let [cache (atom [0 nil])
        maybe-call (fn [[last-time cached-value]]
                     (let [current-time (System/currentTimeMillis)]
                       (if (> (- current-time last-time) cache-time)
                         [current-time (f)]
                         [last-time cached-value])))]
    (fn []
      (let [[_ result] (swap! cache maybe-call)]
        result))))

(def cached-iss-and-weather (cached get-iss-and-weather 10000))

(defroutes main-routes
  ;; International Space State current location
  (GET "/iss-now"
       []
       (proxy-get  "http://api.open-notify.org/iss-now.json"))

  ;; Mars Weather from the Curiosity Rover
  (GET "/marsweather"
       []
       (proxy-get "http://marsweather.ingenology.com/v1/latest/"))

  (GET "/astros"
       []
       (proxy-get "http://api.open-notify.org/astros.json"))

  (GET "/weather"
       {params :params}
       (proxy-get (str "http://api.openweathermap.org/data/2.5/weather?lat=" (:latitude params) 
                       "&lon=" (:longitude params))))

  (GET "/iss-and-weather"
       []
       (-> (cached-iss-and-weather)
           (pr-str)
           (resp/response)
           (resp/header "Cache-Control" "max-age=0, no-store")
           (resp/content-type "application/edn")))

  (GET "/"
       []
       (resp/response
        (h/html
         [:head
          [:title "ISS Weather"]
          [:script {:src "/generated/iss_weather_apis.js"}]]
         [:body
          [:div {:id "demoMap" :style "height:80%,width:90%" }]
          [:div#overlays]]))))

(def app
  (-> main-routes
      (resource/wrap-resource "public")
      (handler/site)))

(defn make-server
  ([]
     (make-server 8000))
  ([port]
     (let [port port]
       (ring.adapter.jetty/run-jetty (var app) {:port port :join? false}))))

(defn -main
  ([]
   (make-server (Integer/parseInt (System/getenv "VCAP_APP_PORT"))))
  ([port]
   (make-server (Integer/parseInt port))))

(comment
  (-main "3000")
  (client/get iss-location-url)
)
