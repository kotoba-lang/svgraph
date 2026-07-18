(ns svgraph.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [svgraph.core :as svgraph]))

(def sample-svg
  "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 420 240\">
     <g id=\"slide-a\" data-kind=\"slide\" data-title=\"Intro\">
       <rect id=\"title-box\" data-role=\"title\" x=\"20\" y=\"20\" width=\"120\" height=\"48\"/>
       <line id=\"rel-title-body\" data-kind=\"relation\" data-from=\"title-box\" data-to=\"body-box\"/>
       <rect id=\"body-box\" data-role=\"body\" x=\"220\" y=\"20\" width=\"140\" height=\"48\"/>
     </g>
   </svg>")

(deftest builds-semantic-graph
  (let [graph (svgraph/build-svgraph sample-svg)]
    (is (= 1 (:svgraph/version graph)))
    (is (= ["slide-a" "title-box" "rel-title-body" "body-box"]
           (map :svgraph.node/id (:svgraph/nodes graph))))
    (is (= [{:svgraph.edge/id "rel-title-body:edge"
             :svgraph.edge/kind :relation
             :svgraph.edge/from "title-box"
             :svgraph.edge/to "body-box"
             :svgraph.edge/source "rel-title-body"}]
           (:svgraph/edges graph)))))

(deftest derives-presentation-view
  (let [presentation (:svgraph/presentation (svgraph/build-svgraph sample-svg))]
    (is (= [420.0 240.0] (:svgraph.presentation/slide-size presentation)))
    (is (= [{:svgraph.slide/id "slide-a"
             :svgraph.slide/index 0
             :svgraph.slide/title "Intro"}]
           (:svgraph.presentation/slides presentation)))))

(deftest projects-office-causal-payload
  (let [payload (-> sample-svg svgraph/build-svgraph svgraph/office-causal-payload)]
    (testing "payload stays plain data"
      (is (= "svgraph" (:generator payload)))
      (is (some #(= "ocz1:svgraph-title-box" (:id %)) (:nodes payload)))
      (is (= "relation" (:kind (first (:edges payload))))))))
