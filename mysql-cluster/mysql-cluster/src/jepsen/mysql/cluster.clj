(ns jepsen.mysql.cluster
    "Constructs tests, handles CLI arguments, etc."
    (:require [clojure.tools.logging :refer [info warn]]
              [clojure [pprint :refer [pprint]]
               [string :as str]]
              [jepsen [cli :as cli]
               [checker :as checker]
               [db :as jdb]
               [generator :as gen]
               [os :as os]
               [tests :as tests]
               [util :as util :refer [parse-long]]]
              [jepsen.os.debian :as debian]
              [jepsen.mysql
               [append :as append]
               [mysql-cluster :as db]]))

(def short-isolation
    {:strict-serializable "Strict-1SR"
     :serializable        "S"
     :strong-snapshot-isolation "Strong-SI"
     :snapshot-isolation  "SI"
     :repeatable-read     "RR"
     :monotonic-atomic-view "MAV"
     :read-committed      "RC"
     :read-uncommitted    "RU"})

(defn mysql-cluster-test
    "Given an options map from the command line runner (e.g. :nodes, :ssh,
    :concurrency, ...), constructs a test map."
    [opts]
    (merge tests/noop-test
           opts
           {:name (str "mysql cluster" :append
                       " " (short-isolation (:isolation opts)) " ("
                       (short-isolation (:expected-consistency-model opts)) ")")
            :pure-generators true
            :os   debian/os
            :db   (db/db (:version opts))
            :checker (checker/compose
                         {:perf       (checker/perf)
                          :clock      (checker/clock-plot)
                          :stats      (checker/stats)
                          :exceptions (checker/unhandled-exceptions)
                          :workload   (:checker (append/workload opts))})
            :client    (:client (append/workload opts))
            :generator (gen/phases
                           (->> (:generator (append/workload opts))
                                (gen/stagger (/ (:rate opts)))
                                (gen/nemesis nil)
                                (gen/time-limit (:time-limit opts))))}))
(def cli-opts
    "Additional CLI options"
     [["-i" "--isolation LEVEL" "What level of isolation we should set: serializable, repeatable-read, etc."
      :default :serializable
      :parse-fn keyword
      :validate [#{:read-uncommitted
                   :read-committed
                   :repeatable-read
                   :serializable}
                 "Should be one of read-uncommitted, read-committed, repeatable-read, or serializable"]]

     [nil "--expected-consistency-model MODEL" "What level of isolation do we *expect* to observe? Defaults to the same as --isolation."
      :default nil
      :parse-fn keyword]

     [nil "--key-count NUM" "Number of keys in active rotation."
      :default  10
      :parse-fn parse-long
      :validate [pos? "Must be a positive integer"]]

     [nil "--max-txn-length NUM" "Maximum number of operations in a transaction."
      :default  4
      :parse-fn parse-long
      :validate [pos? "Must be a positive integer"]]

     [nil "--max-writes-per-key NUM" "Maximum number of writes to any given key."
      :default  256
      :parse-fn parse-long
      :validate [pos? "Must be a positive integer."]]

     ["-r" "--rate HZ" "Approximate request rate, in hz"
      :default 100
      :parse-fn read-string
      :validate [pos? "Must be a positive number."]]

     ["-v" "--version STRING" "What version of MySQL Cluster should we test?"
      :default "8.0.0"]
     ])

(defn opt-fn
    "Transforms CLI options before execution."
    [parsed]
    (update-in parsed [:options :expected-consistency-model]
               #(or % (get-in parsed [:options :isolation]))))

(defn -main
    "Handles command line arguments. Can either run a test, or a web server for
    browsing results."
    [& args]
    (cli/run! (merge (cli/single-test-cmd {:test-fn  mysql-cluster-test
                                           :opt-spec cli-opts
                                           :opt-fn   opt-fn})
                     (cli/serve-cmd))
              args))
