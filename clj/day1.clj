(require '[babashka.deps :as deps])
(deps/add-deps '{:deps {org.clojars.tanelso2/clj-toolbox {:mvn/version "0.8.3"}}})
(require
  '[clj-toolbox.files :as files]
  '[clj-toolbox.prelude :refer :all])

(defn get-input
  []
  (slurp "inputs/day1.txt"))

(defn parse-line
  [l]
  (as-> l v
      (str/split v #"\s+")
      (map parse-int v)))

(defn transform-lists
  [l]
  (->> l
    (apply interleave)
    (partition (count l))))

(defn parse-input
  [input-str]
  (->> input-str
      (str/split-lines)
      (map parse-line)
      (transform-lists)))

(defn part1-sum
  [x y]
  (abs (- x y)))

(defn part1
  [input-str]
  (let [[l1 l2] (parse-input input-str)
        l1' (sort l1)
        l2' (sort l2)
        sums (map part1-sum l1' l2')]
    (sum sums)))

(defn part2-score
  [l2 x]
  (let [occurences (filter #(= x %) l2)
        n (count occurences)]
    (* n x)))

(defn part2
  [input-str]
  (let [[l1 l2] (parse-input input-str)]
    (sum-by #(part2-score l2 %) l1)))

(let [input-str (get-input)]
  (println (str "Part1: " (pr-str (part1 input-str))))
  (println (str "Part2: " (pr-str (part2 input-str)))))
