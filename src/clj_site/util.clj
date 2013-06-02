(ns clj-site.util
  (:gen-class))

(def ^:dynamic *config*)

(def ^:dynamic *post*)
(def ^:dynamic *post-prev*)
(def ^:dynamic *post-next*)

(def ^:dynamic *posts-list*)
(def ^:dynamic *page-posts*)
(def ^:dynamic *page*)
(def ^:dynamic *page-num*)
(def ^:dynamic *page-prev*)
(def ^:dynamic *page-next*)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Utilities

(defn index-of [e coll]
  (first (keep-indexed #(if (= e %2) %1) coll)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Business functions

(defn posts-with-tag [tag]
  (filter #(not (nil? (index-of tag (:tags %))))
          *posts-list*))

(defn config [] *config*)
(defn post-content [] (:contents *post*))
(defn post-prev [] *post-prev*)
(defn post-next [] *post-next*)
(defn page-prev [] *page-prev*)
(defn page-next [] *page-next*)
(defn page-num [] *page-num*)
(defn page-posts [] *page-posts*)


(defn url-for [page-name]
  (str (:url-base *config*) page-name))

(defn link-for [name page-name]
  [:a {:href (url-for page-name)} name])

(defn iframe-link-for [name page-name]
  [:a {:href "#",
       :onclick (str "parent.document.location='" (url-for page-name) "'")}
   name])
