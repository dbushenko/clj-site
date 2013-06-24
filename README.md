# clj-site

A Clojure application for compiling static sites

## Usage

Examples see in the "sample" dir.

	 $ lein uberjar
	 $ java -jar target/clj-site-1.0-standalone.jar sample/
		 
Then open sample/index.html in your browser.

## Directory structure

The input data directory should contain file "config.clj" which is a usual clojure file which returns the map of parameters. The following parameters are available:

`:url-base` -- base url (default -- "/")  
`:page-size` -- posts per page (default -- 5)  
`:posts-dir` -- directory where posts in the markdown format are stored (default -- "posts")  
`:layouts-dir` -- the directory with layouts (default -- "layouts")  
`:output-dir` --  the directory where to store the generated files (default -- "output")  
`:tags-layout` -- the default layout name for all tags (default -- "tags")  
`:post-layout` -- the default layout name for all posts (default -- "post")
`:rss-title` -- the title of the RSS channel
`:rss-link` -- the linkt to the RSS channel
`:rss-description` -- the description of the RSS channel

Also you may add any tag name and specify layout name for each tag.

The layouts dir contains clojure files in hiccup format. The posts dir contains files in the markdown format.

The system also will generate a rss.xml file in the output directory containing 10 lates articles.

### Posts layout

Use function "post-content" to insert the content of a specific post.
If you need to insert another page (e.g. list of posts for a specific task) use the "inner-page" function.
```clojure
(inner-page "news.html"
            {:width "520px"})
```

### Posts

Posts are written in markdown format and have extra headers. A header is a set of lines starting with '#' symbol without empty lines.

    #title: First record
    #tags: news
    #date: 02-06-2013

Each header line starts with '#' and ends with ':', after the colon symbol goes the value. Everything which is written in headers goes to the object 'post'. Header 'title' is required for each post.

### Tags layout

Use function "page-posts" to insert page posts, "page-next" and "page-prev" to get the names of the next and previous pages.

Use "link-for" to generate a html link which will open the file in the current docuement.
Use "parent-link-for" to open a specific file in the parent documents.

Let's say we are generating a list of posts. The links for the next and previous pages should be generated with "link-for". But the links for each posts should be generated using the "parent-link-for".

## License

Copyright © 2013 FIXME

Distributed under the Eclipse Public License, the same as Clojure.
