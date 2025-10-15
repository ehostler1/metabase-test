(ns sidekick.settings
  (:require
   [metabase.settings.core :refer [defsetting]]
   [metabase.util.i18n :refer [deferred-tru]]))

(defsetting sidekick-locals-collection-id
  (deferred-tru "The ID of the collection where Sidekick local collections should be created.")
  :default    nil
  :type       :integer
  :visibility :admin
  :export?    true)
