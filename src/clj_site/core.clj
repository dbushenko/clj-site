(ns clj-site.core
  (:use clj-site.util
        markdown.core
        hiccup.core)
  (:gen-class))

(def ^:dynamic *separator* java.io.File/separatorChar)
(def ^:dynamic *title-prefix* "#title:")
(def ^:dynamic *layout-prefix* "#layout:")

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

(defn- parse-raw-post [lines file-name]
  "Tries to parse arbitrary markdown file"
  (let [l1 (first lines)
        l2 (second lines)
        cont (reduce str (next (next lines)))]
    (if-not (.startsWith l1 *title-prefix*)
      (throw (Exception. (str "No " *title-prefix* " in " file-name))))
    (if-not (.startsWith l2 *layout-prefix*)
      (throw (Exception. (str "No " *layout-prefix* " in " file-name))))
    {:title (.trim (.substring l1 (count *title-prefix*)))
     :file-name file-name
     :html-file-name (make-html-file-name file-name)
     :layout (.trim (.substring l2 (count *layout-prefix*)))
     :contents (md-to-html-string cont)} ))

(defn- parse-raw-page [lines file-name]
  "Tries to parse arbitrary markdown file"
  (let [l1 (first lines)
        cont (reduce str (next (next lines)))]
    (if-not (.startsWith l1 *layout-prefix*)
      (throw (Exception. (str "No " *layout-prefix* " in " file-name))))
    {:file-name file-name
     :html-file-name (make-html-file-name file-name)
     :layout (.trim (.substring l1 (count *layout-prefix*)))
     :contents (read-string cont)} ))

(defn parse-post-file [file]
  "Tries to parse post markdown file. File name should contain date: YYYY-MM-DD-file-name.md"
  (let [file-name (.getName file)
        parts (.split file-name "-")
        file-date (str (first parts) "-" (second parts) "-" (nth parts 2))
        lines (-> file
                  clojure.java.io/reader
                  line-seq)]
    (if (< (count lines) 3)
      (throw (Exception. (str "Too small post contents in " file-name))))
    
    (assoc (parse-raw-post lines file-name) :file-date file-date) ))

(defn parse-page-file [file]
  "Tries to parse page markdown file."
  (let [file-name (.getName file)
        lines (-> file
                  clojure.java.io/reader
                  line-seq)]
    (if (< (count lines) 2)
      (throw (Exception. (str "Too small page contents in " file-name))))
    (parse-raw-page lines file-name)))

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


(defn generate-post [post cfg posts-list]
  "Generates post"
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

(defn contains-pagination? [code]
  (let [data (apply hash-set (flatten code))]
    (or (contains? data 'page-prev)
        (contains? data 'page-next)
        (contains? data 'page-posts)
        (contains? data 'page-num))))

(defn generate-page [page cfg pages-list posts-list]
  "Generates page"
  (let [paginated? (contains-pagination? (:contents page))
        page-size (:page-size cfg)
        pages-count* (int (/ (count posts-list) page-size))
        pages-count (if (> (count posts-list) (* page-size pages-count*)) (inc pages-count*) pages-count*)
        
        dir (:output-dir cfg)
        base-dir (:base-dir cfg)
        layouts-dir (:layouts-dir cfg)
        layout-name (:layout page)
        layout (read-string (slurp (str base-dir layouts-dir layout-name ".clj")))
        eval-data (concat '(do (use 'clj-site.util)) (list layout))]
    (binding [*config* cfg
              *page* page]

      (if-not paginated?
        
        (spit (str base-dir dir (make-html-file-name (:file-name page)))
              (html (eval eval-data)))
        
        (doseq [page-num (range pages-count)]
          (binding [*page-prev* (if (> (dec pages-count) page-num) (make-html-file-name (:file-name page) (inc page-num)))
                    *page-next* (if-not (zero? page-num) (make-html-file-name (:file-name page) (dec page-num)))
                    *page-num* page-num
                    *page-posts* (nth (doall (partition-all page-size posts-list)) page-num)]
            (spit (str base-dir dir (make-html-file-name (:file-name page) page-num))
                  (html (eval (eval eval-data))))))))))

(defn make-config [cfg dir-name]
  "Makes default config, applies user settings from file config.clj"
  {:url-base (or (:url-base cfg) "/")
   :page-size (or (:page-size cfg) 5)
   :posts-dir (add-separator (or (:posts-dir cfg) "posts"))
   :pages-dir (add-separator (or (:pages-dir cfg) "pages"))
   :layouts-dir (add-separator (or (:layouts-dir cfg) "layouts"))
   :output-dir (add-separator (or (:output-dir cfg) "output"))
   :base-dir dir-name})

(defn -main [ & [input-dir-name]]
  (if (nil? input-dir-name)
    (print-usage)
    (let [dir-name (add-separator input-dir-name)
          
          cfg (-> (str dir-name "config.clj")
                  slurp
                  read-string
                  (make-config dir-name))

          posts-files (filter (memfn isFile)
                              (-> (str dir-name (:posts-dir cfg))
                                  clojure.java.io/file
                                  file-seq))

          posts-list (reverse (sort-by :file-name (doall (map parse-post-file posts-files))))
          
          pages-files (filter (memfn isFile)
                              (-> (str dir-name (:pages-dir cfg))
                                  clojure.java.io/file
                                  file-seq))

          pages-list (reverse (sort-by :file-name (doall (map parse-page-file pages-files))))]

      (dorun (map #(generate-page % cfg pages-list  posts-list) pages-list))
      (dorun (map #(generate-post % cfg posts-list) posts-list)))))