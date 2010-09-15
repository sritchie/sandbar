;; Copyright (c) Brenton Ashworth. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file COPYING at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns sandbar.example.session-with-reload-demo
  "Illustrating how to use wrap-reload together with sessions. This demo
   relies on a feature that will be included in Ring after version 0.3.0.
   If you would like do this before that change is made then you will need to
   make the changes shown here http://bit.ly/cf1q0N to your own fork of Ring."
  (:use (ring.adapter jetty)
        (ring.middleware reload)
        (ring.middleware.session memory)
        (compojure core)
        (hiccup core page-helpers)
        (sandbar stateful-session)))

(defn layout [title counter link]
  [:div
   [:h2 (str title " Counter")]
   [:div (str "The current value of counter is " counter)]
   [:div (link-to "/" "Home")]
   [:div link]])

(defn functional-handler
  "Functional style of working with a session."
  [request]
  (let [counter (if-let [counter (-> request :session :counter)]
                  (+ counter 1)
                  1)]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (html
            (layout "Functional" counter (link-to "/stateful" "Stateful")))
     :session {:counter counter}}))

(defn stateful-handler
  "Stateful style of working with a session."
  []
  (let [counter (+ 1 (session-get :counter 0))]
    (do (session-put! :counter counter)
        (html
         (layout "Stateful" counter (link-to "/functional" "Functional"))))))

(defroutes my-routes
  (GET "/functional*" request (functional-handler request))
  (GET "/stateful*" [] (stateful-handler))
  (ANY "*" [] (html 
               [:div
                [:h2 "Functional vs Stateful Session Demo"]
                [:div (link-to "/functional" "Functional")] 
                [:div (link-to "/stateful" "Stateful")]])))

;; Do this in a development environment where you want to use
;; wrap-reload and not wipe your session data on each request. This
;; will also allow you to inspect the session for debugging purposes.
(defonce my-session (atom {}))

(def app (-> my-routes
             (wrap-stateful-session {:store (memory-store my-session)})
             (wrap-reload ['sandbar.example.session-with-reload-demo])))

(defn run []
  (run-jetty (var app) {:join? false :port 8080}))

