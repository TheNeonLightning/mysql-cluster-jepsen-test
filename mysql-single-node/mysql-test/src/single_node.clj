(ns single-node
    "Constructs tests, handles CLI arguments, etc."
    (:require [append :as append]
              [clojure.tools.logging :refer [info]]
              (jepsen
                  [checker :as checker]
                  [cli :as cli]
                  [db :as db]
                  [db]
         [generator :as gen]
         [os :as os]
         [tests :as tests])
              [jepsen.util :as util]))

(defn db
    "Stub for already running cluster."
    [version]
    (reify db/DB
        (setup! [_ test node]
            (info node "installing mysql cluster" version))

        (teardown! [_ test node]
            (info node "tearing down mysql cluster"))))

(def short-isolation
    {:strict-serializable "Strict-1SR"
     :serializable        "S"
     :strong-snapshot-isolation "Strong-SI"
     :snapshot-isolation  "SI"
     :repeatable-read     "RR"
     :monotonic-atomic-view "MAV"
     :read-committed      "RC"
     :read-uncommitted    "RU"})

(defn mysql-test
    "Given an options map from the command line runner (e.g. :nodes, :ssh,
    :concurrency, ...), constructs a test map."
    [opts]
    (merge tests/noop-test
           opts
           {:name (str "mysql " :append
                       " " (short-isolation (:isolation opts)) " ("
                       (short-isolation (:expected-consistency-model opts)) ")")
            :pure-generators true
            :os   os/noop
            :db   (db "stub")
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
    [[nil "--etcd-version STRING" "What version of etcd should we install?"
      :default "3.4.3"]

     ["-i" "--isolation LEVEL" "What level of isolation we should set: serializable, repeatable-read, etc."
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
      :parse-fn util/parse-long
      :validate [pos? "Must be a positive integer"]]

     [nil "--max-txn-length NUM" "Maximum number of operations in a transaction."
      :default  4
      :parse-fn util/parse-long
      :validate [pos? "Must be a positive integer"]]

     [nil "--max-writes-per-key NUM" "Maximum number of writes to any given key."
      :default  10
      :parse-fn util/parse-long
      :validate [pos? "Must be a positive integer."]]

     ["-r" "--rate HZ" "Approximate request rate, in hz"
      :default 100
      :parse-fn read-string
      :validate [pos? "Must be a positive number."]]
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
    (cli/run! (merge (cli/single-test-cmd {:test-fn  mysql-test
                                           :opt-spec cli-opts
                                           :opt-fn   opt-fn})
                     (cli/serve-cmd))
              args))
