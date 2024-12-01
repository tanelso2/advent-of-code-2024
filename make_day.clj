(require '[babashka.pods :as pods])
(pods/load-pod 'retrogradeorbit/bootleg "0.1.9")

(require '[babashka.deps :as deps])
(deps/add-deps '{:deps {org.clojars.tanelso2/clj-toolbox {:mvn/version "0.2.2"}}})

(require
   '[clj-toolbox.files :as files]
   '[clj-toolbox.prelude :refer :all]
   '[clojure.java.shell :refer [sh]]
   '[clojure.repl :refer [doc]]
   '[org.httpkit.client :as http]
   '[pod.retrogradeorbit.bootleg.utils :as bootleg]
   '[pod.retrogradeorbit.hickory.select :as s]
   '[pod.retrogradeorbit.hickory.render :as r]
   '[selmer.parser :refer [render]])

(def YEAR 2024)

(defn render-file [filename args]
  (render (slurp filename) args))

(def cookie-file ".cookie-cache")

(defn get-cookie
  []
  (if (files/file-exists? cookie-file)
      (slurp cookie-file)
      nil))

(defn prompt-cookie
  []
  (println "Please paste another cookie: ")
  (let [ret (str/trim (read-line))]
    (spit cookie-file ret)
    ret))

(defn get-or-prompt-cookie
  []
  (let [ret (get-cookie)]
    (if (nil? ret)
      (prompt-cookie)
      ret)))


(def test-description-link "https://adventofcode.com/2021/day/1")
(def test-input-link "https://adventofcode.com/2021/day/1/input")

(defn make-aoc-request [url]
  (let [cookie (get-or-prompt-cookie)
        resp @(http/get url {:headers {"Cookie" (str "session=" cookie)}})]
    (println (str "Url is " url))
    (if (not= 200 (:status resp))
      (throw (Exception. (str "Non-200 error raised: " (:status resp))))
      (:body resp))))

(defn get-input-for-day [n]
  (let [url (if (= n 0)
              test-input-link
              (str "https://adventofcode.com/" YEAR "/day/" n "/input"))]
    (make-aoc-request url)))

(defn get-description-page-for-day [n]
  (let [url (if (= n 0)
              test-description-link
              (str "https://adventofcode.com/" YEAR "/day/" n))]
    (make-aoc-request url)))

(defn html->markdown [html]
  (->
    (sh "pandoc" "--from=html" "--to=markdown_github" :in html)
    (:out)))

(defn html-article->content [a]
  (->> a
    (r/hickory-to-html)
    (html->markdown)))

(defn get-description-for-day [n]
  (let [page (get-description-page-for-day n)
        html (bootleg/convert-to page :hickory)]
    (->> html
         (s/select (s/and (s/tag :article)
                          (s/class "day-desc")))
         (map html-article->content)
         (str/join "\n"))))

(defn write-input [n]
  (println "Writing input?")
  (let [filename (str "inputs/day" n ".txt")]
    (println (str "Making: " filename))
    (spit filename (get-input-for-day n))
    filename))

(defn make-template-file [template output-loc-fn n]
  (let [template-file (str "templates/" template)
        rendered (render-file template-file {:day n})
        filename (output-loc-fn n)]
    (println (str "Making " filename " from " template-file))
    (if (files/file-exists? filename)
      (throw (Exception. (str "file " filename " already exists")))
      (do
        (spit filename rendered)
        filename))))

(defn make-description-file [n]
  (let [content (get-description-for-day n)
        filename (str "descriptions/day" n ".md")]
    (println (str "Making: " filename))
    (if (files/file-exists? filename)
      (let [current-content (slurp filename)]
        (if (> (count content)
               (count current-content))
          (do
            (println (str filename " already exists, but fetched larger content than current file. Replacing"))
            (spit filename content)
            filename)
          (do
            (println (str filename " already exists, doing nothing"))
            filename)))
      (do
        (spit filename content)
        filename))))

(defn git-add [filename]
  (println (str "Git adding: " filename))
  (sh "git" "add" filename))

(defn make-day [n]
  (doall
    (for [f [write-input
             ;; (partial make-template-file "test_dayx.ml" (fn [n] (str "test/test_day" n ".ml")))
             ;; (partial make-template-file "dayx.ml" (fn [n] (str "lib/day" n ".ml")))
             make-description-file]]
      (let [filename (f n)]
        (git-add filename)))))

(require '[babashka.cli :as cli])

(def args-spec {:coerce {:update-desc :boolean
                         :day :int}
                :args->opts [:day]
                :require [:day]
                :exec-args {:update-desc false}})

(defn parse-args [args]
  (cli/parse-opts args args-spec))

(defn -main [& args]
  (let [{:keys [day update-desc]} (parse-args args)]
    (if update-desc
      (do
        (println (str "Only updating description for day " day))
        (-> (make-description-file day)
            (git-add)))
      (do
        (println (str "Making day " day))
        (make-day day)))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
