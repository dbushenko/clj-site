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

;; <strong>add-separator</strong> adds directory separator (/ for unix and \ for windows) to the end of the path.
;;
;; <i>Parameters:</i>
;; <ul>
;; <li>name -- directory name which may not end with the directory separator;</li>
;; </ul>
;; <i>Output:</i> directory name which ends with the separator.
(defn- add-separator [name]
  (if (= (last name) *separator*)
    name
    (str name *separator*)))

;; <strong>make-html-file-name</strong> creates html-file name from the markdown file name.
;; If num is specified then adds this number to file name. The number is incremented so that
;; neither file name has number 0. If the incremented number is 1 then it is ommitted.
;; Let's say we have file name "index.clj" and we need to creat 3 html files. Using this function
;; you will produce the following file names: "index.html", "index2.html", "index3.html"
;;
;; <i>Parameters:</i>
;; <ul>
;; <li>file-name -- source file name which may be ".md" or ".clj";</li>
;; <li>num -- <i>(optional)</i> number to add to the resulting file name;</li>
;; </ul>
;; <i>Output:</i> html file name.
(defn- make-html-file-name [file-name & [num]]
  (let [n (if (or (nil? num) (zero? num)) "" (str "-" (inc num)))
        fn (cond
            (.endsWith file-name ".md") (.substring file-name 0 (- (count file-name) 3))
            (.endsWith file-name ".clj") (.substring file-name 0 (- (count file-name) 4))
            :else file-name)]
    (str fn n ".html")))

;; Just prints the usage.
(defn- print-usage []
  (println "clj-site <dir-name>"))


;; <strong>find-previous-post</strong> using current post finds previous post in the sequence.
;;
;; <i>Parameters:</i>
;; <ul>
;; <li>post -- current post;</li>
;; <li>posts-list -- the posts sequence;</li>
;; </ul>
;; <i>Output:</i> previous post after the current post.
(defn- find-previous-post [post posts-list]
  (let [i (index-of post posts-list)]
    (if (> (count posts-list) (inc i))
      (nth posts-list (inc i))
      nil)))

;; <strong>find-next-post</strong> using current post finds next post in the sequence.
;;
;; <i>Parameters:</i>
;; <ul>
;; <li>post -- current post;</li>
;; <li>posts-list -- the posts sequence;</li>
;; </ul>
;; <i>Output:</i> next post after the current post.
(defn- find-next-post [post posts-list]
  (let [i (index-of post posts-list)]
    (if (zero? i)
      nil
      (nth posts-list (dec i)))))

;; <strong>parse-data-files-list</strong> finds files in the directory which we should parse.
;;
;; <i>Parameters:</i>
;; <ul>
;; <li>dir-name -- the directory to search;</li>
;; <li>cfg -- the configuration object;</li>
;; <li>selector -- the field in the config file which points to the posts directory.</li>
;; </ul>
;; <i>Output:</i> sequence of found files.
(defn- find-data-files [cfg dir-name selector]
  (filter (memfn isFile)
          (-> (str dir-name (get cfg selector))
              clojure.java.io/file
              file-seq)))

;; <strong>parse-data-files-list</strong> accepts list of data files and parses each.
;;
;; <i>Parameters:</i>
;; <ul>
;; <li>data-files -- list of java Files to parse (posts files in markdown format).</li>
;; </ul>
;; <i>Output:</i> sequence of parsed posts files.
(defn parse-data-files-list [data-files]
  (reverse (sort-by :file-name (doall (map parse-data-file data-files)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Business functions

;; Contains the name of the currently processed file.
(def ^:dynamic *current-file-name*)

;; <strong>parse-header-line</strong> -- parses metainformation from the header of the markdown file.
;;
;; <i>Parameters:</i>
;; <ul>
;; <li>line -- header line (line from the header of the file which starts with '#')</li>
;; </ul>
;; <i>Output:</i> vector of key and value.
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

;; <strong>parse-file-content</strong> -- tries to split header and the content of the markdown file.
;;
;; <i>Parameters:</i>
;; <ul>
;; <li>raw-lines -- a sequence of line from the input file;</li>
;; </ul>
;; <i>Output:</i> a map with parsed file contents.
(defn parse-file-content [raw-lines]
  (loop [lines raw-lines
         header []]
    (let [line (first lines)]
      (if (or (empty? lines)
              (not (.startsWith line "#")))
        (let [h (apply hash-map (flatten header))]
          (assoc h :contents (md-to-html-string (reduce #(str %1 %2 "\n") lines)
                                                :code-style #(format "class=\"code\" data-lang=\"%s\"" %))))
        (recur (next lines)
               (conj header (parse-header-line line)))))))

;; <strong>parse-tags</strong> -- parses the tags line.
;;
;; <i>Parameters:</i>
;; <ul>
;; <li>tags-string -- a string containing tags;</li>
;; </ul>
;; <i>Output:</i> a sequence of tags.
(defn parse-tags [tags-string]
  (let [arr (.split tags-string " ")]
    (filter #(not (empty? %)) arr)))

;; <strong>parse-data-file</strong> -- parses a markdown file with the header containing metainformation.
;;
;; <i>Parameters:</i>
;; <ul>
;; <li>file -- java File object;</li>
;; </ul>
;; <i>Output:</i> a map containing a set of fields from the parsed file.
;; These fields are: file-name, html-file-name, tags, contents and all the
;; other fields specified in the header of the file.
(defn parse-data-file [file]
  (binding [*current-file-name* (.getName file)]
    (println *current-file-name*)
    (let [lines (-> file
                    clojure.java.io/reader
                    line-seq)
          d (parse-file-content lines)
          data (if (nil? (:tags d))
                 d
                 (assoc d :tags (parse-tags (:tags d))))]
      (-> data
          (assoc :file-name *current-file-name*)
          (assoc :html-file-name (make-html-file-name *current-file-name*))))))

;; <strong>generate-post</strong> -- generates just one html file for the specified post.
;;
;; <i>Parameters:</i>
;; <ul>
;; <li>post -- parsed post;</li>
;; <li>cfg -- the configuration object;</li>
;; <li>posts-list -- the list of parsed posts. Each post is a map of values (see fun. parse-data-file).</li>
;; </ul>
;; <i>Output:</i> None.
(defn generate-post [post cfg posts-list]
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

;; <strong>generate-list</strong> -- generates a paginated list of posts. List of post is limited by
;; the specified posts number per page. If the actual number of posts exceed the limit then the function
;; creates multiple html pages.
;;
;; <i>Parameters:</i>
;; <ul>
;; <li>file-list-name -- the name of the resulting html file;</li>
;; <li>layout-name -- name of the layout file for the list;</li>
;; <li>cfg -- the configuration object;</li>
;; <li>posts-list -- the list of parsed posts. Each post is a map of values (see fun. parse-data-file).</li>
;; </ul>
;; <i>Output:</i> None.
(defn generate-list [file-list-name layout-name cfg posts-list]
  (let [page-size (:page-size cfg)                          ;; posts per page
        pages-count* (int (/ (count posts-list) page-size))
        pages-count (if (> (count posts-list)               ;; number of resulting html files
                           (* page-size pages-count*))
                      (inc pages-count*)
                      pages-count*)
        dir (:output-dir cfg)                               ;; output directory
        base-dir (:base-dir cfg)                            ;; base directory where the project is stored
        layouts-dir (:layouts-dir cfg)                      ;; name of the directory where the layouts are stored
        layout (read-string (slurp (str base-dir layouts-dir;; layout file
                                        layout-name ".clj")))
        eval-data (concat '(do (use 'clj-site.util)) (list layout))]
    (doseq [page-num (range pages-count)]
      (binding [*config* cfg
                *page-prev* (if (> (dec pages-count) page-num) ;; if there exists previous page -- then generate its name
                              (make-html-file-name file-list-name (inc page-num)))
                *page-next* (if-not (zero? page-num)           ;; if there exists next page -- then generate its name
                              (make-html-file-name file-list-name (dec page-num)))
                *page-num* page-num                            ;; current page number
                *page-posts* (nth (doall (partition-all page-size posts-list)) page-num)] ;; posts on the current page
        (spit (str base-dir dir (make-html-file-name file-list-name page-num))
              (html (eval eval-data)))))))


;; <strong>get-all-tags</strong> gets all tags from the posts list. Doesn't contain duplicate tags.
;;
;; <i>Parameters:</i>
;; <ul>
;; <li>posts-list -- the list of parsed posts. Each post is a map of values (see fun. parse-data-file) .</li>
;; </ul>
;; <i>Output:</i> Vector of strings -- one for each tag.
(defn get-all-tags [posts-list]
  (vec
   (apply hash-set
          (flatten (reduce conj []
                           (map :tags posts-list))))))

;; <strong>generate-tags-list</strong> generates a list of posts for each tag.
;; Result is a set of html-files each containing list of posts for a specific tag.
;;
;; <i>Parameters:</i>
;; <ul>
;; <li>cfg -- the configuration object;</li>
;; <li>posts-list -- the list of parsed posts. Each post is a map of values (see fun. parse-data-file).</li>
;; </ul>
;; <i>Output:</i> None.
(defn generate-tags-list [cfg posts-list]
  (binding [*posts-list* posts-list]
    (let [tags (get-all-tags posts-list)]
      (dorun (map (fn [tag]
                    (generate-list tag
                                   (or (get cfg (keyword tag))  ;; first try to get the layout name for this tag from the configuration
                                       (:tags-layout cfg))      ;; if no layout specified for this tag -- use the default layout for all tags
                                   cfg
                                   (filter #(not (nil? (index-of tag (:tags %)))) ;; finds only those posts which contain current tag
                                           posts-list)))
                  tags)))))


;; <strong>make-config</strong> creates global configuration object. It merges the options from the configuration file
;; with the default settings. If any configuration option is missing then the function
;; sets it to the predefined value.
;;
;; <i>Parameters:</i>
;; <ul>
;; <li>cfg -- the configuration file;</li>
;; <li>dir-name -- the name of the input directory</li>
;; </ul>
;; <i>Output:</i> global configuration object.
(defn make-config [cfg dir-name]
  "Makes default config, applies user settings from file config.clj"
  (-> cfg
      (assoc :url-base (or (:url-base cfg) "/"))                            ;; base url (site url)
      (assoc :page-size (or (:page-size cfg) 5))                            ;; posts per page
      (assoc :posts-dir (add-separator (or (:posts-dir cfg) "posts")))      ;; directory where posts in the markdown format are stored
      (assoc :layouts-dir (add-separator (or (:layouts-dir cfg) "layouts")));; the directory with layouts
      (assoc :output-dir (add-separator (or (:output-dir cfg) "output")))   ;; the directory where to store the generated files
      (assoc :base-dir dir-name)                                            ;; input directory
      (assoc :tags-layout (or (:tags-layout cfg) "tags"))                   ;; the default layout for all tags
      (assoc :post-layout (or (:post-layout cfg) "post"))))                 ;; the default layout for all posts


;; <strong>main</strong> -- entry point for the application.
;; The function reads config file and creates the global configuration object. Then it
;; finds all the layout files and data files in the specified input directory and tries
;; to compile the site.
;; 
;; <i>Parameters:</i>
;; <ul>
;; <li>input-dir-name -- the name of the directory where the templates and data are stored.</li>
;; </ul>
;; <i>Output:</i> None.
(defn -main [ & [input-dir-name]]
  (if (nil? input-dir-name)
    (print-usage)
    (let [dir-name (add-separator input-dir-name)      ;; input directory name ending with file separator
          
          cfg (-> (str dir-name "config.clj")          ;; configuration object
                  slurp
                  read-string
                  (make-config dir-name))

          posts-files (find-data-files cfg dir-name :posts-dir) ;; list of files with posts in markdown format
          posts-list (parse-data-files-list posts-files)]       ;; list of parsed posts

      (dorun (map #(generate-post % cfg posts-list) posts-list)) ;; generate all posts
      (generate-tags-list cfg posts-list))))                     ;; generate lists for each tag
