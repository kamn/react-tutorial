(ns react-re-frame.core
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require [reagent.core :as reagent]
              [re-frame.core :as re-frame]
              [react-re-frame.config :as config]))

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
(re-frame/register-handler
 :initialize-db
 (fn  [_ _]
   default-db))

;; ===
;; Subs
;; ===
(re-frame/register-sub
 :name
 (fn [db]
   (reaction (:name @db))))

(re-frame/register-sub
 :comments
 (fn [db]
   (reaction (:comments @db))))

(re-frame/register-sub
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
    [:form.commentForm
      [:input {:type "text"
               :placeholder "Your Name"
               :value (:author new-comment)
               :on-change #()}]
      [:input {:type "text"
               :placeholder "Say something..."
               :value (:text new-comment)
               :on-change #()}]
      [:input {:type "submit"
               :value "post"}]]))


(defn comment-box []
  (let [comments (re-frame/subscribe [:comments])
        new-comment (re-frame/subscribe [:new-comment])]
    (fn []
      [:div.commentBox
        [:h1 "Comments"]
        [comment-list @comments]
        [comment-form @new-comment]])))

(defn mount-root []
  (reagent/render [comment-box]
                  (.getElementById js/document "app")))

(defn ^:export init [] 
  (re-frame/dispatch-sync [:initialize-db])
  (mount-root))
