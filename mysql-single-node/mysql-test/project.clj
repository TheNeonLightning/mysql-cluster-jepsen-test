(defproject jepsen.mysql "0.1.0"
  :description "Jepsen tests for single node MySQL."
  :url "https://github.com/jepsen-io/jepsen"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [jepsen "0.2.0"]
                 [seancorfield/next.jdbc "1.0.445"]
                 [mysql/mysql-connector-java "8.0.29"]
                 [cheshire "5.10.0"]
                 [clj-wallhack "1.0.1"]]
  :main single-node
  :repl-options {:init-ns single-node})
