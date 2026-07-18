(ns svgraph.core
  "Portable SVGraph data projection.

  This namespace is the CLJC authority for the stable handoff model. Browser,
  CLI, and package adapters can elaborate visual conversion, but the semantic
  graph contract is plain data."
  (:require [clojure.string :as str]))

(def default-slide-size [1280 720])

(defn- attr-value
  [attrs key]
  (or (second (re-find (re-pattern (str "(?:^|\\s)" key "\\s*=\\s*\"([^\"]*)\"")) attrs))
      (second (re-find (re-pattern (str "(?:^|\\s)" key "\\s*=\\s*'([^']*)'")) attrs))))

(defn- parse-attrs
  [attrs]
  (into {}
        (keep (fn [[_ k v1 v2]]
                [(keyword k) (or v1 v2)]))
        (re-seq #"([A-Za-z_:][-A-Za-z0-9_:.]*)\s*=\s*(?:\"([^\"]*)\"|'([^']*)')" attrs)))

(defn- data-attrs
  [attrs]
  (into {}
        (keep (fn [[k v]]
                (when (str/starts-with? (name k) "data-")
                  [(keyword (subs (name k) 5)) v])))
        attrs))

(defn- viewbox-size
  [svg-text]
  (when-let [[_ attrs] (re-find #"(?is)<svg\b([^>]*)>" svg-text)]
    (when-let [view-box (attr-value attrs "viewBox")]
      (let [parts (mapv parse-double (str/split (str/trim view-box) #"\s+"))]
        (when (= 4 (count parts))
          [(nth parts 2) (nth parts 3)])))))

(defn- text-content
  [body]
  (some-> body
          (str/replace #"(?is)<[^>]+>" "")
          str/trim
          not-empty))

(defn nodes
  [svg-text]
  (->> (re-seq #"(?is)<([A-Za-z][A-Za-z0-9:_-]*)\b([^>]*)/?>" svg-text)
       (remove #(= "svg" (str/lower-case (second %))))
       (map-indexed
        (fn [idx match]
          (let [tag (nth match 1)
                attrs (parse-attrs (or (nth match 2) ""))
                data (data-attrs attrs)
                id (or (:id attrs) (str "node-" idx))]
            {:svgraph.node/id id
             :svgraph.node/tag (keyword tag)
             :svgraph.node/kind (keyword (or (:kind data) (:role data) tag))
             :svgraph.node/attrs attrs
             :svgraph.node/data data
             :svgraph.node/text nil})))
       vec))

(defn edges
  [nodes]
  (vec
   (for [node nodes
         :let [data (:svgraph.node/data node)
               from (or (:from data) (:source data))
               to (or (:to data) (:target data))]
         :when (and from to)]
     {:svgraph.edge/id (str (:svgraph.node/id node) ":edge")
      :svgraph.edge/kind (keyword (or (:edge data) (:type data) "relation"))
      :svgraph.edge/from from
      :svgraph.edge/to to
      :svgraph.edge/source (:svgraph.node/id node)})))

(defn presentation
  [svg-text nodes]
  (let [slides (vec (filter #(#{:slide} (:svgraph.node/kind %)) nodes))
        size (or (viewbox-size svg-text) default-slide-size)]
    {:svgraph.presentation/version 1
     :svgraph.presentation/slide-size size
     :svgraph.presentation/slides
     (if (seq slides)
       (mapv (fn [node idx]
               {:svgraph.slide/id (:svgraph.node/id node)
                :svgraph.slide/index idx
                :svgraph.slide/title (or (get-in node [:svgraph.node/data :title])
                                         (:svgraph.node/text node)
                                         (:svgraph.node/id node))})
             slides
             (range))
       [{:svgraph.slide/id "root"
         :svgraph.slide/index 0
         :svgraph.slide/title "root"}])}))

(defn build-svgraph
  [svg-text]
  (let [nodes (nodes svg-text)]
    {:svgraph/version 1
     :svgraph/source-kind :svg
     :svgraph/nodes nodes
     :svgraph/edges (edges nodes)
     :svgraph/presentation (presentation svg-text nodes)}))

(defn office-causal-payload
  [svgraph]
  {:generator "svgraph"
   :version 1
   :nodes (mapv (fn [node]
                  {:id (str "ocz1:svgraph-" (:svgraph.node/id node))
                   :kind (name (:svgraph.node/kind node))
                   :text (:svgraph.node/text node)})
                (:svgraph/nodes svgraph))
   :edges (mapv (fn [edge]
                  {:id (str "ocz1:svgraph-" (:svgraph.edge/id edge))
                   :kind (name (:svgraph.edge/kind edge))
                   :from (str "ocz1:svgraph-" (:svgraph.edge/from edge))
                   :to (str "ocz1:svgraph-" (:svgraph.edge/to edge))
                   :source (str "ocz1:svgraph-" (:svgraph.edge/source edge))})
                (:svgraph/edges svgraph))})
