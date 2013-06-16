(defproject clj-site "1.0"
  :description "Static sites generator"
  :url "http://github.com/dbushenko/clj-site"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [hiccup "1.0.3"]
                 [markdown-clj "0.9.25"]]
  :main clj-site.core
  :aot :all)
