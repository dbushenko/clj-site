[:div
 {:class "main-post"}

 (doall (map #(vector :div (link-for (:title %)
                                     (:html-file-name %))
                      [:br])
             (page-posts)))
 [:br]

 (if (page-next) (link-for "&lt;&lt; Next &nbsp;&nbsp; " (page-next)))
 (if (page-prev) (link-for "Prev &gt;&gt;" (page-prev)))

 [:br][:br][:br]

 
 ]

