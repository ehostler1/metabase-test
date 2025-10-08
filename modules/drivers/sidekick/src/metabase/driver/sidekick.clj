(ns metabase.driver.sidekick
  "Driver for the Sidekick database. Uses the official Microsoft JDBC driver under the hood (pre-0.25.0, used jTDS)."
  (:require

   [clojure.string :as str]
   [metabase.driver :as driver]
   [metabase.driver-api.core :as driver-api]
   [metabase.driver.sql-jdbc.common :as sql-jdbc.common]
   [metabase.driver.sql-jdbc.connection :as sql-jdbc.conn]
   [metabase.util.log :as log]))

(set! *warn-on-reflection* true)

(driver/register! :sidekick, :parent #{:sqlserver})

(defmethod sql-jdbc.conn/connection-details->spec :sidekick [_ details-map]
  (let [{:keys [local-user local-password admin-user admin-password db host port domain local]
         :as   details}
        (merge details-map {:local-user (or (:local-user details-map) "customReports_{local}")})] ;; default values
    (log/infof "Connection spec requested for local: %s" local)
    (-> {:applicationName    driver-api/mb-version-and-process-identifier
         :subprotocol        "sqlserver"
       ;; it looks like the only thing that actually needs to be passed as the `subname` is the host; everything else
       ;; can be passed as part of the Properties
         :subname            (str "//" host)
         :port               port
       ;; everything else gets passed as `java.util.Properties` to the JDBC connection.  (passing these as Properties
       ;; instead of part of the `:subname` is preferable because they support things like passwords with special
       ;; characters)
         :database           db
         :password           (if (string? local) local-password admin-password)
       ;; Wait up to 10 seconds for connection success. If we get no response by then, consider the connection failed
         :loginTimeout       10
       ;; apparently specifying `domain` with the official SQLServer driver is done like `user:domain\user` as opposed
       ;; to specifying them seperately as with jTDS see also:
       ;; https://social.technet.microsoft.com/Forums/sqlserver/en-US/bc1373f5-cb40-479d-9770-da1221a0bc95/connecting-to-sql-server-in-a-different-domain-using-jdbc-driver?forum=sqldataaccess
         :user               (str (when domain (str domain "\\")) (if (string? local) (str/replace local-user "{local}" local) admin-user))
         :instanceName       ""
         :encrypt            false
       ;; only crazy people would want this. See https://docs.microsoft.com/en-us/sql/connect/jdbc/configuring-how-java-sql-time-values-are-sent-to-the-server?view=sql-server-ver15
         :sendTimeAsDatetime false}
      ;; only include `port` if it is specified; leave out for dynamic port: see
      ;; https://github.com/metabase/metabase/issues/7597
        (sql-jdbc.common/handle-additional-options details, :seperator-style :semicolon))))

;; (defmethod sql.qp/preprocess :sidekick
;;   [driver inner-query]
;;   (log/warnf "The inner-query object: %s" (pr-str inner-query))
;;   (let [parent-preprocess (get-method sql.qp/preprocess :sqlserver)]
;;     (parent-preprocess driver inner-query))) ;; delegate to the sqlserver preprocess, since Sidekick is just a thin wrapper around SQL Server
