(ns sidekick.model
  (:require
   [methodical.core :as methodical]
   [toucan2.core :as t2]))

(methodical/defmethod t2/table-name :model/LocalDatabase [_model] :local_database)

(doto :model/LocalDatabase
  (derive :metabase/model))
