### 0.1.2

First release into griffon artifacts portal.

### 0.1.3

1.  Add `--force-overwrite' to generate-all command so user can overwrite existing files.
2.  Add `install-templates` command for customizing templates used by generate-all scaffolding.
3.  Fixes groovy.lang.MissingPropertyException: No such property: editor.background for generated view with JXDatePicker.

### 0.2

1.  Add dbUnit support in integration testing for creating consistent data for each test methods (generate-all will create a new integration test class and XML document to be filled with data).
2.  Add 'page' and 'pageSize' to configuration map for dynamic finders.
3.  Add 'newEntityManager()' method to force creating a new persistence context.
4.  Modification to MVC templates used by generate-all.
5.  Fixes generate-all --startup-group can't be executed.
6.  Fixes groovy.lang.MissingMethodException: No signature of method when using simple-jpa validation.
7.  Fixes bug in transaction propagation.

### 0.2.1

1.  Add new feature that will always wrap simple-jpa methods inside transaction when not called from controller (not in transaction), for example, when calling simple-jpa methods from view or model.
2.  Add dateTimePicker() node that support LocalDate, LocalTime, LocalDateTime, and DateTime.
3.  Minor improvement on scripts.
4.  generate-all command will create a comment in output file if found unsupported attribute's type.
5.  Fixes database synchronization bug by always clear persistence context when starting a transaction.
6.  Fixes create-simple-jpa will still connect to MySQL even when using --skip-database=true / --skipDatabase=true.

### 0.3

1.  Deprecated toXXX() used by binding converter; use the new numberTextField() node that will create a JFormattedTextField.
2.  New maskTextField() node that will create a JFormattedTextField with MaskFormatter.
3.  generate-all with --set-startup will automatically set startupGroups to the generated MVCGroup name.
4.  Improvements on default templates to make them more readable.
5.  remove() will try to get entity by id if it is not managed.
6.  Add a boolean parameter, resume, with default value true to beginTransaction().  Setting resume to false will force start a new transaction (no transaction propagation).
7.  Add new functions that can be used inside template renderer: numberFormat, percentFormat, currentFormat, titleCase.
8.  Add new softDelete(entity) method.
9.  Add columnClasses to eventTableModel() to specify class type for each column (used by renderers).
10.  generate-all now support one-to-many relationship.
11.  generate-all will generate one-to-one relationship as pair dialog (similiar to one-to-many child dialog).
12.  Improvements on tagChooser().
13.  Change HibernateTestCase to DbUnitTestCase that will retrieve JDBC connection based on current persistence.xml (user can choose which persistence unit to be used).

### 0.4

1.  @SimpleJpaTransaction can now be used in individual method & closure declaration.
2.  @SimplaJpaTransaction can be configured to use Policy.PROPAGATE, Policy.NO_PROPAGATE, or Policy.SKIP.
3.  @SimpleJpaTransaction(newSession=true) will automatically destroy previous EntityManager before executing the method/closure.
4.  Add `griffon.simplejpa.entityManager.lifespan` with default value `MANUAL`.  Change this to `TRANSACTION` to make simple-jpa create EntityManager per transaction.
5.  Fixes creating new entity manager when transaction was rollbacked.
6.  Fixes PropertyChangeListener duplication in validation.
7.  Extensible ErrorNotification and ErrorCleaner for customizing validation.

### 0.4.1
1. Fixes fail to prevent AST on a domain class when only compiling one class (partial compile).
1. Fixes can't use another AbstractEventList besides BasicEventList in eventTableModel() node.
1. Fixes boolean rendered as textfield not checkbox.
1. Fixes bug in transaction holder that causes EntityManager didn't closed properly.
1. Fixes @SimpleJpaTransaction(newSession=true) was not working properly.
1. Fixes bug in validation logs.
1. Fixes EntityManager operations can't be used after an Exception is thrown or transaction rollback.
1. Updates integration test templates.
1. Add 'tableColumnConfig' node to configure table columns such as resizeable and maxWidth without actually creating a new TableColumnModel.
1. Scaffolding feature will generate @Enumerated property as a JComboBox that uses SwingX's EnumComboBoxModel.
1. Expose all EntityManager's methods as its name (ex: 'refresh(args)' will call 'refresh' method of current EntityManager).
1. Template rendere now can use closure as its expression.

### 0.4.2
1. Fixes can't use custom renderer in tableColumnConfig().
1. Fixes "can't commit on null object" exception.
1. Fixes findModelById didn't work when outside transaction.
1. Fixes "session is closed" when using named query.
1. New mvcPopupButton() node for popup that will display view from another MVCGroup.
1. New 'linkRenderer' for tableColumnConfig() node for creating a column in JTable that execute an action when clicked.
1. Add griffon.simplejpa.entityManager.checkThreadSafeLoading configuration key for checking entities lazy loading from thread other than the one that associated with it.
1. Add griffon.simplejpa.entityManager.defaultFlushMode configuration key for changing EntityManager's flush mode globally.
1. Add griffon.simplejpa.validation.convertEmptyStringToNull configuration key for automatically converting an empty String into null value when performing validation for a JPA entity.
1. Add 'flushMode' config for executing individual query.
1. Add 'excludeSubclass' config for excluding subclass instance from query result.
1. Minor scaffolding template improvements.
1. Support grouping in validation (pass grouping class as second argument).
1. findModelByDsl's closure parameter has been moved to the end (as the last parameter).
1. Discard parameters that are not specified in JP QL when executing named query.
1. Add methods for getting EMF properties, database user, password, etc. to SimpleJpaUtil.

### 0.5
1. Upgrade to Griffon 1.4
1. Read EntityManagerFactory properties from simplejpa.properties or from Griffon's Config.groovy.
1. Fixes mvcPopupButton() not working in Griffon 1.4.
1. Add descriptive information about plugin scripts.  Use `-info` argument such as `griffon generate-all -info` to display help information for a plugin script.
1. simple-jpa will trigger Griffon's events such as 'simpleJpaNewTransaction', 'simpleJpaCommitTransaction', etc.
1. simple-jpa will always precompile all classes in packages which name is 'ast'. This behaviour can be disabled by adding '-Dsimplejpa.precompileAST=false' when building application.
1. Add 'contentDecorator' attribute which takes a closure to MVCPopupButton node.
1. Add Swing builder 'glazedXXX()' node for generating JTable with GlazedLists.
1. Add 'simple-jpa-console' script command for executing simple-jpa methods interactively.
1. findModelByDsl() or findByDsl(Model) will return an entity or null while findAllModelByDsl() or findAllByDsl(Model) will return a list of entities.
1. findModelBy() is replaced by findModelByAnd() or findByAnd(Model), findAllModelByAnd() or findAllByAnd(Model), findModelByOr() or findByOr(Model), findAllModelByOr() or findAllByOr(Model).
1. findModelByAttribute() will return an entity or null while findAllModelByAttribute() will return a list of entities.
1. findModelByAttribute() now supports more than one attributes and operators such as findStudentByNameLikeAndAgeLt('%steve%', 20).
1. doNamedQueryOnModel() is replaced by executeNamedQuery().
1. Refactor JPA provider and JDBC driver out from simple-jpa so that simple-jpa only contains APIs.  create-simple-jpa command will add dependency to JPA provider and JDBC driver for the project.
1. Default value for 'griffon.simplejpa.entityManager.lifespan' is set to TRANSACTION.
1. Rename @DomainModel to @DomainClass and add attributes to excludes injected fields.
1. Rename @SimpleJpaTransaction to @Transaction.

### 0.5.1
1. Fixes can't run create-simple-jpa.

### 0.5.2
1. New nested attributes support in findByDsl() by separating attributes using double underscores(__), for example: item__category__name eq('CAT1').
1. Fixes create-simple-jpa failed with error 'java.sql.SQLException: No suitable driver found'.
1. Add '-jdbc=derby-embedded' to create-simple-jpa command.

### 0.5.3
1. Allow user to specify default content decorator for `mvcPopupButton()` in `DialogUtils`.
1. `executeQuery()` now supports named parameter.

### 0.6
1. Update to Griffon 1.5, JPA 2.1 and Bean Validation API 1.1.0.
1. Add `generate-schema` command.
1. Add `fecthGraph` and `configGraph` config to select a named entity graphs when executing finders.
1. Using `@Transaction` for domain classes will also inject simple-jpa methods (finders, add, remove, etc) to them.  This can be used to implement repositories and domain services (based on domain driven design).
1. Fixes integration testing error if specified MVCGroup doesn't exists.
1. Allow executing `validate()` outside MVCGroup (not tied to current MVCGroup's model).
1. Code generated by `@Transaction` now re-throw original exception instead of wrapping it.
1. Add error cleaner for `JCheckBox`.

### 0.7
1. Add `findAllModelFetch[Graph]()`, `findModelByDslFetch[Graph]()`, and `findModelByXXXFetch[Graph]()`.
1. Add `withTransaction`.
1. Add `@AutoMerge` that will merge current entity when their method is called (inserted as last statement).  This will work only if entity is detached (their id is not null but the entity is not managed by current entity manager).
1. Add `isIn` operation (to represent query like `status in ('A', 'B', 'C')`) to DSL.
1. Add `enterKeyAction` and `doubleClickAction` to `glazedTable()`.
1. Add bindable `visible` property to `glazedColumn()`.
1. Domain classes now can be in any packages.
1. Fixes `@Transaction` to restrictive: allow transformation on getter and setter.
1. Finders by default will remove duplicate entities.  Use `allowDuplicate: true` config or global `griffon.simplejpa.finders.alwaysAllowDuplicate` config to allow duplicate entities in the result.
1. Fixes incosistencies when Exception was thrown while committing transaction.
1. Add shortcut attribute `exp` that accepts both String and Closure for `templateRenderer`.
1. Change `notSoftDeleted` config to `excludeDeleted`.
1. `showMVCGroup()` will return value of `onFinish` closure or `null` if not defined.
1. Add `decimalTextField()` node which is a synonym for `numberTextField()` that bind to a `BigDecimal`.
1. Add `args` to `onBeforeDisplay` in `mvcPopupButton()` to allow changing args before creating new MVCGroup.
1. Allow `model__attribute` syntax in `orderBy`.
1. Remove obsoletes methods in `SimpleJpaUtil`.
1. Add `obfuscate` command and support `obfucasted:` string in JPA configurations.
1. Add `isNotRowSelected` to `GlazedTable`.
1. Remove dependency to Reflections and use `simple-jpa-annotated.txt` file to register annotated domain classes.

### 0.8
1. Improvements in default scaffolding's templates.
1. Allow nested property in finders.
1. Allow user to specify custom generator for scaffolding by using `generate-all -generator=package.custom.generator`.
1. Allow user to add scaffolding configurations in `Config.groovy`.
1. Add `griffon.simplejpa.scaffolding.auto` to automatically run scaffolding before compiling classes.
1. Add DDD scaffolding generator that can be selected by using `generate-all -generator=simplejpa.scaffolding.generator.ddd.DDDGenerator`.
1. Add new artifact type: repository.
1. Add `create-repository` command.
1. Automatically add `createdBy` and `modifiedBy` to domain classes.
1. Add optional login dialog that will be displayed at startup time.
1. Fixes nested property path for validation wasn't parsed properly.
1. Fixes `TagChooser` hover not working properly.
1. Changes `DialogUtils` methods signature.
1. Increases performance of `DbUnitTestCase` by caching `IDataSet`.
1. Changes `DbUnitTestCase` to perform insert operation only.  This can be changed by overriding `cleanDataSet()` and `insertDataSet()`.
1. Changes `DbUnitTestCase` to execute `clean.sql` before inserting records from dataset if it is exists.
1. Changes `DbUnitTestCase` to execute `before.sql` before `setUpDatabase()` and `after.sql` after `setUpDatabase()` operation.
1. Split setup method in `DbUnitTestCase` to `loadMVC()` and `setUpDatabase()`.
1. Add `confirm()` and `message()` to `DialogUtils` that will display message dialogs from EDT thread.
1. Don't execute String in templateRenderer node using SimpleTemplateEngine but treats it as property/function lookup (use closure for more complex expression).
1. Add default popup menu for `glazedTable()` to copy a cell value and print table's content.
1. Allow using `menuItem()` inside `glazedTable()` to define menu items for popup menu.
1. Add `exp` as synonym for `expression` in `glazedColumn()`.