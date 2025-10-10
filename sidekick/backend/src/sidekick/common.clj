(ns sidekick.common
  (:require
   [metabase.util :as u]
   [metabase.util.log :as log]
   [toucan2.core :as t2]))

(defn- create-local-db
  "Given the Sidekick database parent, creates a new local database for the given local."
  [sidekick-db-id local]
  (log/infof "Creating local database for Sidekick DB %s and local %s" sidekick-db-id local)
  (let [sidekick-db (t2/select-one :model/Database :id sidekick-db-id)
        {:keys [name details is_on_demand auto_run_queries cache_ttl creator_id is_full_sync]} sidekick-db]
    (first (t2/insert-returning-instances!
            :model/Database
            {:name         (str name " (" local ")")
             :engine       :sidekick
             :details      (merge details {:local local :sidekick-parent-id sidekick-db-id :local-databases {}})
             :auto_run_queries   auto_run_queries
             :is_full_sync       is_full_sync
             :is_on_demand       is_on_demand
             :cache_ttl          cache_ttl
             :creator_id         creator_id}))))

(defn sidekick-db->local-db
  "Given a Sidekick database and local, returns the the local database that should be used to connect."
  [sidekick-db local]
  (log/infof "Fetching local database for Sidekick DB %s and local %s" (u/the-id sidekick-db) local)
  (if (= (:engine sidekick-db) :sidekick)
    (let [local-db-id (get-in sidekick-db [:details :local-databases (keyword local)])]
      (log/infof "Sidekick database local databases: %s" (pr-str (get-in sidekick-db [:details :local-databases])))
      (log/infof "Local database ID is %s" local-db-id)
      (if-let [local-db (if local-db-id (t2/select-one :model/Database :id local-db-id) nil)]
        local-db
        (let [local-db (create-local-db (:id sidekick-db) local)]
          (log/infof "Created local database is: %s" (pr-str local-db))
          (t2/update! :model/Database (u/the-id sidekick-db) {:details (assoc-in (:details sidekick-db) [:local-databases local] (u/the-id local-db))})
          local-db)))
    (throw (ex-info "Not a Sidekick database" {:status-code 400}))))
