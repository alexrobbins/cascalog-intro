(ns cascalog-intro.core
  (:require [cascalog.api :refer :all]
            [cascalog.ops :as c]
            [clojure.string :as s]
            [cascalog-intro.stopwords :refer (stopwords)]))
;; This file is used for the live demo, so it is organized temporally, top to bottom.




;; Let's start with word counts for one talk, just to get started

;; First we need a tuple source.

(comment
  (??<- [?line] ((lfs-textline "data/one-talk.txt") ?line))

  )


(defn talk-tap [file-path]
  (<- [?location ?type ?description]
      ((lfs-textline file-path) ?line)
      (s/split ?line #"  " 3 :> ?location ?type ?description)))

(def one-talk-tap (talk-tap "data/one-talk.txt"))
(def all-talks-tap (talk-tap "data/talks.txt"))

(comment
  (??- one-talk-tap)

  )

;; Now we need something to split a description into multiple words
(defmapcatop description->words [description]
  (re-seq #"[A-Za-z0-9']+" description))

;; Splitting into words, then counting them.
(comment
  (??<- [?word ?count]
          (one-talk-tap _ _ ?description)
          (description->words ?description :> ?word)
          (c/count ?count))

  )

;; Oops, lowercase everything
(defn lowercase [string]
  (.toLowerCase string))

(comment
  (??<- [?word ?count]
          (one-talk-tap _ _ ?description)
          (lowercase ?description :> ?lower-description)
          (description->words ?lower-description :> ?word)
          (c/count ?count))

  )

;; Top ten words
(def word-counts-for-a-talk
  (<- [?word ?count]
      (one-talk-tap _ _ ?description)
      (lowercase ?description :> ?lower-description)
      (description->words ?lower-description :> ?word)
      (c/count ?count)))

(comment
  (??<- [?top-word ?top-count]
        (word-counts-for-a-talk ?word ?count)
        (:sort ?count)
        (:reverse true)
        (c/limit [10] ?word ?count :> ?top-word ?top-count))

  )

;; Word counts, separated by talk type

(def word-counts-by-talk-type
  (<- [?type ?word ?count]
      (all-talks-tap _ ?type ?description)
      (lowercase ?description :> ?lower-description)
      (description->words ?lower-description :> ?word)
      (c/count ?count)))

(comment
  (??<- [?type ?top-word ?top-count]
        (word-counts-by-talk-type ?type ?word ?count)
        (:sort ?count)
        (:reverse true)
        (c/limit [10] ?word ?count :> ?top-word ?top-count))

  )

;; Too many junk words, let's filter those out
(def not-stopword?
  (complement stopwords))

(comment
  (??<- [?type ?top-word ?top-count]
        (word-counts-by-talk-type ?type ?word ?count)
        (not-stopword? ?word)
        (:sort ?count)
        (:reverse true)
        (c/limit [10] ?word ?count :> ?top-word ?top-count))

  )

;; What about theater vs studio?

(def word-counts-by-talk-location
  (<- [?location ?word ?count]
      (all-talks-tap ?location _ ?description)
      (lowercase ?description :> ?lower-description)
      (description->words ?lower-description :> ?word)
      (c/count ?count)))

(def studio-or-theater? #{"studio" "theater"})

(comment
  (??<- [?location ?top-word ?top-count]
        (word-counts-by-talk-location ?location ?word ?count)
        (not-stopword? ?word)
        (studio-or-theater? ?location)
        (:sort ?count)
        (:reverse true)
        (c/limit [10] ?word ?count :> ?top-word ?top-count))

  )
