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

(defn index-of [e coll]
  (first (keep-indexed #(if (= e %2) %1) coll)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Business functions

;; <strong>posts-with-tag</strong> returns only those posts which contain the specified tag.
;; Available while generating post
;;
;; <i>Parameters:</i>
;; <ul>
;; <li>tag -- tag name;</li>
;; </ul>
;; <i>Output:</i> posts list with the specified tag.
(defn posts-with-tag [tag]
  (filter #(not (nil? (index-of tag (:tags %))))
          *posts-list*))

;; Returns global configuration.
(defn config [] *config*)

;; Returns the current post.
;; Available only while generating post.
(defn current-post [] *post*)

;; Returns the content of the current post without the header.
;; Available only while generating post.
(defn post-content [] (:contents *post*))

;; Returns the title of the current post.
;; Available only while generating post.
(defn post-title [] (:title *post*))

;; Returns the html file name for the current post.
;; Available only while generating post.
(defn post-html [] (:html-file-name *post*))

;; The name of the previous post in the common posts list.
;; Available only while generating post.
(defn post-prev [] *post-prev*)

;; The name of the next post in the common posts list.
;; Available only while generating post.
(defn post-next [] *post-next*)

;; The name of the previous page of posts list (or nil if not available).
;; Available only while generating lists.
(defn page-prev [] *page-prev*)

;; The name of the next page of posts list (or nil if not available).
;; Available only while generating lists.
(defn page-next [] *page-next*)

;; Current page number.
;; Available only while generating lists.
(defn page-num [] *page-num*)

;; The list of posts on the current page.
;; Available only while generating lists.
(defn page-posts [] *page-posts*)

;; Generates an URL for the current page using the base URL.
(defn url-for [page-name]
  (str (:url-base *config*) page-name))

;; Generates an html <a href="..."></a> for the specified page name.
;;
;; <i>Parameters:</i>
;; <ul>
;; <li>name -- the text which will be shown to the user;</li>
;; <li>page-name -- the name of the page for which the URL should be generated;</li>
;; </ul>
;; <i>Output:</i> html <a> tag.
(defn link-for [name page-name]
  [:a {:href (url-for page-name)} name])

;; Generates an html <a href="..."></a> for the specified page name.
;; The specified page will be opened in the parrent document.
;; This element should be used in the included pages (in iframes).
;;
;; <i>Parameters:</i>
;; <ul>
;; <li>name -- the text which will be shown to the user;</li>
;; <li>page-name -- the name of the page for which the URL should be generated;</li>
;; </ul>
;; <i>Output:</i> html <a> tag.
(defn parent-link-for [name page-name]
  [:a {:href "#",
       :onclick (str "parent.document.location='" (url-for page-name) "'")}
   name])

;; Generates the code to include the specified page into the current page.
;; Creates an iframe for it.
(defn inner-page [name & [options]]
  [:iframe (conj {:src (url-for name),
                  :seamless "seamless",
                  :frameBorder "0"}
                 options)])

