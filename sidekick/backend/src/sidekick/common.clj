(ns sidekick.common
  (:require 
   [metabase.util :as u]
   [metabase.util.log :as log]
   [toucan2.core :as t2]))

(defn- create-local-db
  "Given the Sidekick database parent, creates a new local database for the given local."
  [sidekick-db-id local]
  (let [sidekick-db (t2/select-one :model/Database :id sidekick-db-id)
        {:keys [name details is_on_demand auto_run_queries cache_ttl creator_id is_full_sync]} sidekick-db]
    (log/infof "Database info: %s" (pr-str sidekick-db))
    (first (t2/insert-returning-instances!
            :model/LocalDatabase
            {:name         (str name " (" local ")")
             :engine       :sidekick
             :details      (merge details {:local local :sidekick-parent-id sidekick-db-id :local-databases {}})
             :auto-run-queries   auto_run_queries
             :is-full-sync       is_full_sync
             :is-on-demand       is_on_demand
             :cache-ttl          cache_ttl
             :creator-id         creator_id}))))

(defn sidekick-db->local-db
  "Given a Sidekick database and local, returns the the local database that should be used to connect."
  [sidekick-db local]
  (log/infof "Fetching local database for Sidekick DB %s and local %s" (u/the-id sidekick-db) local)
  (if (= (:engine sidekick-db) :sidekick)
    (let [local-db-id (get-in sidekick-db [:details :local-databases local])]
      (if-let [local-db (if local-db-id (t2/select-one :model/LocalDatabase :id local-db-id) nil)]
        local-db
        (let [local-db (create-local-db (:id sidekick-db) local)]
          (log/info "This is a test")
          (t2/update! :model/Database (u/the-id sidekick-db) {:details {:local-databases {local (u/the-id local-db)}}})
          local-db)))
    (throw (ex-info "Not a Sidekick database" {:status-code 400}))))
