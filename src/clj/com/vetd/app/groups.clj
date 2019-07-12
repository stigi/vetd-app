(ns com.vetd.app.groups
  (:require [com.vetd.app.db :as db]
            [com.vetd.app.common :as com]
            [com.vetd.app.util :as ut]
            [com.vetd.app.hasura :as ha]
            [com.vetd.app.email-client :as ec]
            [com.vetd.app.links :as l]
            [clojure.string :as st]
            [buddy.hashers :as bhsh]
            [taoensso.timbre :as log]
            [honeysql.core :as hs]))

(defn insert-group-org-membership
  [org-id group-id]
  (let [[id idstr] (ut/mk-id&str)]
    (-> (db/insert! :group_org_memberships
                    {:id id
                     :idstr idstr
                     :created (ut/now-ts)
                     :updated (ut/now-ts)
                     :org_id org-id
                     :group_id group-id})
        first)))

(defn select-group-org-memb-by-ids
  [org-id group-id]
  (-> [[:group-org-memberships
        {:group-id group-id
         :org-id org-id}
        [:id :group-id :org-id :created]]]
      ha/sync-query
      vals
      ffirst))

(defn create-or-find-group-org-memb
  [org-id group-id]
  (if-let [memb (select-group-org-memb-by-ids org-id group-id)]
    [false memb]
    [true (insert-group-org-membership org-id group-id)]))

(defn insert-group [group-name admin-org-id]
  (let [[id idstr] (ut/mk-id&str)]
    (-> (db/insert! :groups
                    {:id id
                     :idstr idstr
                     :created (ut/now-ts)
                     :updated (ut/now-ts)
                     :gname group-name
                     :admin_org_id admin-org-id})
        first)))

(defmethod com/handle-ws-inbound :create-group
  [{:keys [group-name admin-org-id]} ws-id sub-fn]
  (insert-group group-name admin-org-id)
  {})

(defmethod com/handle-ws-inbound :add-org-to-group
  [{:keys [org-id group-id]} ws-id sub-fn]
  (create-or-find-memb org-id group-id)
  {})