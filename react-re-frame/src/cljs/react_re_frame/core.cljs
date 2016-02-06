(ns react-re-frame.core
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require [reagent.core :as reagent]
              [re-frame.core :as re-frame :refer [register-sub register-handler
                                                  subscribe dispatch dispatch-sync]]
              [react-re-frame.config :as config]
              [ajax.core :refer [GET POST]]))

(when config/debug?
  (println "dev mode"))

;; -- Data --------------------------------------------------------------------
(def default-db
  {:comments []
   :new-comment {:author ""
                 :text ""}})

;; -- Event Handlers ----------------------------------------------------------
(register-handler
 :initialize-db
 (fn  [_ _]
   (dispatch [:load-comments])
   (js/setInterval #(dispatch [:load-comments]) 2000)
   default-db))

(register-handler
 :new-com-author
 (fn  [db [_ val]]
   (assoc-in db [:new-comment :author] val)))

(register-handler
 :new-com-text
 (fn  [db [_ val]]
   (assoc-in db [:new-comment :text] val)))

(register-handler
 :load-comments
 (fn  [db _]
   (GET "/api/comments"
      {:response-format :json
       :keywords? true
       :handler #(dispatch [:load-comments-success %])
       :error-handler #(js/console.error "/api/comments" (:status %) (:status-text %))})
   db))

(register-handler
 :load-comments-success
 (fn  [db [_ val]]
   (assoc db :comments val)))

(register-handler
 :submit-comment
 (fn  [db [_ e]]
   (.preventDefault e)
   (POST "/api/comments"
      {:params {"id" (js/Date.now)
                "author" (get-in db [:new-comment :author])
                "text" (get-in db [:new-comment :author])}
       :response-format :json
       :format :raw
       :handler #(dispatch [:submit-comments-success %])
       :error-handler #(js/console.error "/api/comments" (:status %) (:status-text %))})
   (-> db
       (update :comments #(conj % (:new-comment db)))
       (assoc :new-comment {:author "" :text ""}))))
    

(register-handler
 :submit-comments-success
 (fn  [db [_ val]]
   (assoc db :comments val)))

;; -- Subscription Handlers ---------------------------------------------------
(register-sub
 :name
 (fn [db]
   (reaction (:name @db))))

(register-sub
 :comments
 (fn [db]
   (reaction (:comments @db))))

(register-sub
 :new-comment
 (fn [db]
   (reaction (:new-comment @db))))

;; -- View Components ---------------------------------------------------------
(defn comment-cmp [data]
    [:div.comment {:key (:id data)}
      [:h2.commentAuthor (:author data)]
      [:span {:dangerouslySetInnerHTML 
               {:__html (js/marked (:text data) 
                                   (js-obj {"sanitize" true}))}}]])

(defn comment-list [comments]
    [:div.commentList
      (map comment-cmp comments)])

(defn comment-form [new-comment]
  (fn []
    [:form.commentForm {:on-submit #(dispatch-sync [:submit-comment %])}
      [:input {:type "text"
               :placeholder "Your Name"
               :value (:author @new-comment)
               :on-change #(dispatch [:new-com-author (.-target.value %)])}]
      [:input {:type "text"
               :placeholder "Say something..."
               :value (:text @new-comment)
               :on-change #(dispatch [:new-com-text (.-target.value %)])}]
      [:input {:type "submit"
               :value "Post"}]]))

(defn comment-box []
  (let [comments (subscribe [:comments])
        new-comment (subscribe [:new-comment])]
    (fn []
      [:div.commentBox
        [:h1 "Comments"]
        [comment-list @comments]
        [comment-form new-comment]])))

(defn mount-root []
  (reagent/render [comment-box]
                  (.getElementById js/document "app")))

;; -- Entry Point -------------------------------------------------------------
(defn ^:export init [] 
  (dispatch-sync [:initialize-db])
  (mount-root))
