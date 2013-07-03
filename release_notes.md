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
1. New mvcPopupButton() node for popup that will display view from another MVCGroup.
1. New 'linkRenderer' for tableColumnConfig() node for creating a column in JTable that execute an action when clicked.
1. New check for entities lazy loading from thread other than the one that associated with it.
1. Minor scaffolding template modification to startup group's view.