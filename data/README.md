# Process for creating data:

Downloaded session data from http://clojurewest.org/sessions/ on 2013-03-05
Replace all `<hr />` with `</div><div>` and add divs at beginning and end of content area. Saved as sessions-div.html
Run cascalog-intro.scrape to pull text from html
Manually annotate talk type and location. Saved as talks.txt
