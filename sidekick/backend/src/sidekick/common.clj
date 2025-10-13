(ns sidekick.common
  (:require
   [metabase.api.common :as api]
   [metabase.events.core :as events]
   [metabase.util :as u]
   [metabase.util.log :as log]
   [toucan2.core :as t2]))

(defn- create-local-db
  "Given the Sidekick database parent, creates a new local database for the given local."
  [sidekick-db-id local]
  (log/infof "Creating local database for Sidekick DB %s and local %s" sidekick-db-id local)
  (let [sidekick-db (t2/select-one :model/Database :id sidekick-db-id)
        {:keys [name details]} sidekick-db]
    (u/prog1 (t2/insert-returning-instance!
              :model/Database
              (merge
               (dissoc sidekick-db :id :created_at :initial_sync_status :features :description :updated_at)
               {:name         (str name " (" local ")")
                :engine       :sidekick
                :is_full_sync false
                :details      (merge (dissoc details :local-databases :admin-user :admin-password) {:local local :sidekick-parent-id sidekick-db-id})}))
             (events/publish-event! :event/database-create {:object <>
                                                            :user-id api/*current-user-id*
                                                            :details {:slug (:name <>)
                                                                      :sidekick_db_name (:name sidekick-db)
                                                                      :sidekick_db_id (:id sidekick-db)}}))))

(defn sidekick-db->local-db
  "Given a Sidekick database and local, returns the the local database that should be used to connect."
  [sidekick-db local]
  (log/infof "Fetching local database for Sidekick DB %s and local %s" (u/the-id sidekick-db) local)
  (if (= (:engine sidekick-db) :sidekick)
    (let [local-db-id (get-in sidekick-db [:details :local-databases (keyword local)])] 
      (log/infof "Local database ID is %s" local-db-id)
      (if-let [local-db (if local-db-id (t2/select-one :model/Database :id local-db-id) nil)]
        local-db
        (let [local-db (create-local-db (:id sidekick-db) local)]
          (log/infof "Created local database is: %s" (pr-str local-db))
          (t2/update! :model/Database (u/the-id sidekick-db) {:details (assoc-in (:details sidekick-db) [:local-databases local] (u/the-id local-db))})
          local-db)))
    (throw (ex-info "Not a Sidekick database" {:status-code 400}))))
