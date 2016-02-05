(ns react-re-frame.core
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require [reagent.core :as reagent]
              [re-frame.core :as re-frame :refer [register-sub register-handler
                                                  subscribe dispatch dispatch-sync]]
              [react-re-frame.config :as config]
              [ajax.core :refer [GET POST]]))

(when config/debug?
  (println "dev mode"))

;; ===
;; Data
;; ===

(def default-db
  {:name "re-frame"
   :comments [
              {:author "Aaaa"
               :comment "This is a test"}
              {:author "Bbbb"
               :comment "This is another test"}]
   :new-comment {
                 :author ""
                 :comment ""}})



;; ===
;; Handlers
;; ===
(register-handler
 :initialize-db
 (fn  [_ _]
   default-db))



(register-handler
 :load-comments
 (fn  [db _]
   (GET "/api/comments"
      {:response-format :json
       :handler #(js/console.log "Success")
       :error-handler #(js/console.log "Error")})))

(register-handler
 :submit-comment
 (fn  [db [_ e]]
   ;;TODO: Prevent default
   (.preventDefault e)
   ;;TODO: Get the comment data
   (POST "/api/comments"
      {:params {"id" (js/Date.now)
                "author" ""
                "text" ""}
       :response-format :json
       :handler #(js/console.log "Success")
       :error-handler #(js/console.log "Error")})
   db))

;; ===
;; Subs
;; ===
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

;; ===
;; Views
;; ===
(defn comment-cmp [data]
    [:div.comment
      [:h2.commentAuthor (:author data)]
      [:span (:comment data)]])

(defn comment-list [comments]
    [:div.commentList
      (map comment-cmp comments)])

(defn comment-form [new-comment]
  (fn []
    [:form.commentForm {:on-submit #(dispatch-sync [:submit-comment %])}
      [:input {:type "text"
               :placeholder "Your Name"
               :value (:author new-comment)
               :on-change #()}]
      [:input {:type "text"
               :placeholder "Say something..."
               :value (:text new-comment)
               :on-change #()}]
      [:input {:type "submit"
               :value "Post"}]]))


(defn comment-box []
  (let [comments (subscribe [:comments])
        new-comment (subscribe [:new-comment])]
    (fn []
      [:div.commentBox
        [:h1 "Comments"]
        [comment-list @comments]
        [comment-form @new-comment]])))

(defn mount-root []
  (reagent/render [comment-box]
                  (.getElementById js/document "app")))

(defn ^:export init [] 
  (dispatch-sync [:initialize-db])
  (mount-root))
