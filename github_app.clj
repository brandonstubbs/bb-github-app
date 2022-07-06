#!/usr/bin/env bb
(ns github-app
  (:require [clojure.string :as str]
            [babashka.pods  :as pods]
            [babashka.curl  :as curl]
            [cheshire.core  :as json])
  (:import (java.time Instant)
           (java.time.temporal ChronoUnit)))



;; we need the buddy pod to create the application access-key
(pods/load-pod 'org.babashka/buddy "0.2.0")
(require
  '[pod.babashka.buddy.sign.jwt :as jwt]
  '[pod.babashka.buddy.keys     :as keys])



(def base-uri "https://api.github.com")



(defn ->path
  [& strs]
  (str/join "/" strs))



(defn from-json
  [string]
  (json/parse-string string true))



(defn app-installation-token
  [app-id private-key]
  (jwt/sign
    {:iss app-id
     ;; 1 minute in the past to allow for clock drift
     :iat (-> (Instant/now) (.minus 1 ChronoUnit/MINUTES) .getEpochSecond)
     ;; JWT expiration time - 10 minutes it maximum
     :exp (-> (Instant/now) (.plus 10 ChronoUnit/MINUTES) .getEpochSecond)}
    (keys/private-key private-key)
    {:alg :rs256}))



(defn installation-id
  [installation-token gh-owner]
  (let [{:keys [status body]}
        (curl/get "https://api.github.com/app/installations"
          {:headers {"Authorization" (str "Bearer " installation-token)
                     "Accept"        "application/vnd.github.v3+json"}})]
    (when (<= 200 status 299)
      (->> body
        from-json
        (filter #(= gh-owner (-> % :account :login)))
        first
        :id))))



(defn installation-access-token
  [installation-token installation-id]
  (let [{:keys [status body]}
        (curl/post (->path base-uri "app/installations" installation-id "access_tokens")
          {:headers {"Authorization" (str "Bearer " installation-token)
                     "Accept"        "application/vnd.github.v3+json"}})]
    (when (<= 200 status 299)
      (-> body from-json :token))))



(defn create-check-run
  [gh-owner repo access-token payload]
  (let [{:keys [status body]}
        (curl/post (->path base-uri "repos" gh-owner repo "check-runs")
          {:headers {"Authorization" (str "token " access-token)
                     "Accept"        "application/vnd.github.v3+json"}
           :body (json/generate-string payload)})]
    (when (<= 200 status 299)
      (-> body from-json :id))))



(defn update-check-run
  [gh-owner repo access-token check-run-id payload]
  (curl/patch (->path base-uri "repos" gh-owner repo "check-runs" check-run-id)
    {:headers {"Authorization" (str "token " access-token)
               "Accept"        "application/vnd.github.v3+json"}
     :body (json/generate-string payload)}))



(defn- check-runs-helper
  [gh-owner repo check-run-id]
  (->path "https://github.com" gh-owner repo "runs" check-run-id))



(comment
  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;;                                                                          ;;
  ;;                 ----==| C H A N G E   T H E S E |==----                  ;;
  ;;                                                                          ;;
  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; Github App ID from applicaiton you created, e.g 123456
  (def app-id "<app-id>")
  ;; Github account owner, organisation or person. e.g brandonstubbs
  (def gh-owner "<github-owner>")
  ;; repo you want to test this on, e.g bb-github-app
  (def repo "<repo>")
  ;; Path to Github App private key, e.g /tmp/bb-checks.pem
  (def private-key "<private-pem-path>")
  ;; Commit you want to create the check runs on, e.g 60c83533123fac893b88a20d24938f488cf63968
  (def commit "<commit>")



  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;;                                                                          ;;
  ;;                        ----==| D E M O |==----                           ;;
  ;;                                                                          ;;
  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  ;; we create a signed access-token for our Github app.
  (def installation-token (app-installation-token app-id private-key))
  ;; we need to find the correct installation-id for the target account.
  (def installation-id (installation-id installation-token gh-owner))
  ;; we need to create an access-token on behalf of the github app to the target
  ;; account.
  (def access-token (installation-access-token installation-token installation-id))


  ;; -- example of creation -> success --

  ;; we create a new chuck run on the target commit, this one we will evolve
  ;; into a success
  (def check-run-id
    (create-check-run gh-owner repo access-token
      {:name     "will succeed"
       :head_sha commit
       :status   "queued"
       :output   {:title   "Queued"
                  :summary "## Job Summary \n some markdown of the summary"}}))

  ;; view your check run here:
  (check-runs-helper gh-owner repo check-run-id)

  ;; we update that check run to show that it is running.
  (update-check-run gh-owner repo access-token check-run-id
    {:status "in_progress"
     :output {:title   "In Progress"
              :summary "## Job Summary \n I am busy running..."}})
  ;; You will see the check run updated.

  ;; we update that check run to a success.
  (update-check-run gh-owner repo access-token check-run-id
    {:status "completed"
     :conclusion "success"
     :output {:title   "Completed"
              :summary "## Job Summary \n I ran successfully..."}})


  ;; -- example of creation -> failure --
  (def check-run-id
    (create-check-run gh-owner repo access-token
      {:name     "will fail"
       :head_sha commit
       :status   "queued"
       :output   {:title   "Queued"
                  :summary "## Job Summary \n some markdown of the summary"}}))

  ;; view your check run here:
  (check-runs-helper gh-owner repo check-run-id)

  ;; we update that check run to show that it is running.
  (update-check-run gh-owner repo access-token check-run-id
    {:status "in_progress"
     :output {:title   "In Progress"
              :summary "## Job Summary \n I am busy running..."}})
  ;; You will see the check run updated.

  ;; we update that check run to a success.
  (update-check-run gh-owner repo access-token check-run-id
    {:status     "completed"
     :conclusion "failure"
     :output     {:title   "Completed"
                  :summary "## Job Summary \n I failed..."}})
  ;; You will see the check run updated.
  )
