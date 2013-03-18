# Cascalog
## Logic Programming Over Hadoop

Alex Robbins &nbsp;&nbsp;&nbsp;&nbsp; ![factual-logo]

---

# What can Cascalog do for me?

<!--- Notes:
This is meant to be a practical talk about Cascalog. Although it is certainly
some cool tech, we each already have a list of things we'd like to check out
that is longer than we'll ever get to. It is my hope that this talk will help
you decide whether or not Cascalog is worth the time it'll take you to
understand it.
--->

---

# What problem does it solve?

<!--- Notes:
Cascalog provides a way to describe and execute distributed computing jobs.
If you have a task that can be parallelized and is taking too long, Cascalog
can help. Cascalog allows you to easily break a task down into chunks, and
pass those chunks around a computing cluster of dozens or hundreds of servers.
--->

---

# Who else solves this problem?

<!--- Notes:
Cascalog is not the only distributed computing framework. Actually, it is built
on top of one of its biggest competitors, Hadoop. Other packages currently
solving this problem include Pig, Hive and Riak, just to name a few. The different
packages embrace different abstractions, as well as different levels of
abstraction.
--->

---

# Hadoop

Perfect for anyone who thinks in Map/Reduce, and likes typing.

<!--- Notes:
Hadoop is the current dominant force in the world of distributed computing.
Most of its competitors are built on top of it, and reuse pieces of it, such as
HDFS, Hadoop's distributed file system. Hadoop provides low level control over your
jobs, with the normal tradeoffs for low level programming. Writing compute jobs
in hadoop requires a fair amount of boilerplate, with even very simple tasks
taking more than 50 lines of code. At the same time, Hadoop works.
--->

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

Because everyone wants to learn another programming language.


<!--- Notes:
Disclaimer: I've never used Pig myself.
Pig allows you to write commands in pig-latin that are run over the cluster. If you
want to add functionality to a Pig query, you write a "User Defined Function."
As your needs get more and more specialized, you end up writing many UDFs, all
in Java.
--->

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

# Hive

A great fit for people who love SQL, and wish they could use it for everything.

---

```
CREATE TABLE docs(contents STRING);
FROM

(MAP docs.contents USING 'tokenizer_script' AS word, cnt
FROM docs
CLUSTER BY word) map_output

REDUCE map_output.word, map_output.cnt USING 'count_script' AS word, cnt;

;; Note that you have to provide your own tokenizer and counter, in whatever language you want.
```

---

# Cascalog

The power of logic programming and Clojure, combined with a lot of magic.

<!--- Notes:
Obviously, we'll be talking about Cascalog for the rest of the talk, so I don't
want to give it all away now. The high level strengths and weaknesses are:
Strengths: concise, easily extensible, Clojure!
Weaknesses: lots of magic, forces a restricted subset of clojure (no lambdas),
  cryptic error messages
--->

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


```Left-java-code
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

    public void map(LongWritable key, Text value, Context context)
      throws IOException, InterruptedException {
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

```Right-clojure-code
(ns cascalog-class.core
  (:require [cascalog.api :refer :all]
            [cascalog.ops :as c]))

(defmapcatop split
  [^String sentence]
  (.split sentence "\\s+"))

(def -main
  (?<- (stdout)
    [?word ?count]
    ((hfs-textline "input-dir") ?textline)
    (split ?textline :> ?word)
    (c/count ?count)))

;; https://github.com/sritchie/cascalog-class/blob/master/src/cascalog_class/core.clj
```

<div style="clear: both">&nbsp;</div>
Cascalog: 311 characters

Hadoop:  1950 characters (6x more)

---

# What is Cascalog?

**Casca**ding + Data**log** = **Cascalog**

<!--- Notes:
Unfortunately, over the next several slides I'll be defining terms by using other
terms that you likely don't know. However, I promise to cover those terms in
even later slides.

In this case, the question is, "What is Cascalog?" The answer is, "Cascalog is a
combination of Cascading and Datalog." An answer that is technically correct
(the best kind of correct), but useless to most of you.
--->

---

# Cascading?
## Cascading Wraps Hadoop
## Cascalog Wraps Cascading

<!--- Notes:
What is Cascading? Cascading is a Java library that makes it easier to program
jobs for Hadoop. It takes the Hadoop Map/Reduce idea and layers a data flow
abstraction on top of it. You create data source called taps, allow the data
to flow through many different transforms or combinations, and then at the end
the data flows into a sink. This abstraction can make many Map/Reduce jobs easier
to program, and has many followers.
--->

---

# Datalog?

<!--- Notes:
Datalog is a logic programming language designed to query data. It is similar to
Prolog, but with a couple of tweaks to make it better behaved. For instance, all
Datalog queries are guaranteed to terminate when run on finite datasets. Datalog,
like most logic programming languages, lets you program declaratively. Instead
of telling Cascalog how to do things, you tell it what you want at the end, and
Cascalog figures out how to do it.
--->

---

# Logic Programming?
(in core.logic)

<!--- Notes:
Cascalog may be the first time some of you have actually used logic programming.
Clojure has a library called core.logic which enables logic programming in
Clojure. Core.logic and cascalog are not the same, and neither uses the other,
but they share many of the same concepts. I'll explain the concepts first using
core.logic, and then in Cascalog so you can see the difference.

Declarative programming can be a difficult transition for many programmers. Years
of telling the computer exactly what to do have trained you to care about the
details of how. Declarative programming asks you to relax a little, and trust
the computer. When it works, it can be a great time saver. When something goes
wrong, it can be difficult to tell where the problem is.

For each line in the logic program, even if the other variables aren't
mentioned, they still exist. Filtering or adding an element will affect the entire
row. Picturing the rest of the members as ghosts over to the side might be helpful.
--->

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
- run* causes (repeated) wave function collapses
- Don't use classical reasoning in quantum contexts!

---

# Logic Variables

```
(defrel person name)
(fact person "Dr. Horrible")
(fact person "Penny")
(fact person "Captain Hammer")

(run* [variable]
  (person variable))

;; output:
("Captain Hammer" "Penny" "Dr. Horrible")
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
(["Captain Hammer" "Captain Hammer"])
```

---

# Cascalog

- (not= core.logic Datalog)
- Tuple all the things

<!--- Notes:
Cascalog and core.logic are not the same. Although they both embrace the logic
programming paradigm, you won't be able to copy code from one to the other.
--->

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

TSV source (using a tap):

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
    ((lfs-textline "people.tsv") ?line)
    (split ?line :> ?fname ?lname ?role)))
```

---

# Taps

- lfs-textline - Read (or write) local files
- hfs-textline - Read (or write) inputs from HDFS
- hfs-seqfile - Read (or write) hadoop seqfiles

You can write:

- Taps that read/write JSON
- Taps that read/write protobufs

---

# Queries

Define a query.

```
(def query
  (<- [?name ?age]
    (people ?name ?age)
    (< ?age 40)))
```

Execute the query.

```
(?- (lfs-textline "/home/alexr/output-path")
  query)
```

Define and execute the query.

```
(??<- [?name ?age]
  (people ?name ?age)
  (< ?age 40))
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
 ["Dr. Horrible"   :villain  ]
 ["Bad Horse"      :villain  ]
 ["Captain Hammer" :hero     ]
 ["Penny"          :bystander]])

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
 ["Dr. Horrible"   :villain  ]
 ["Bad Horse"      :villain  ]
 ["Captain Hammer" :hero     ]
 ["Penny"          :bystander]])

; How many of each role are there?
(??<- [?role ?count]
  (people _ ?role)
  (cascalog.ops/count ?count))

;output
([:villain 2] [:hero 1] [:bystander 1])
```

---

# Demo

---

Please don't take any of the conclusions from the demo seriously.

---

# Cascalog at Factual

---

# Thanks

Nathan Marz (and others) for Cascalog

Rich Hickey for Clojure

Alex Miller for Clojure/West

---

# Cascalog
## Logic Programming Over Hadoop

Alex Robbins

- alexr@factual.com &nbsp;&nbsp;&nbsp;&nbsp; ![factual-logo]
- alexander.j.robbins@gmail.com
- https://github.com/alexrobbins/cascalog-intro

Factual is hiring!

[factual-logo]: images/factual-high-res.png "Factual"
