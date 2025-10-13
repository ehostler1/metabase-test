(ns sidekick.middleware
  (:require

   [metabase.api.common :as api]
   [metabase.lib.metadata :as lib.metadata]
   [metabase.query-processor.schema :as qp.schema]
   ^{:clj-kondo/ignore [:discouraged-namespace]} [metabase.query-processor.store :as qp.store]
   [metabase.util :as u]
   [metabase.util.i18n :refer [tru]]
   [metabase.util.log :as log]
   [metabase.util.malli :as mu]
   [sidekick.common :as sk.comm]))

(defn- with-local
  "Adds the `local` to the Metadata Provider."
  [local & {:keys [qp query rff]}]
  (let [provider (qp.store/metadata-provider)
        database (lib.metadata/database provider)
        rff* (fn [metadata]
               (binding [qp.store/*DANGER-allow-replacing-metadata-provider* true]
                 (qp.store/with-metadata-provider (u/id database)
                   (rff metadata))))]
    (binding [qp.store/*DANGER-allow-replacing-metadata-provider* true]
      (qp.store/with-metadata-provider (u/id (sk.comm/sidekick-db->local-db database local))
        (qp query rff*)))))

(mu/defn swap-local-db :- ::qp.schema/qp
  "Swaps the base Sidekick database with the appropriate local database."
  [qp :- ::qp.schema/qp]
  (fn [query rff]
    (let [database (lib.metadata/database (qp.store/metadata-provider))]
      (if (and (= (:engine database) :sidekick) (nil? (:local (:details database))))
        (let [local (get (api/current-user-attributes) "local")] 
          (cond
            (nil? @api/*current-user*)
            (throw (ex-info (tru "Anonymous users cannot access a Sidekick database.") {:status-code 400}))

            ;; src/metabase/embedding/api/common.clj binds api/*is-superuser?* to true for embedded sessions, so use the property on the user instead to determine if the user is a superuser
            (get @api/*current-user* :is_superuser)
            (do
              (log/info "Superuser detected. Use the admin username and password.")
              (qp query rff))

            (string? local)
            (do
              (log/info "Local user detected. Use the templated username and password.")
              (with-local local {:qp qp :query query :rff rff}))

            :else
            (throw (ex-info (tru "Required user attribute `local` is missing. Cannot access Sidekick database.") {:status-code 400}))
            ))
        (qp query rff)))))
