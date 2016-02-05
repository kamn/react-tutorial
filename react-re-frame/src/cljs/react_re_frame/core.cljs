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
  {:comments [
              {:author "Aaaa"
               :text "This is a test"}
              {:author "Bbbb"
               :text "This is another test"}]
   :new-comment {
                 :author ""
                 :text ""}})

;; -- Event Handlers ----------------------------------------------------------
(register-handler
 :initialize-db
 (fn  [_ _]
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
       :handler #(dispatch [:load-comments-success %])
       :error-handler #(js/console.log "Error")})))

(register-handler
 :load-comments-success
 (fn  [db [_ val]]
   (println val)
   db))

(register-handler
 :submit-comment
 (fn  [db [_ e]]
   (.preventDefault e)
   ;;TODO: Get the comment data
   (POST "/api/comments"
      {:params {"id" (js/Date.now)
                "author" (get-in db [:new-comment :author])
                "text" (get-in db [:new-comment :author])}
       :response-format :json
       :format :json
       :handler #(dispatch [:submit-comments-success %])
       ;;TODO - Copy example error logs
       :error-handler #(js/console.error "Error")})
   (-> db
       (update :comments #(conj % (:new-comment db)))
       (assoc :new-comment {:author "" :text ""}))))
    

(register-handler
 :submit-comments-success
 (fn  [db [_ val]]
   (println val)
   db))

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
    [:div.comment
      [:h2.commentAuthor (:author data)]
      [:span (:text data)]])

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
