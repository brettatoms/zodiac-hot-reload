(ns zodiac.ext.hot-reload-test
  (:require [clojure.test :refer [deftest is testing]]
            [integrant.core :as ig]
            [zodiac.ext.hot-reload :as z.hot-reload]))

;; ============================================================================
;; Init Config Transformation Tests
;; ============================================================================

(deftest init-defaults-test
  (testing "init with no args adds hot-reload components"
    (let [config-fn (z.hot-reload/init)
          config (config-fn {})]
      (is (contains? config ::z.hot-reload/hot-reload))
      (is (contains? config ::z.hot-reload/middleware))
      (is (= (ig/ref ::z.hot-reload/hot-reload)
             (get-in config [::z.hot-reload/middleware :hot-reload]))))))

(deftest init-custom-opts-test
  (testing "init passes options to hot-reload component"
    (let [config-fn (z.hot-reload/init {:watch-paths ["src" "resources"]
                                        :debounce-ms 200})
          config (config-fn {})]
      (is (= ["src" "resources"]
             (get-in config [::z.hot-reload/hot-reload :watch-paths])))
      (is (= 200
             (get-in config [::z.hot-reload/hot-reload :debounce-ms]))))))

(deftest init-adds-user-middleware-test
  (testing "init adds middleware ref to ::z/app :user-middleware"
    (let [config-fn (z.hot-reload/init)
          config (config-fn {})]
      (is (some #(= % (ig/ref ::z.hot-reload/middleware))
                (get-in config [:zodiac.core/app :user-middleware]))))))

(deftest init-preserves-existing-middleware-test
  (testing "init preserves existing user-middleware"
    (let [existing-mw (fn [handler] handler)
          config-fn (z.hot-reload/init)
          config (config-fn {:zodiac.core/app {:user-middleware [existing-mw]}})]
      ;; Both the existing middleware and the hot-reload middleware should be present
      (is (= 2 (count (get-in config [:zodiac.core/app :user-middleware]))))
      (is (some #(= % existing-mw)
                (get-in config [:zodiac.core/app :user-middleware]))))))

(deftest init-preserves-existing-config-test
  (testing "init does not clobber other config keys"
    (let [config-fn (z.hot-reload/init)
          config (config-fn {:some/other-key "value"})]
      (is (= "value" (get config :some/other-key))))))

(deftest init-custom-watch-extensions-test
  (testing "custom watch-extensions are passed through"
    (let [config-fn (z.hot-reload/init {:watch-extensions #{".clj" ".cljc"}})
          config (config-fn {})]
      (is (= #{".clj" ".cljc"}
             (get-in config [::z.hot-reload/hot-reload :watch-extensions]))))))

(deftest init-custom-uri-prefix-test
  (testing "custom uri-prefix is passed through"
    (let [config-fn (z.hot-reload/init {:uri-prefix "/__my-reload"})
          config (config-fn {})]
      (is (= "/__my-reload"
             (get-in config [::z.hot-reload/hot-reload :uri-prefix]))))))
