(ns cascalog-intro.scrape
  (:require [clojure.java.io :refer (reader)]
            [clojure.string :as s]
            [clojure.tools.cli :refer :all]
            [net.cgrand.enlive-html :as html]))

(defn -main [& args]
  "Grab the talk content from the div'ed session page. Dump as newline delimited talks"
  (let [[arg-map _ usage]
        (cli args
             ["-i" "--input"  "input path"])]
    (when-not (:input arg-map)
      (println usage)
      (System/exit 0))
    (let [resource (-> (:input arg-map)
                    reader
                    html/html-resource)
          talk-nodes (html/select resource [:div#content :div])]
      (doseq [tn talk-nodes]
        (-> tn
            html/text
            (s/replace "\n" " ")
            println)))))
