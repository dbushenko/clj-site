(ns clj-site.util
  (:gen-class))

(def ^:dynamic *config*)

(def ^:dynamic *post*)
(def ^:dynamic *post-prev*)
(def ^:dynamic *post-next*)

(def ^:dynamic *page-posts*)
(def ^:dynamic *page*)
(def ^:dynamic *page-num*)
(def ^:dynamic *page-prev*)
(def ^:dynamic *page-next*)

(defn config []
  *config*)

(defn post-content []
  (:contents *post*))

(defn post-prev []
  *post-prev*)

(defn post-next []
  *post-next*)

(defn page-content []
  (:contents *page*))

(defn page-prev [] *page-prev*)
(defn page-next [] *page-next*)
(defn page-num [] *page-num*)
(defn page-posts [] *page-posts*)

(defn url-for [page-name]
  (str (:url-base *config*) page-name))

(defn link-for [name page-name]
  [:a {:href (url-for page-name)} name])
