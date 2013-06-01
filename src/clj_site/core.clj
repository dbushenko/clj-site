(ns clj-site.core
  (:use clj-site.util
        markdown.core
        hiccup.core)
  (:gen-class))

(def ^:dynamic *separator* java.io.File/separatorChar)
(def ^:dynamic *title-prefix* "#title:")
(def ^:dynamic *layout-prefix* "#layout:")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Utilities

(declare parse-data-file)

(defn- index-of [e coll]
  (first (keep-indexed #(if (= e %2) %1) coll)))

(defn- add-separator [name]
  (if (= (last name) *separator*)
    name
    (str name *separator*)))

(defn- make-html-file-name [file-name & [num]]
  "Creates html-file name from the markdown file name"
  (let [n (if (or (nil? num) (zero? num)) "" (inc num))
        fn (cond
            (.endsWith file-name ".md") (.substring file-name 0 (- (count file-name) 3))
            (.endsWith file-name ".clj") (.substring file-name 0 (- (count file-name) 4))
            :else file-name)]
    (str fn n ".html")))

(defn print-usage []
  "Just prints the usage."
  (println "clj-site <dir-name>"))


(defn- find-previous-post [post posts-list]
  (let [i (index-of post posts-list)]
    (if (> (count posts-list) (inc i))
      (nth posts-list (inc i))
      nil)))

(defn- find-next-post [post posts-list]
  (let [i (index-of post posts-list)]
    (if (zero? i)
      nil
      (nth posts-list (dec i)))))

(defn find-data-files [cfg dir-name selector]
  (filter (memfn isFile)
          (-> (str dir-name (get cfg selector))
              clojure.java.io/file
              file-seq)))

(defn parse-data-files-list [data-files]
  (reverse (sort-by :file-name (doall (map parse-data-file data-files)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Business functions

(def ^:dynamic *current-file-name*)

(defn parse-header-line [line]
  (if (or (< (.length line) 4)
          (not (.startsWith line "#"))
          (= (.indexOf line ":") -1))
    (throw (Exception. "Wrong header in file " *current-file-name*)))
  (let [s (.substring line 1)
        splitter (.indexOf s ":")
        content (.trim (.substring s (inc splitter)))
        header (.substring s 0 splitter)]
    (vector (keyword header) content)))

(defn parse-file-content [raw-lines]
  (loop [lines raw-lines
         header []]
    (let [line (first lines)]
      (if (or (empty? lines)
              (not (.startsWith line "#")))
        (let [h (apply hash-map (flatten header))]
          (assoc h :contents (md-to-html-string (reduce #(str %1 %2 "\n") lines))))
        (recur (next lines)
               (conj header (parse-header-line line)))))))

(defn parse-tags [tags-string]
  (let [arr (.split tags-string " ")]
    (filter #(not (empty? %)) arr)))

(defn parse-data-file [file]
  (binding [*current-file-name* (.getName file)]
    (println *current-file-name*)
    (let [parts (.split *current-file-name* "-")
          lines (-> file
                    clojure.java.io/reader
                    line-seq)
          d (parse-file-content lines)
          data (if (nil? (:tags d))
                 d
                 (assoc d :tags (parse-tags (:tags d))))
          ]
      (-> data
          (assoc :file-name *current-file-name*)
          (assoc :html-file-name (make-html-file-name *current-file-name*))))))

(defn generate-post [post cfg posts-list]
  "Generates one post"
  (let [dir (:output-dir cfg)
        base-dir (:base-dir cfg)
        layouts-dir (:layouts-dir cfg)
        file-name (str base-dir dir (make-html-file-name (:file-name post)))
        layout-name (:layout post)
        layout (read-string (slurp (str base-dir layouts-dir layout-name ".clj")))
        eval-data (concat '(do (use 'clj-site.util)) (list layout))]
    (binding [*config* cfg
              *post* post
              *post-prev* (find-previous-post post posts-list)
              *post-next* (find-next-post post posts-list)]
      (spit file-name (html (eval eval-data))))))


(defn generate-page [page cfg pages-list]
  "Generates page"
  (let [dir (:output-dir cfg)
        base-dir (:base-dir cfg)
        layouts-dir (:layouts-dir cfg)
        layout-name (:layout page)
        layout (read-string (slurp (str base-dir layouts-dir layout-name ".clj")))
        eval-data (concat '(do (use 'clj-site.util)) (list layout))]
    (binding [*config* cfg
              *page* page]
      (spit (str base-dir dir (make-html-file-name (:file-name page)))
            (html (eval eval-data))))))

(defn generate-list [file-list-name layout-name cfg posts-list]
  (let [page-size (:page-size cfg)
        pages-count* (int (/ (count posts-list) page-size))
        pages-count (if (> (count posts-list) (* page-size pages-count*)) (inc pages-count*) pages-count*)
        dir (:output-dir cfg)
        base-dir (:base-dir cfg)
        layouts-dir (:layouts-dir cfg)
        layout (read-string (slurp (str base-dir layouts-dir layout-name ".clj")))
        eval-data (concat '(do (use 'clj-site.util)) (list layout))]
    (doseq [page-num (range pages-count)]
      (binding [*config* cfg
                *page-prev* (if (> (dec pages-count) page-num) (make-html-file-name file-list-name (inc page-num)))
                *page-next* (if-not (zero? page-num) (make-html-file-name file-list-name (dec page-num)))
                *page-num* page-num
                *page-posts* (nth (doall (partition-all page-size posts-list)) page-num)]
        (spit (str base-dir dir (make-html-file-name file-list-name page-num))
              (html (eval eval-data)))))))

(defn generate-post-list [cfg posts-list]
  (generate-list (:paginated-layout cfg) (:paginated-layout cfg) cfg posts-list))

(defn get-all-tags [posts-list]
  (vec
   (apply hash-set
          (flatten (reduce conj []
                           (map :tags posts-list))))))

(defn generate-tags-list [cfg posts-list]
  (let [tags (get-all-tags posts-list)]
    (dorun (map (fn [tag] (generate-list tag (:tags-layout cfg) cfg
                                         (filter #(not (nil? (index-of tag (:tags %))))
                                                 posts-list)))
                tags))))

(defn make-config [cfg dir-name]
  "Makes default config, applies user settings from file config.clj"
  {:url-base (or (:url-base cfg) "/")
   :page-size (or (:page-size cfg) 5)
   :posts-dir (add-separator (or (:posts-dir cfg) "posts"))
   :pages-dir (add-separator (or (:pages-dir cfg) "pages"))
   :layouts-dir (add-separator (or (:layouts-dir cfg) "layouts"))
   :output-dir (add-separator (or (:output-dir cfg) "output"))
   :base-dir dir-name
   :paginated-layout (or (:paginated-layout cfg) "paginated")
   :tags-layout (or (:tags-layout cfg) "tags")})

(defn -main [ & [input-dir-name]]
  (if (nil? input-dir-name)
    (print-usage)
    (let [dir-name (add-separator input-dir-name)
          
          cfg (-> (str dir-name "config.clj")
                  slurp
                  read-string
                  (make-config dir-name))

          posts-files (find-data-files cfg dir-name :posts-dir)
          posts-list (parse-data-files-list posts-files)
          
          pages-files (find-data-files cfg dir-name :pages-dir)
          pages-list (parse-data-files-list pages-files)
          ]

      (dorun (map #(generate-page % cfg pages-list) pages-list))
      (dorun (map #(generate-post % cfg posts-list) posts-list))
      (generate-post-list cfg posts-list)
      (generate-tags-list cfg posts-list))))
