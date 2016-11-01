(ns ctia-ui.components
  (:require
    [clojure.string :refer [blank? lower-case]]
    [ctim.schemas.common :refer [disposition-map]]
    [ctim.schemas.vocabularies :refer [observable-type-identifier]]
    [goog.events.KeyCodes :refer [BACKSPACE DOWN ENTER ESC UP]]
    [ctia-ui.data :refer [crud-left-nav]]
    [ctia-ui.state :refer [app-state]]
    [ctia-ui.util :refer [random-element-id truncate-id vec-remove]]
    [oakmac.util :refer [by-id js-log log]]
    [rum.core :as rum]))

(def $ js/jQuery)

;;------------------------------------------------------------------------------
;; SVG Icons
;;------------------------------------------------------------------------------

(rum/defc SVGIcon < rum/static
  [svg-class icon-id]
  [:svg {:class svg-class
         :dangerouslySetInnerHTML
           {:__html (str "<use xlink:href='images/icon-sprite.svg#" icon-id "' />")}}])

;;------------------------------------------------------------------------------
;; Labels
;;------------------------------------------------------------------------------

(rum/defc InputLabel < rum/static
  [txt required?]
  [:label {:class (if required? "required-input-label-29a82" "input-label-a1af2")}
    txt])

;;------------------------------------------------------------------------------
;; Checkbox
;;------------------------------------------------------------------------------

(defn- click-checkbox [app-path _js-evt]
  (swap! app-state update-in app-path not))

(rum/defc Checkbox < rum/static
  [app-path label checked?]
  [:button
    {:class (if checked? "selected-checkbox-da5f6" "checkbox-7b421")
     :on-click (partial click-checkbox app-path)}
    label])

;;------------------------------------------------------------------------------
;; Text Inputs
;;------------------------------------------------------------------------------

(defn- on-change-text-input [app-path js-evt]
  (let [new-txt (aget js-evt "currentTarget" "value")]
    (swap! app-state assoc-in app-path new-txt)))

(rum/defc TextInput < rum/static
  [app-path txt placeholder-txt]
  [:input.text-input-65a69
    {:on-change (partial on-change-text-input app-path)
     :placeholder placeholder-txt
     :type "text"
     :value txt}])

(rum/defc TextareaInput < rum/static
  [app-path txt placeholder-txt]
  [:textarea.text-area-bc533
    {:on-change (partial on-change-text-input app-path)
     :placeholder placeholder-txt
     :type "text"
     :value txt}])

;;------------------------------------------------------------------------------
;; Disposition Buttons
;;------------------------------------------------------------------------------

(defn- click-disposition-btn [app-path disposition-num _js-evt]
  (swap! app-state assoc-in app-path disposition-num))

(rum/defc DispositionButton < rum/static
  [app-path disposition-num txt active? extra-class]
  [:label
    {:class (str "btn-series-med-7566f"
                 (when active? " white-2ca39")
                 (when extra-class (str " " extra-class)))
     :on-click (partial click-disposition-btn app-path disposition-num)}
    txt])

;; TODO: map this to disposition-map
(rum/defc DispositionButtons < rum/static
  [app-path disposition-number]
  [:div.group-be764
    (DispositionButton app-path 1 "Clean" (= disposition-number 1) "first-child")
    (DispositionButton app-path 2 "Malicious" (= disposition-number 2))
    (DispositionButton app-path 3 "Suspicious" (= disposition-number 3))
    (DispositionButton app-path 4 "Common" (= disposition-number 4))
    (DispositionButton app-path 5 "Unknown" (= disposition-number 5) "last-child")])

;;------------------------------------------------------------------------------
;; Confidence Buttons
;;------------------------------------------------------------------------------

(defn- click-confidence-btn [app-path txt _js-evt]
  (swap! app-state assoc-in app-path txt))

(rum/defc ConfidenceButton < rum/static
  [app-path txt active? extra-class]
  [:label
    {:class (str "btn-series-med-7566f"
                 (when active? " white-2ca39")
                 (when extra-class (str " " extra-class)))
     :on-click (partial click-confidence-btn app-path txt)}
    txt])

(rum/defc ConfidenceButtons < rum/static
  [app-path active-txt]
  [:div.group-be764
    (ConfidenceButton app-path "None" (= active-txt "None") "first-child")
    (ConfidenceButton app-path "Low" (= active-txt "Low"))
    (ConfidenceButton app-path "Medium" (= active-txt "Medium"))
    (ConfidenceButton app-path "High" (= active-txt "High") "last-child")])

;;------------------------------------------------------------------------------
;; Campaign Status
;;------------------------------------------------------------------------------

;; TODO: make a generic component for "Buttons" and combine these

(defn- click-campaign-status-button [app-path txt _js-evt]
  (swap! app-state assoc-in app-path txt))

(rum/defc CampaignStatusButton < rum/static
  [app-path txt active? extra-class]
  [:label
    {:class (str "btn-series-med-7566f"
                 (when active? " white-2ca39")
                 (when extra-class (str " " extra-class)))
     :on-click (partial click-campaign-status-button app-path txt)}
    txt])

(rum/defc CampaignStatusButtons < rum/static
  [app-path status]
  [:div.group-be764
    (CampaignStatusButton app-path "Historic" (= status "Historic") "first-child")
    (CampaignStatusButton app-path "Ongoing" (= status "Ongoing"))
    (CampaignStatusButton app-path "Future" (= status "Future") "last-child")])

;;------------------------------------------------------------------------------
;; CAPEC Input
;;------------------------------------------------------------------------------

(defn- select-capec [app-path js-evt js-ui]
  (.preventDefault js-evt)
  (let [js-capec (aget js-ui "item" "value")
        capec {:capec-id (aget js-capec "id")
               :description (aget js-capec "description")}
        search-txt (str (:capec-id capec) " - " (:description capec))]
    (swap! app-state assoc-in app-path {:capec capec
                                        :search-txt search-txt})))

(defn- on-change-capec [app-path js-evt]
  (let [new-value (aget js-evt "currentTarget" "value")]
    (swap! app-state assoc-in (conj app-path :search-txt) new-value)))

(def capec-url "data/capec.json")

(def capec-input-mixin
  {:will-mount
   (fn [state]
     (assoc state ::input-id (random-element-id)))

   :did-mount
   (fn [state]
     (let [input-id (::input-id state)
           [app-path _opts] (:rum/args state)
           js-options (js-obj "select" (partial select-capec app-path)
                              "source" capec-url)]
       (.autocomplete ($ (str "#" input-id)) js-options))
     state)

   :will-unmount
   (fn [state]
     (let [input-id (::input-id state)]
       (.autocomplete ($ (str "#" input-id)) "destroy"))
     state)})

(rum/defcs CAPECInput < (merge rum/static capec-input-mixin)
  [state app-path {:keys [search-txt capec]}]
  (let [input-id (::input-id state)]
    [:div.chunk-e556a
      (InputLabel "CAPEC")
      [:input.text-input-65a69
        {:id input-id
         :on-change (partial on-change-capec app-path)
         :value search-txt}]]))

;;------------------------------------------------------------------------------
;; Reference Input
;;------------------------------------------------------------------------------

(defn- click-remove-entity-row [app-path idx _js-evt]
  (swap! app-state update-in (conj app-path :entities) vec-remove idx))

(def describable-entity-row-mixin
  {:key-fn
   (fn [app-path idx m]
     (str (pr-str app-path) "-" idx "-" (pr-str m)))})

(rum/defc DescribableEntityRow < (merge rum/static describable-entity-row-mixin)
  [app-path idx {:keys [title description id]}]
  [:div.row-fd6fe
    [:div.title-90d7c title]
    [:div.id-7a918 id]
    [:div.description-1a102 description]
    [:div.remove-ef67b
      [:div.outline-btn-sml-b39fb {:on-click (partial click-remove-entity-row app-path idx)}
        "Remove"]]])

(defn- on-change-ref-input-text [app-path js-evt]
  (let [new-txt (aget js-evt "currentTarget" "value")]
    (swap! app-state assoc-in (conj app-path :search-txt) new-txt)))

(defn- on-entity-select [app-path js-evt js-ui]
  (let [js-itm (aget js-ui "item")
        itm (js->clj js-itm :keywordize-keys true)]
    (swap! app-state update-in (conj app-path :entities) conj itm)))

;; NOTE: do we need to truncate the description here?
(defn- entity-autocomplete-row-html [js-itm]
  (str
    "<li>"
    (aget js-itm "title")
    (when (aget js-itm "id")
      (str "<span class=id-0ed5b>" (truncate-id (aget js-itm "id") 6) "</span>"))
    (when (aget js-itm "description")
      (str "<div class=secondary-cdca9>" (aget js-itm "description") "</div>"))
    "</li>"))

(defn- autocomplete-row-render [$ul js-itm]
  (doto ($ (entity-autocomplete-row-html js-itm))
        (.appendTo $ul)))

;;!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
;; Start Demo Code
;;!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

(def in-demo-mode? true)

(defn- demo-filter [search-txt itm]
  (let [title (lower-case (get itm "title"))
        search-txt (lower-case search-txt)]
    (not= -1 (.indexOf title search-txt))))

(defn- demo-sort [a b]
  (compare (get a "title") (get b "title")))

(defn- demo-source-fn [url js-request js-response-fn]
  (let [search-txt (aget js-request "term")]
    (.getJSON $ url
      (fn [js-data]
        (->> js-data
             js->clj
             (filter (partial demo-filter search-txt))
             (sort demo-sort)
             clj->js
             js-response-fn)))))

;;!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
;; End Demo Code
;;!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

(def reference-input-mixin
  {;; create a random id for the input element
   :will-mount
   (fn [state]
     (assoc state ::input-id (random-element-id)))

   ;; initialize jquery-ui autocomplete on the input element
   :did-mount
   (fn [state]
     (let [input-id (::input-id state)
           [app-path opts] (:rum/args state)
           url (:url opts)
           $input-el ($ (str "#" input-id))
           js-options (js-obj "source" (if in-demo-mode? (partial demo-source-fn url) url)
                              "select" (partial on-entity-select app-path))
           $autocomplete (.autocomplete $input-el js-options)]
       (aset (.autocomplete $autocomplete "instance") "_renderItem" autocomplete-row-render))
     state)

   ;; destroy the jquery-ui widget
   :will-unmount
   (fn [state]
     (let [input-id (::input-id state)
           $input-el ($ (str "#" input-id))]
       (.autocomplete $input-el "destroy"))
     state)})

(def default-reference-input-placeholder "Find …")

(rum/defcs ReferenceInput < (merge rum/static reference-input-mixin)
  [state app-path {:keys [entities placeholder search-txt url]}]
  (let [input-id (::input-id state)
        placeholder (if-not placeholder default-reference-input-placeholder placeholder)]
    [:div.input-wrapper-ac07d
      [:input.text-input-65a69
        {:id input-id
         :on-change (partial on-change-ref-input-text app-path)
         :placeholder placeholder
         :type "text"
         :value search-txt}]
      [:div
        (map-indexed (partial DescribableEntityRow app-path) entities)]]))

;;------------------------------------------------------------------------------
;; Date Input
;;------------------------------------------------------------------------------

(rum/defc CalendarIconLabel < rum/static
  [for-id]
  [:label {:for for-id}
    (SVGIcon "date-time-icon-bd68d" "calendar")])

(def calendar-num-months 2)

(def js-day-names
  (array "S" "M" "T" "W" "T" "F" "S"))

(def js-calendar-options
  (js-obj "currentText" ""
          "dateFormat" "M. d, yy"
          "dayNamesMin" js-day-names
          "numberOfMonths" calendar-num-months
          "selectOtherMonths" true
          "showAnim" ""
          "showButtonPanel" true
          "showOtherMonths" true))

(defn- select-datepicker-date [app-path new-date]
  (swap! app-state assoc-in app-path new-date))

(def date-input-mixin
  {;; initialize the datepicker after mounting
   :did-mount
   (fn [state]
     (let [[app-path input-id _value _placeholder] (:rum/args state)
           select-fn (partial select-datepicker-date app-path)
           js-datepicker-options (.extend $ (js-obj)
                                            js-calendar-options
                                            (js-obj "onSelect" select-fn))]
       (.datepicker ($ (str "#" input-id)) js-datepicker-options))
     state)

   ;; destroy the datepicker on unmount
   :will-unmount
   (fn [state]
     (let [[_app-path input-id _value _placeholder] (:rum/args state)]
       (.datepicker ($ (str "#" input-id)) "destroy"))
     state)})

(rum/defc DateInput < (merge rum/static date-input-mixin)
  [app-path input-id value placeholder]
  [:div.group-fe67d
    [:input.date-9e63a
      {:id input-id
       :placeholder placeholder
       :type "text"
       :value value}]
    (CalendarIconLabel input-id)])

;;------------------------------------------------------------------------------
;; Time Range
;;------------------------------------------------------------------------------

(defn- click-no-end-date [app-path _js-evt]
  (swap! app-state update-in (conj app-path :valid-to-present?) not))

(def time-range-mixin
  {;; create random ids for the start and end input elements when the component mounts
   :will-mount
   (fn [state]
     (assoc state ::start-id (random-element-id)
                  ::end-id (random-element-id)))})

(rum/defcs TimeRange < (merge rum/static time-range-mixin)
  [state app-path {:keys [start-time end-time valid-to-present?]} not-required?]
  (let [start-id (::start-id state)
        end-id (::end-id state)]
    [:div
      [:div.group-1b8a3
        [:label
          {:class (if not-required? "input-label-a1af2" "required-input-label-29a82")
           :for start-id}
          "Valid Time"]
        (DateInput (conj app-path :start-time) start-id start-time "Start Date")
        (when-not valid-to-present?
          [:div.interstitial-d6112 "to"])
        (when-not valid-to-present?
          (DateInput (conj app-path :end-time) end-id end-time "End Date"))]
      [:div.group-fe67d
        [:button
          {:class (if valid-to-present? "selected-checkbox-da5f6" "checkbox-7b421")
           :on-click (partial click-no-end-date app-path)}
          "No End Date"]]]))

;;------------------------------------------------------------------------------
;; Observed Time
;;------------------------------------------------------------------------------

;; TODO: pretty sure TimeRange and ObservedTime could be combined into one component

(defn- click-toggle-observed-time-range [app-path _js-evt]
  (swap! app-state update-in (conj app-path :range?) not))

(def observed-time-mixin
  {;; create random ids for the start and end input elements when the component mounts
   :will-mount
   (fn [state]
     (assoc state ::start-id (random-element-id)
                  ::end-id (random-element-id)))})

(rum/defcs ObservedTime < (merge rum/static observed-time-mixin)
  [state app-path {:keys [start_time end_time range?]} not-required?]
  (let [start-id (::start-id state)
        end-id (::end-id state)]
    [:div
      [:div.group-1b8a3
        [:label
          {:class (if not-required? "input-label-a1af2" "required-input-label-29a82")
           :for start-id}
          "Observed Time"]
        (DateInput (conj app-path :start_time) start-id start_time "Start Date")
        (when range?
          [:div.interstitial-d6112 "to"])
        (when range?
          (DateInput (conj app-path :end_time) end-id end_time "End Date"))]
      [:div.group-fe67d
        [:button
          {:class (if range? "checkbox-7b421" "selected-checkbox-da5f6")
           :on-click (partial click-toggle-observed-time-range app-path)}
          "No End Date"]]]))

;;------------------------------------------------------------------------------
;; Number Input
;;------------------------------------------------------------------------------

;; TODO:
;; - add min / max optional arguments here
(rum/defc NumberInput < rum/static
  [app-path n]
  [:input.text-input-65a69
    {:on-change (partial on-change-text-input app-path)
     :type "number"
     :value n}])

;;------------------------------------------------------------------------------
;; Vocabulary Token Input
;;------------------------------------------------------------------------------

(defn- click-remove-token [cursor idx]
  (swap! cursor update-in [:tokens] vec-remove idx))

(def token-mixin
  {:key-fn
   (fn [cursor idx txt]
     (str idx "-" txt))})

(rum/defc Token < (merge rum/static token-mixin)
  [cursor idx txt]
  [:div.token-0ec5d
    [:div.token-label-a2683 txt]
    [:div.remove-9f89b {:on-click (partial click-remove-token cursor idx)}]
    ;; TODO: only add the tooltip if the text is longer than N characters?
    [:span.tooltip-arrow-down-ef7a4 txt]])

(defn- beginning-match?
  "Is search-txt at the beginning of option-txt?"
  [search-txt option-txt]
  (zero? (.indexOf (lower-case option-txt) (lower-case search-txt))))

(defn- any-match?
  "Is search-txt inside of option-txt at all?"
  [search-txt option-txt]
  (not= -1 (.indexOf (lower-case option-txt) (lower-case search-txt))))

(defn- sort-options
  "Put matches that start at the beginning of the string ahead of matches that
   exist anywhere in the string."
  [search-txt a b]
  (let [a-beginning-match? (beginning-match? search-txt a)
        b-beginning-match? (beginning-match? search-txt b)]
    (cond
      (and a-beginning-match? (not b-beginning-match?))
      -1

      (and b-beginning-match? (not a-beginning-match?))
      1

      :else
      (compare a b))))

(defn- change-token-input [cursor all-options allow-freeform? js-evt]
  (let [new-search-txt (aget js-evt "currentTarget" "value")
        ;; match against any text
        new-options (filter (partial any-match? new-search-txt) all-options)
        ;; sort putting beginning matches first
        new-options (sort (partial sort-options new-search-txt) new-options)
        ;; add the freeform option if available
        new-options (if allow-freeform?
                      (conj (vec new-options) {:option-text new-search-txt, :option-type :freeform})
                      new-options)]
    (swap! cursor assoc :active-options new-options
                        :search-txt new-search-txt
                        :selected-option-idx 0)))

(defn- add-token [cursor new-value]
  (let [new-value (if (string? new-value) new-value (:option-text new-value))
        current-tokens (:tokens @cursor [])
        new-tokens (conj current-tokens new-value)
        all-options (:all-options @cursor)
        max-tokens (:max-tokens @cursor)
        at-max-tokens? (and (number? max-tokens)
                            (>= (count new-tokens) max-tokens))]
    (swap! cursor assoc :active-options all-options
                        :options-showing? (not at-max-tokens?)
                        :search-txt ""
                        :selected-option-idx 0
                        :tokens new-tokens)))

(defn- move-token-up [cursor current-idx active-options]
  (when-not (empty? active-options)
    (let [new-idx (if (zero? current-idx)
                    (dec (count active-options))
                    (dec current-idx))]
      (swap! cursor assoc :selected-option-idx new-idx))))

(defn- move-token-down [cursor current-idx active-options]
  (when-not (empty? active-options)
    (let [new-idx (if (= current-idx (dec (count active-options)))
                    0
                    (inc current-idx))]
      (swap! cursor assoc :selected-option-idx new-idx))))

(defn- key-down-token-input
  "Fires with every keydown event inside a Token Input."
  [cursor state js-evt]
  (let [input-el (aget js-evt "currentTarget")
        key-code (aget js-evt "keyCode")
        active-options (:active-options state)
        allow-freeform? (:allow-freeform? state)
        current-txt (:search-txt state "")
        current-tokens (:tokens state [])
        selected-option-idx (:selected-option-idx state 0)
        num-tokens (count current-tokens)]
    (cond
      ;; TODO: backspace should select the previous token, then allow delete
      ; ;; delete previous token on backspace with an empty field
      ; (and (= key-code BACKSPACE)
      ;      (blank? current-txt)
      ;      (pos? num-tokens))
      ; (click-remove-token cursor (dec num-tokens))

      ;; select the active token on enter
      (and (= key-code ENTER)
           (not (empty? active-options)))
      (add-token cursor (nth active-options selected-option-idx))

      ;; move up the list
      (= key-code UP)
      (do (.preventDefault js-evt)
          (move-token-up cursor selected-option-idx active-options))

      ;; move down the list
      (= key-code DOWN)
      (do (.preventDefault js-evt)
          (move-token-down cursor selected-option-idx active-options))

      ;; blur the input field and hide the options on escape
      (= key-code ESC)
      (do (.blur input-el)
          (swap! cursor assoc :options-showing? false))

      ;; else do nothing
      :else nil)))

(defn- mouseover-token-option [cursor option-idx _js-evt]
  (swap! cursor assoc :selected-option-idx option-idx))

(def active-token-id (random-element-id))

(def token-list-option-mixin
  {:did-update
   (fn [state]
     ;; update the scroll position of the active element
     (let [js-react-component (:rum/react-component state)
           menu-el (js/ReactDOM.findDOMNode js-react-component)
           menu-height (aget menu-el "offsetHeight")
           top-of-menu (aget menu-el "scrollTop")
           bottom-of-menu (+ menu-height top-of-menu)
           active-token-el (by-id active-token-id)
           token-height (aget active-token-el "offsetHeight")
           top-of-token (aget active-token-el "offsetTop")
           bottom-of-token (+ top-of-token token-height)]
       (cond
         ;; scroll up
         (< top-of-token top-of-menu)
         (aset menu-el "scrollTop" top-of-token)

         ;; scroll down
         (> bottom-of-token bottom-of-menu)
         (aset menu-el "scrollTop" (- bottom-of-token menu-height))

         :else nil))
     state)})

(rum/defc NormalListItem < rum/static
  [cursor txt idx active?]
  [:li (merge
         {:class (str "token-option-cd274" (when active? " active-2e410"))
          :on-click #(add-token cursor txt)
          :on-mouse-over (partial mouseover-token-option cursor idx)}
         (when active? {:id active-token-id}))
    txt])

(rum/defc FreeformListItem < rum/static
  [cursor option idx active?]
  [:div
    (when-not (zero? idx) [:hr])
    [:li (merge
           {:class (str "token-option-cd274" (when active? " active-2e410"))
            :on-click #(add-token cursor (:option-text option))
            :on-mouse-over (partial mouseover-token-option cursor idx)}
           (when active? {:id active-token-id}))
      (str "\"" (:option-text option) "\"")]])

(def list-item-mixin
  {:key-fn
   (fn [cursor option idx active?]
     (str (pr-str option) "-" idx "-" (if active? "0" "1")))})

(rum/defc ListItem < (merge rum/static list-item-mixin)
  [cursor option idx active?]
  (if (= (:option-type option) :freeform)
    (FreeformListItem cursor option idx active?)
    (NormalListItem cursor option idx active?)))

(rum/defc ListMenu < (merge rum/static token-list-option-mixin)
  [cursor active-options selected-option-idx]
  [:ul.token-down-menu-0e11b
    (map-indexed
      (fn [idx itm] (ListItem cursor itm idx (= idx selected-option-idx)))
      active-options)])

(rum/defc NoMatchesMenu < rum/static
  []
  [:ul.token-down-menu-0e11b
    [:li.nothing-found-75aec "No Matches Found"]])

(defn- focus-token-input
  "Show the options when the input field takes focus."
  [cursor]
  (swap! cursor assoc :has-focus? true
                      :options-showing? true))

;; NOTE: make this configurable?
(def hide-options-list-timeout-ms 200)
(def menu-fadeout-speed-ms 100)

(defn- blur-token-input
  "Hide the options shortly after they leave the input field."
  [cursor]
  (swap! cursor assoc :has-focus? false)
  (js/setTimeout
    (fn []
      ;; hide the options list if they have not re-focused the input field
      (when-not (:has-focus? @cursor)
        (swap! cursor assoc :options-showing? false)))
        ; (.fadeOut ($ (str "#" menu-id)) menu-fadeout-speed-ms
        ;   (fn [] (swap! cursor assoc :options-showing? false)))
    hide-options-list-timeout-ms))

(def default-token-input-placeholder "Add …")

(defn- vocab-input-default-values
  "Set default values for TokensInput config."
  [m]
  {:active-options (if (or (blank? (:search-txt m))
                           (not (coll? (:active-options m))))
                     (:all-options m [])
                     (:active-options m []))
   :all-options (:all-options m [])
   :allow-freeform? (true? (:allow-freeform? m))
   :max-tokens (:max-tokens m)
   :options-showing? (true? (:options-showing? m))
   :placeholder (if (string? (:placeholder m))
                  (:placeholder m)
                  default-token-input-placeholder)
   :searching? (true? (:searching? m))
   :search-txt (if (string? (:search-txt m))
                 (:search-txt m)
                 "")
   :selected-option-idx (if (number? (:selected-option-idx m))
                          (:selected-option-idx m)
                          0)
   :tokens (if (vector? (:tokens m))
             (:tokens m)
             [])})

;; TODO: change this to be app-path instead of cursor
;; allow either a cursor or an app-path to be passed in?
(rum/defc TokensInput < rum/static
  [cursor state]
  (let [state (vocab-input-default-values state)
        {:keys [active-options
                all-options
                allow-freeform?
                max-tokens
                options-showing?
                placeholder
                searching?
                selected-option-idx
                search-txt
                tokens]} state
        at-max-tokens? (and (number? max-tokens)
                            (>= (count tokens) max-tokens))]
    [:div.token-wrapper-eb049
      (map-indexed (partial Token cursor) tokens)
      [:div.token-input-wrapper-2eace
        ;; do not show the input field when we are at max tokens
        (when-not at-max-tokens?
          [:input.token-input-f464e
            {:on-blur (partial blur-token-input cursor)
             :on-change (partial change-token-input cursor all-options allow-freeform?)
             :on-focus (partial focus-token-input cursor)
             :on-key-down (partial key-down-token-input cursor state)
             :placeholder placeholder
             :value search-txt}])
        (cond
          (and options-showing?
               (not allow-freeform?)
               (empty? active-options))
          (NoMatchesMenu)

          (and options-showing?
               (not (empty? active-options)))
          (ListMenu cursor active-options selected-option-idx)

          :else nil)]]))

;;------------------------------------------------------------------------------
;; Left Nav Tabs
;;------------------------------------------------------------------------------

;; NOTE: we could just use anchor tags instead this custom event
(defn- click-left-tab [route _js-evt]
  (when (and (string? route) (not (blank? route)))
    (aset js/document "location" "hash" route)))

(def left-tab-mixin
  {:key-fn
   (fn [txt selected?]
     (str txt "-" (if selected? "1" "0")))})

;; TODO: need to do something here about the icons flickering when switching tabs
(rum/defc LeftNavTab < (merge rum/static left-tab-mixin)
  [{:keys [icon-id txt route]} selected?]
  [:li {:class (str "tab-5bd07" (when selected? " selected"))
        :on-click (partial click-left-tab route)}
    [:div.btn-74b6e
      (SVGIcon "icon-ba6b1" icon-id)
      [:span.label-7dafe txt]
      [:span.tooltip-arrow-left-f034c txt]]])

(rum/defc LeftNavTabs < rum/static
  [active-nav-section]
  [:div.pg-nav-a5f89
    [:ul.pg-tabs-list-498c2
      (map
        (fn [tab] (LeftNavTab tab (= (:txt tab) active-nav-section)))
        crud-left-nav)]])

;;------------------------------------------------------------------------------
;; Header Bar
;;------------------------------------------------------------------------------

(defn- on-change-top-nav-search-txt [js-evt]
  (let [new-text (aget js-evt "currentTarget" "value")]
    (swap! app-state assoc-in [:header-bar :search-txt] new-text)))

(rum/defc HeaderBar < rum/static
  [{:keys [search-txt]}]
  [:header.hdr-84fbe
    [:div.global-search-b63a6
      (SVGIcon "global-search-icon-a61cb" "search")
      [:input.global-search-input-337ab
        {:on-change on-change-top-nav-search-txt
         :placeholder "Search..."
         :type "text"
         :value search-txt}]]
    [:div.pg-bar-65f2b
      [:div.pg-nav-collapse-24cf8
        [:div.nav-hamburger-809d2]
        [:div.nav-arrow-9ec1d]]
      [:h1.pg-title-9c6e5 "CTIA Entites"]]])

;;------------------------------------------------------------------------------
;; Describable Entities Fields
;;------------------------------------------------------------------------------

(defn- click-show-short-description-btn [app-path]
  (swap! app-state assoc-in (conj app-path :show-short-description?) true))

(rum/defc ShortDescriptionBtn < rum/static
  [app-path]
  [:button.outline-btn-med-1c619
    {:on-click (partial click-show-short-description-btn app-path)}
    "Add Short Description"])

;; TODO: need a "hide" link on the short description when it is showing
(rum/defc DescribableEntityInputs < rum/static
  [app-path {:keys [title description short_description show-short-description?]} entity-name]
  [:div
    [:div.chunk-e556a
      (InputLabel "Title" true)
      (TextInput (conj app-path :title) title (str "Untitled " entity-name))]
    [:div.chunk-e556a
      (InputLabel "Description" true)
      (TextareaInput (conj app-path :description) description
        (str "What is " (if (blank? title) "Untitled" title) " " entity-name "?"))]
    (if show-short-description?
      [:div.chunk-e556a
        (InputLabel "Short Description" false)
        (TextInput (conj app-path :short_description) short_description (str "Briefly describe this " entity-name))]
      [:div.chunk-e556a
        (ShortDescriptionBtn app-path)])])

;;------------------------------------------------------------------------------
;; Single Observable
;;------------------------------------------------------------------------------

(defn- change-observable-type [app-path js-evt]
  (let [new-value (aget js-evt "currentTarget" "value")]
    (swap! app-state assoc-in app-path new-value)))

(def observable-option-mixin
  {:key-fn identity})

(rum/defc ObservableOption < (merge rum/static observable-option-mixin)
  [type]
  [:option {:value type} type])

(def observable-options (sort observable-type-identifier))
(def default-observable-type (first observable-options))

(rum/defc ObservableTypeInput < rum/static
  [app-path type]
  [:select.select-d38d0
    {:on-change (partial change-observable-type app-path)
     :value (if type type default-observable-type)}
    (map ObservableOption observable-options)])

(def observable-placeholder-txt
  "IP, email, file hash, etc")

(rum/defc SingleObservableInput < rum/static
  [app-path {:keys [type value]}]
  [:div
    [:div.group-1b8a3
      (ObservableTypeInput (conj app-path :type) type)]
    ;; FIXME: temporary style hack
    [:div.group-fe67d {:style {:width "280px"}}
      (TextInput (conj app-path :value) value observable-placeholder-txt)]])

;;------------------------------------------------------------------------------
;; Multiple Observables
;;------------------------------------------------------------------------------

(defn- click-add-observable-btn [app-path type value _js-evt]
  (when-not (blank? value)
    (let [new-observable {:type (if type type default-observable-type)
                          :value value}]
      ;; TODO: make this one atomic operation
      (swap! app-state update-in (conj app-path :tokens) conj new-observable)
      (swap! app-state assoc-in (conj app-path :value) "")
      (swap! app-state assoc-in (conj app-path :type) default-observable-type))))

(defn- click-remove-observable-token [app-path token-idx js-evt]
  (swap! app-state update-in (conj app-path :tokens) vec-remove token-idx))

(rum/defc ObservableToken < rum/static
  [app-path idx {:keys [type value]}]
  [:div.token-0ec5d
    [:div.token-label-a2683
      [:span.label-e557a (str type ":")]
      [:span.value-a67b4 value]]
    [:div.remove-9f89b {:on-click (partial click-remove-observable-token app-path idx)}]])

(rum/defc ObservablesInput < rum/static
  [app-path {:keys [tokens type value]}]
  (let [value (if value value "")]
    [:div
      [:div.group-wrapper-92a48
        [:div.left-17ca2
          (ObservableTypeInput (conj app-path :type) type)]
        [:div.middle-dbe47
          (TextInput (conj app-path :value) value observable-placeholder-txt)]
        [:div.right-77f10
          [:button.outline-btn-med-1c619
            {:on-click (partial click-add-observable-btn app-path type value)}
            "Add Observable"]]]
      (when-not (empty? tokens)
        [:div.token-wrapper-eb049
          (map-indexed (partial ObservableToken app-path) tokens)])]))

;;------------------------------------------------------------------------------
;; Entity Form Page
;;------------------------------------------------------------------------------

(rum/defc EntityFormPage < rum/static
  [state left-nav-txt page-title body-component body-key]
  [:div.app-container-0e505
    (HeaderBar (:header-bar state))
    (LeftNavTabs left-nav-txt)
    [:div.page-content-area-ad20c
      [:div.full-panel-bbc33
        [:h2.panel-title-595ca page-title]
        (body-component (get state body-key))]]])

;;------------------------------------------------------------------------------
;; Judgement Form
;;------------------------------------------------------------------------------

(def initial-judgement-form-state
  {:observable
     {:type default-observable-type
      :value ""}
   :disposition 1
   :priority 95
   :confidence "Low"
   :severity 5
   :valid_time
     {:start-time ""
      :end-time ""
      :valid-to-present? true}
   :reason ""
   :reason_uri ""
   :indicators
     {:entities []
      :search-txt ""
      :url "data/fake-describable-entities.json"}})

;; NOTE: the Judgement Form goes "inside" the Indicator form as well as having
;; it's own page, so it needs to be an independent component

(rum/defc JudgementForm < rum/static
  [app-path state]
  (let [{:keys [observable
                disposition_name
                disposition
                priority
                confidence
                severity
                valid_time
                reason
                reason_uri
                indicators]} state]
    [:div
      ; c/base-entity-entries
      ;; TODO: does a Judgement need TLP? TLP is in base-entity-entries
      ; c/sourced-object-entries

      [:div.chunk-e556a
        (InputLabel "Observable" true)
        (SingleObservableInput (conj app-path :observable) observable)]
      [:div.chunk-e556a
        (InputLabel "Disposition" true)
        (DispositionButtons (conj app-path :disposition) disposition)]
      [:div.chunk-e556a
        (InputLabel "Priority" true)
        (NumberInput (conj app-path :priority) priority)]
      [:div.chunk-e556a
        (InputLabel "Confidence" true)
        (ConfidenceButtons (conj app-path :confidence) confidence)]
      [:div.chunk-e556a
        (InputLabel "Severity" true)
        (NumberInput (conj app-path :severity) severity)]
      (TimeRange (conj app-path :valid_time) valid_time)

      ;; Optional Entries
      [:div.chunk-e556a
        (InputLabel "Reason")
        (TextareaInput (conj app-path :reason) reason)]
      [:div.chunk-e556a
        (InputLabel "Reason URI")
        (TextInput (conj app-path :reason_uri) reason_uri)]
      [:div.chunk-e556a
        (InputLabel "Related Indicators")
        (ReferenceInput (conj app-path :indicators) indicators)]]))
