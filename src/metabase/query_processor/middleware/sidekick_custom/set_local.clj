(ns metabase.query-processor.middleware.sidekick-custom.set-local
  (:require

   [metabase.api.common :as api]
   [metabase.lib.metadata :as lib.metadata]
   [metabase.lib.metadata.protocols :as lib.metadata.protocols]
   [metabase.query-processor.schema :as qp.schema]
   [metabase.query-processor.store :as qp.store]
   [metabase.util :as u]
   [metabase.util.i18n :refer [tru]]
   [metabase.util.log :as log]
   [metabase.util.malli :as mu]))


(defn- with-local
  "Adds the `local` to the Metadata Provider."
  [local & body]
  (let [{:keys [qp query rff]} body]
    (log/infof "QP: %s" (pr-str qp))
    (log/infof "Query: %s" (pr-str query))
    (log/infof "RFF: %s" (pr-str rff))
    (let [provider (qp.store/metadata-provider)
          database (lib.metadata/database provider)
          rff* (fn [metadata]
                 (binding [qp.store/*DANGER-allow-replacing-metadata-provider* true]
                   (qp.store/with-metadata-provider (u/id database)
                     (rff metadata))))]
      (binding [qp.store/*DANGER-allow-replacing-metadata-provider* true]
        (qp.store/with-metadata-provider (reify lib.metadata.protocols/MetadataProvider
                                           (database [_this] (assoc database :details (assoc (:details database) :local local)))
                                           (metadatas [_this metadata-spec] (lib.metadata.protocols/metadatas provider metadata-spec))
                                           (setting [_this setting-key] (lib.metadata/setting provider setting-key)))
          (qp query rff*))))))

(mu/defn set-local :- ::qp.schema/qp
  "Sets the connection details local for Sidekick databases."
  [qp :- ::qp.schema/qp]
  (fn [query rff]
    (let [database (lib.metadata/database (qp.store/metadata-provider))]
      (if (and (= (:engine database) :sidekick) (nil? (:local (:details database))))
        (let [local (get (api/current-user-attributes) "local")]
          (cond
            (nil? @api/*current-user*)
            (throw (ex-info (tru "Anonymous users cannot access a Sidekick database.") {:status-code 400}))

            api/*is-superuser?*
            (do
              (log/info "Superuser detected. Use the admin username and password.")
              (qp query rff))

            (string? local)
            (do
              (log/info "Local user detected. Use the templated username and password.")
              (with-local local (qp query rff)))

            :else
            (throw (ex-info (tru "Required user attribute `local` is missing. Cannot access Sidekick database.") {:status-code 400}))
            ))
        (qp query rff))))
)
