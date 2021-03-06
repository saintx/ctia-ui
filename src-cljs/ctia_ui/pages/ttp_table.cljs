(ns ctia-ui.pages.ttp-table
  (:require
    [ctia-ui.components :refer [ConfidenceCell
                                EntityTablePage
                                TLPCell]]
    [ctia-ui.config :refer [config]]
    [ctia-ui.state :refer [app-state]]
    [ctia-ui.util :refer [encode-uri]]
    [oakmac.util :refer [atom-logger by-id fetch-json-as-clj js-log log]]
    [rum.core :as rum]))

;;------------------------------------------------------------------------------
;; Data Fetching
;;------------------------------------------------------------------------------

;; NOTE: these functions should probably be in their own namespace

(defn- ttps-url
  ([] (ttps-url ""))
  ([query-str]
   (if (:in-demo-mode? config)
     "data/fake-ttps.json?_slow=true"
     (str (:api-base-url config) "ctia/ttp/search?query=*" (encode-uri query-str)))))

(defn- fetch-ttps-error [request-page-id]
  ;; make sure we are still on the same page instance when the request returns
  (when (= request-page-id (:page-id @app-state))
    (swap! app-state update-in [:ttp-table] merge
      {:ajax-error? true
       :loading? false})))

(defn- fetch-ttps-success [request-page-id new-data]
  ;; make sure we are still on the same page instance when the request returns
  (when (= request-page-id (:page-id @app-state))
    (swap! app-state update-in [:ttp-table] merge
      {:loading? false
       :data new-data})))

(defn- fetch-ttps [next-fn error-fn]
  (fetch-json-as-clj (ttps-url) next-fn error-fn))

;;------------------------------------------------------------------------------
;; Page Components
;;------------------------------------------------------------------------------

(rum/defc TTPExpandedRow < rum/static
  [an-indicator]
  [:div.details-wrapper-819a2
    "TODO: TTP Expanded row goes here"])

;;------------------------------------------------------------------------------
;; Initial Page State
;;------------------------------------------------------------------------------

;; NOTE: just guessing at these columns for now
;; more discussion: https://github.com/threatgrid/ctia-ui/issues/28
(def cols
  [{:th "Campaign"
    :td :description}
   {:th "Version"
    :td :producer}
   {:th "Status"
    :td ConfidenceCell}
   {:th "Intent"
    :td TLPCell}
   {:th "Conf."
    :td :modified}
   {:th "ID"
    :td :modified}
   {:th "Last Modified"
    :td :modified}])

(def initial-page-state
  {:ajax-error? false
   :cols cols
   :data []
   :entity-name "TTPs"
   :expanded-row-cmp TTPExpandedRow
   :expanded-rows #{}
   :hovered-row-id nil
   :loading? true
   :search-txt ""})

;;------------------------------------------------------------------------------
;; Top Level Page Component
;;------------------------------------------------------------------------------

(def left-nav-tab "TTPs")

(rum/defc TTPTablePage < rum/static
  [state]
  (EntityTablePage state left-nav-tab :ttp-table))

;;------------------------------------------------------------------------------
;; Page Init / Destroy
;;------------------------------------------------------------------------------

(defn init-ttp-table-page! []
  (let [new-page-id (str (random-uuid))]
    (fetch-ttps (partial fetch-ttps-success new-page-id)
                (partial fetch-ttps-error new-page-id))
    (swap! app-state assoc :page :ttp-table
                           :page-id new-page-id
                           :ttp-table initial-page-state)))

(defn destroy-ttp-table-page! []
  (swap! app-state dissoc :page :page-id :ttp-table))
