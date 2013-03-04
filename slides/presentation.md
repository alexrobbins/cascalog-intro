# Cascalog
## Logic Programming Over Hadoop

Alex Robbins ![factual-logo]

---

# What can Cascalog do for me?

---

# What problem does it solve?

---

# Who solves this problem?

---

# Hadoop

Perfect for anyone who thinks in Map/Reduce, and likes typing.

---

```Java
package org.myorg;

import java.io.IOException;
import java.util.*;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

public class WordCount {

  public static class Map extends Mapper<LongWritable, Text, Text, IntWritable> {
    private final static IntWritable one = new IntWritable(1);
    private Text word = new Text();

    public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
      String line = value.toString();
      StringTokenizer tokenizer = new StringTokenizer(line);
      while (tokenizer.hasMoreTokens()) {
        word.set(tokenizer.nextToken());
        context.write(word, one);
      }
    }
  }

  public static class Reduce extends Reducer<Text, IntWritable, Text, IntWritable> {

    public void reduce(Text key, Iterable<IntWritable> values, Context context)
      throws IOException, InterruptedException {
      int sum = 0;
      for (IntWritable val : values) {
        sum += val.get();
      }
      context.write(key, new IntWritable(sum));
    }
  }

  public static void main(String[] args) throws Exception {
    Configuration conf = new Configuration();
    Job job = new Job(conf, "wordcount");
    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(IntWritable.class);
    job.setMapperClass(Map.class);
    job.setReducerClass(Reduce.class);
    job.setInputFormatClass(TextInputFormat.class);
    job.setOutputFormatClass(TextOutputFormat.class);
    FileInputFormat.addInputPath(job, new Path(args[0]));
    FileOutputFormat.setOutputPath(job, new Path(args[1]));
    job.waitForCompletion(true);
  }

}

// http://wiki.apache.org/hadoop/WordCount
```

---

# Pig

A great fit for people who love SQL, and wish they could use it for everything.

---

```
input_lines = LOAD '/tmp/my-copy-of-all-pages-on-internet' AS (line:chararray);

-- Extract words from each line and put them into a pig bag
-- datatype, then flatten the bag to get one word on each row
words = FOREACH input_lines GENERATE FLATTEN(TOKENIZE(line)) AS word;

-- filter out any words that are just white spaces
filtered_words = FILTER words BY word MATCHES '\\w+';

-- create a group for each word
word_groups = GROUP filtered_words BY word;

-- count the entries in each group
word_count = FOREACH word_groups GENERATE COUNT(filtered_words) AS count, group AS word;

-- order the records by count
ordered_word_count = ORDER word_count BY count DESC;
STORE ordered_word_count INTO '/tmp/number-of-words-on-internet';

-- http://en.wikipedia.org/wiki/Pig_(programming_tool)
```


---

# Cascalog

The power of logic programming and Clojure, combined with a lot of magic

---

```
(ns cascalog-class.core
  (:require [cascalog.api :refer :all]
            [cascalog.ops :as c]))

(defmapcatop split
  "Accepts a sentence 1-tuple, splits that sentence on whitespace, and
  emits a single 1-tuple for each word."
  [^String sentence]
  (.split sentence "\\s+"))

(def -main
  "Accepts a generator of lines of text and returns a subquery that
  generates a count for each word in the text sample."
  (?<- (stdout)
    [?word ?count]
    ((hfs-textline "input-dir") ?textline)
    (split ?textline :> ?word)
    (c/count ?count)))

;; https://github.com/sritchie/cascalog-class/blob/master/src/cascalog_class/core.clj
```

---

# What is Cascalog?

**Casca**ding + Data**log** = **Cascalog**

---

# Cascading?
## Cascading Wraps Hadoop
## Cascalog Wraps Cascading

---

# Datalog?

---

# Logic Programming?
(in core.logic)

---

# Facts/Relations

```
(defrel
      person first-name last-name  role)
(fact person "Dr."      "Horrible" :villain)
(fact person "Bad"      "Horse"    :villain)
(fact person "Captain"  "Hammer"   :hero)
(fact person "Penny"    ""         :bystander)
```

---

# Logic Variables
## For Physicists

- Logic Variables abide by the Quantum Superposition principle
- run* causes (repeated) wave function collapse
- Don't use classical reasoning in quantum contexts!

---

# Logic Variables
## For Non-Physicists

```
(defrel person name)
(fact person "Dr. Horrible")
(fact person "Penny")
(fact person "Captain Hammer")

(run* [variable]
  (person variable))

;; output:
("Dr. Horrible" "Penny" "Captain Hammer")
```

---

# Predicates

```
(defrel likes liker likee)
(fact likes "Dr. Horrible" "Penny")
(fact likes "Penny" "Captain Hammer")
(fact likes "Captain Hammer" "Captain Hammer")

;; Who likes Penny?
(run* [q]
  (likes q "Penny"))

;; output:
("Dr. Horrible")

;; Any pairs that like each other?
(run* [q]
  (fresh [x y]
    (== q [x y])
    (likes x y)
    (likes y x)))

;; output:
()
```

---

# Cascalog

- (not= core.logic Datalog)
- Tuple all the things

---

# Cascalog

- Generators - Tuple source
- Operations - Augment or filter tuples
- Aggregators - Act on sequences of tuples

---

# Generators
Vector source:

```
(def people [
 [ "Dr."      "Horrible" :villain   ]
 [ "Bad"      "Horse"    :villain   ]
 [ "Captain"  "Hammer"   :hero      ]
 [ "Penny"    ""         :bystander ]])
```

TSV source:

```
Dr.     Horrible villain
Bad     Horse    villain
Captain Hammer   hero
Penny            bystander
```

```
(defn split
  [^String sentence]
  (.split sentence "\\t"))

(def people
  (<- [?fname ?lname ?role]
    (lfs-textline "people.tsv" ?line)
    (split ?line :> ?fname ?lname ?role)))
```

---

# Operations

```
(def people [
 ["Dr."      "Horrible" :villain  ]
 ["Bad"      "Horse"    :villain  ]
 ["Captain"  "Hammer"   :hero     ]
 ["Penny"    ""         :bystander]])

;; Filter:
(??<- [?fname]
  (people ?fname ?lname _)
  (clojure.string/blank? ?lname))

;output
(["Penny"])

;; Augment
(defn expand-abbreviations [name]
  (if (= name "Dr.") "Doctor" name))

(??<- [?fname ?lname]
  (people ?orig-fname ?lname :villain) ; Filter, only villains
  (expand-abbreviations ?orig-fname :> ?fname))

;ouput
(["Doctor" "Horrible"] ["Bad Horse"])
```

---

# Joins

```
(def people [
 ["Dr."      "Horrible" :villain  ]
 ["Bad"      "Horse"    :villain  ]
 ["Captain"  "Hammer"   :hero     ]
 ["Penny"    ""         :bystander]])

(def likes [
 ["Dr. Horrible"   "Penny"         ]
 ["Penny"          "Captain Hammer"]
 ["Captain Hammer" "Captain Hammer"]])

(def favorite-foods [
 ["Penny" "Frozen yogurt"]])

;; What people are liked by villains, and what food do they like?
(??<- [?liked-person ?food]
  (people ?liker :villain) ; filtering!
  (likes ?liker ?liked-person)
  (favorite-foods ?liked-person ?food))

;output
(["Penny" "Frozen yogurt"])
```

Painless join across three sources!

---

# Aggregators

```
(def people [
 ["Dr."      "Horrible" :villain  ]
 ["Bad"      "Horse"    :villain  ]
 ["Captain"  "Hammer"   :hero     ]
 ["Penny"    ""         :bystander]])

; How many of each role are there?
(??<- [?role ?count]
  (people _ _ ?role)
  (cascalog.ops/count ?count))

;output
([:villain 2] [:hero 1] [:bystander 1])
```

---

# Demo

---

# Thanks

Nathan Marz (and others) for Cascalog

Rich Hickie for Clojure

Alex Miller for Clojure/West

---

# Cascalog
## Logic Programming Over Hadoop

Alex Robbins

- alexander.j.robbins@gmail.com
- alexr@factual.com ![factual-logo]
- https://github.com/alexrobbins/cascalog-intro

Factual is hiring!

[factual-logo]: images/factual-high-res.png "Factual"
