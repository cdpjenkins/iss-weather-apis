(ns iss-weather-apis.core
  (:require-macros [hiccups.core :as h]
                   [cljs.core.async.macros :as m :refer [go alt!]])
  (:require [clojure.browser.repl :as repl]
            [domina :as dom]
            [hiccups.runtime :as hiccupsrt]
            [ajax.core :refer [GET POST]]
            [cljs.core.async
             :as async
             :refer [<! >! chan close! sliding-buffer put! alts! timeout]]
            ol.Map
            ol.layer.Tile
            ol.layer.Vector
            ol.source.OSM
            ol.source.Vector
            ol.View
            ol.proj
            ol.control.FullScreen
            ol.style.Style
            ol.style.Fill
            ol.style.Circle
            ol.style.Stroke
            ol.Overlay
            goog.dom))

(enable-console-print!)

(def weather-atom (atom nil))



;; (repl/connect "http://localhost:9000/repl")

(defn move-iss! [osm-map longitude latitude weather]
  (let [position (ol.proj.transform #js [longitude latitude]
                                    "EPSG:4326"
                                    "EPSG:3857")
        satellite-overlay (ol.Overlay.
                           #js {:position position
                                :element (dom/by-id "satellite")
                                :positioning "center-center"})

        weather-icon-div (dom/by-id "weather-icon")
        weather-icon-overlay (ol.Overlay.
                              #js {:position position
                                   :element weather-icon-div
                                   :positioning "top-right"})]

    ;; Clear the old overlays. Possibily inefficient but hey ho.
    (-> osm-map
        (.getOverlays)
        (.clear))

    ;; Create the satellite overlay
    (.addOverlay osm-map satellite-overlay)

    ;; Weather icon
    (dom/destroy-children! weather-icon-div)
    (dom/append! weather-icon-div
                 (h/html
                  [:img {:src (str "http://openweathermap.org/img/w/"
                                   (get-in weather [0 "icon"])
                                   ".png")}]))
    (.addOverlay osm-map weather-icon-overlay)

    ;; Centre the map on the ISS location
    (-> osm-map
        (.getView)
        (.setCenter position))))

(defn make-map []
  (let [source (ol.source.OSM. #js {:layer "osm"})
        raster (ol.layer.Tile. #js {:source source})
        view (ol.View. #js {:center #js [37.41, 8.82]
                            :zoom 4
                            :maxZoom (get props :max-zoom 18)})
        osm-map (ol.Map. #js {:layers #js [raster]
                              :target "demoMap"
                              :view   view})]
    (dom/append! (dom/by-id "overlays")
                 (h/html
                  [:img#satellite {:src "/satellite.png"}]
                  [:div#crew.overlay]
                  [:div#weather.overlay]
                  [:div#weather-icon.overlay]))
    osm-map))

(defn init []
  (let [data-channel (chan)
        osm-map (make-map)]
    (go
      (loop []
        ;; API Management proxy for the ISS location API
        ;;
        (GET "/iss-and-weather"
         ;;"https://apimdev1035.hursley.ibm.com/spaceorg/sb/iss/positionweather?client_id=73ed7c19-89f5-4b4f-8421-03869c3cd80c" 
             {:handler (fn [response]
                         (let [latitude (get-in response ["iss_position" "latitude"])
                               longitude (get-in response ["iss_position" "longitude"])
                               weather (get-in response ["weather"])]
                           (move-iss! osm-map longitude latitude weather)))})
        (<! (timeout 10000))
        (recur)))))

(when js/window
  (set! (.-onload js/window) init))

