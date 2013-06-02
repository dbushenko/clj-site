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

(defn- add-separator [name]
  (if (= (last name) *separator*)
    name
    (str name *separator*)))

(defn- make-html-file-name [file-name & [num]]
  "Creates html-file name from the markdown file name"
  (let [n (if (or (nil? num) (zero? num)) "" (str "-" (inc num)))
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
  "Parses metainformation from the header of the mardown file"
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
  "Tries to split header and the content of the mardown file."
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
  "Parses the tags line."
  (let [arr (.split tags-string " ")]
    (filter #(not (empty? %)) arr)))

(defn parse-data-file [file]
  "Parses a mardown file with the header containing metainformation."
  (binding [*current-file-name* (.getName file)]
    (println *current-file-name*)
    (let [parts (.split *current-file-name* "-")
          lines (-> file
                    clojure.java.io/reader
                    line-seq)
          d (parse-file-content lines)
          data (if (nil? (:tags d))
                 d
                 (assoc d :tags (parse-tags (:tags d))))]
      (-> data
          (assoc :file-name *current-file-name*)
          (assoc :html-file-name (make-html-file-name *current-file-name*))))))

(defn generate-post [post cfg posts-list]
  "Generates one post."
  (let [dir (:output-dir cfg)
        base-dir (:base-dir cfg)
        layouts-dir (:layouts-dir cfg)
        file-name (str base-dir dir (make-html-file-name (:file-name post)))
        layout-name (or (:layout post) (:post-layout cfg))
        layout (read-string (slurp (str base-dir layouts-dir layout-name ".clj")))
        eval-data (concat '(do (use 'clj-site.util)) (list layout))]
    (binding [*config* cfg
              *post* post
              *post-prev* (find-previous-post post posts-list)
              *post-next* (find-next-post post posts-list)
              *posts-list* posts-list]
      (spit file-name (html (eval eval-data))))))

(defn generate-list [file-list-name layout-name cfg posts-list]
  "Generates a paginated list of posts."
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


(defn get-all-tags [posts-list]
  "Gets all tags list from the posts list. Doesn't contain duplicate tags."
  (vec
   (apply hash-set
          (flatten (reduce conj []
                           (map :tags posts-list))))))

(defn generate-tags-list [cfg posts-list]
  "Generates a list of posts for each tag."
  (binding [*posts-list* posts-list]
    (let [tags (get-all-tags posts-list)]
      (dorun (map (fn [tag]
                    (generate-list tag
                                   (or (get cfg (keyword tag))
                                       (:tags-layout cfg))
                                   cfg
                                   (filter #(not (nil? (index-of tag (:tags %))))
                                           posts-list)))
                  tags)))))

(defn make-config [cfg dir-name]
  "Makes default config, applies user settings from file config.clj"
  (-> cfg
      (assoc :url-base (or (:url-base cfg) "/"))
      (assoc :page-size (or (:page-size cfg) 5))
      (assoc :posts-dir (add-separator (or (:posts-dir cfg) "posts")))
      (assoc :layouts-dir (add-separator (or (:layouts-dir cfg) "layouts")))
      (assoc :output-dir (add-separator (or (:output-dir cfg) "output")))
      (assoc :base-dir dir-name)
      (assoc :tags-layout (or (:tags-layout cfg) "tags"))
      (assoc :post-layout (or (:post-layout cfg) "post"))))

(defn -main [ & [input-dir-name]]
  (if (nil? input-dir-name)
    (print-usage)
    (let [dir-name (add-separator input-dir-name)
          
          cfg (-> (str dir-name "config.clj")
                  slurp
                  read-string
                  (make-config dir-name))

          posts-files (find-data-files cfg dir-name :posts-dir)
          posts-list (parse-data-files-list posts-files)]

      (dorun (map #(generate-post % cfg posts-list) posts-list))
      (generate-tags-list cfg posts-list))))
