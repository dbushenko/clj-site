#layout: main

[:div
 [:br]
 [:div
  {:class "feed-header"}
  "\n\t      ЛЕНТА\n\t      "
  [:div {:class "line"}]]
 (let [p (first (page-posts))]
   [:div
    [:h1 (:title p)]
    [:br]
    (:contents p)
    [:br]])
 
 [:br]
 
 (doall (map #(vector :div (link-for (:title %) (:html-file-name %)) [:br]) (next (page-posts))))
 [:br]

 (if-not (nil? (page-next)) (link-for "&lt;&lt; Next &nbsp;&nbsp; " (page-next)))
 (if-not (nil? (page-prev)) (link-for "Prev &gt;&gt;" (page-prev)))

 [:br][:br][:br]
 
 [:div
  {:class "feed"}
  [:div
   {:class "main-post"}
   [:div
    {:class "info-header"}
    "ЧТО ТАКОЕ CLOJURE?\n\t\t  "
    [:div {:class "line"}]]
   [:div
    {:class "main-post-body"}
    [:div {:class "post"}
     [:p {}
      "Clojure - сильно осовремененный Lisp, язык программирования с динамической типизацией, работающий на нескольких платформах (JVM, JavaScript, .NET) и ориентированный на быстрое решение практических задач."]]
    [:div
     {:class "feed-more"}
     [:a
      {:shape "rect",
       :class "red-link",
       :href (url-for "manifest.html")}
      "Манифест лиспера"] ]]]]
 
 ]
