(ns org.jetbrains.plugins.clojure.refactoring.introduce.introduce-variable
  (:use [org.jetbrains.plugins.clojure.refactoring.utils.refactoring-utils])
  (:use [org.jetbrains.plugins.clojure.utils.clojure-utils])
  (:use [org.jetbrains.plugins.clojure.utils.java-wrappers])
  (:use [org.jetbrains.plugins.clojure.name_suggester.name-suggester])
  (:import [com.intellij.refactoring RefactoringActionHandler]
   [com.intellij.refactoring.introduce.inplace OccurrencesChooser InplaceVariableIntroducer]
   [com.intellij.openapi.util Pass Computable TextRange]
   [com.intellij.openapi.command CommandProcessor]
   [com.intellij.openapi.project Project]
   [com.intellij.openapi.editor Editor Document SelectionModel CaretModel]
   [com.intellij.psi PsiFile PsiElement PsiDocumentManager PsiNamedElement]
   [com.intellij.openapi.application ApplicationManager Application]
   [org.jetbrains.plugins.clojure.psi.api ClVector ClList]
   [org.jetbrains.plugins.clojure.refactoring.utils.refactoring_utils Declaration]
   [org.jetbrains.plugins.clojure.psi.api.symbols ClSymbol]
   [java.util LinkedHashSet]
   [com.intellij.refactoring.introduce.inplace OccurrencesChooser$ReplaceChoice]
   [com.intellij.util.containers ContainerUtil]))

(def refactoring-name (bundle-message
                        "introduce.variable.title"))


(defn- file-ok?!
  [^Project project ^Editor editor ^PsiFile file]
  (let [clojure? (with-error!
                   is-clojure?
                   project
                   editor
                   (bundle-message
                     "must.be.clojure"))
        writable? (with-error!
                    is-writable?
                    project
                    editor
                    (bundle-message
                      "file.is.not.writable"))]
    (if-let [answer (and
                      (clojure? file)
                      (writable? file project))]
      answer
      false)))

(defn- can-introduce?
  [expression ^Project project ^Editor editor]
  true) ;todo

(defn- get-container-body
  [container]
  (if (is-let-form? container)
    (get-let-body
      container)
    container))

(defn- modify-psi-tree
  [^PsiElement container bindings project]
  (let [body (get-container-body container)
        created-container (create-let-form
                            project
                            bindings
                            body)]
    (.replace container created-container)))

(defn- introduce-runner!
  [expression ^PsiElement container occurences name bindings project file editor]
  (let [container-position (get-text-offset container)
        ranges (map
                 get-text-range
                 occurences)]
    (do
      (replace-occurences! ranges name editor)
      (some-> (find-element-by-offset file container-position)
        (modify-psi-tree bindings project)))))

(defn- refactor-write-action!
  [expression container occurences name bindings project file editor]
  (run-computable-write-action!
    (reify
      Computable
      (compute [this]
        (introduce-runner!
          expression
          container
          occurences
          name
          bindings
          project
          file
          editor)))))


(defn- run-inplace!
  [expression container occurences name suggestions bindings ^Project project ^Editor editor file]
  (letfn [(run-inplace-introducer!
            [^PsiNamedElement named-element]
            (let [introducer (proxy
                               [InplaceVariableIntroducer]
                               [named-element editor project refactoring-name (into-array PsiElement occurences) nil]
                               (checkLocalScope []
                                 (.getContainingFile named-element)))]
              (if (inplace-available? editor)
                (.performInplaceRefactoring
                  introducer
                  (new-linked-hash-set suggestions)))))
          (do-inplace-refactoring!
            [replace]
            (do
              (remove-selection editor)
              (move-to-offset
                editor
                (some-> replace
                  (get-binding-symbol-by-name name)
                  get-text-offset))
              (commit-document editor)
              (do-postponed-operations-and-release-document editor)
              (some-> replace
                (get-binding-symbol-by-name name)
                run-inplace-introducer!)))
           (cmd
             []
             (-> (refactor-write-action! expression container occurences name bindings project file editor)
               do-inplace-refactoring!))]
    (execute-command!
      project
      cmd
      refactoring-name
      nil)))

(defn- get-container
  [expression]
  (let [ancestor (find-ancestor-by-name-set expression guards)
        parent (get-parent ancestor)
        grand-parent (get-parent parent)
        container? (fn
                     [element]
                     (if (instance? ClList element)
                       (contains?
                         containers
                         (get-list-name element))))]
    (if (and
          (instance? ClVector parent)
          (container? grand-parent))
      grand-parent
      (if (container? parent)
        parent
        ancestor))))


(defn- ^ClVector get-container-bindings
  [container expression name project]
  (let [declaration (Declaration. name expression)]
    (create-ClVector-from-Declarations
      (if (is-let-form?
            container)
        (conj
          (vec
            (get-Declarations-from-ClVector
              (get-let-bindings container)))
          declaration)
        (vector declaration))
      project)))

(defn- do-refactoring!
  [expression project editor file]
  (let [container (get-container
                    expression)
        names (get-var-names expression container)
        name (first names)
        occurences (doall
                     (get-occurences container expression))
        bindings (get-container-bindings container expression name project)
        callback (proxy
                   [Pass]
                   []
                   (pass [replace-choice]
                     (let [replaces (if (=
                                          OccurrencesChooser$ReplaceChoice/NO
                                          replace-choice)
                                      (vector expression)
                                      occurences)]
                       (run-inplace! expression container replaces name names bindings project editor file))))]
    (if (inplace-available? editor)
      (-> (OccurrencesChooser/simpleChooser editor)
        (.showChooser expression occurences callback))
      (->> OccurrencesChooser$ReplaceChoice/ALL
        (.pass callback)))))


(defn- invoke-on-expression!
  [project editor file start end]
  (if-let [expression (get-expression file start end)]
    (if (can-introduce? expression project editor)
      (do-refactoring! expression project editor file))
    (show-error project editor (bundle-message
                                 "cannot.refactor.not.form"))))

(defn- invoke-selection!
  [project editor file context start end]
  (do
    (commit-all-documents project)
    (if (file-ok?! project editor file)
      (invoke-on-expression! project editor file start end))))

(defn invoke!
  [project editor file context]
  (let [invokes (fn [e]
                  (let [start (get-selection-start editor)
                        end (get-selection-end editor)]
                    (invoke-selection! project e file context start end)))]
    (invoke-refactoring! project editor file context invokes)))


(defn introduce-variable-action-handler
  []
  (reify
    RefactoringActionHandler
    (invoke [this project editor file context]
      (invoke! project editor file context))))



