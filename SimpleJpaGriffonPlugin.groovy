/*
 * Copyright 2013 Jocki Hendry.
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

class SimpleJpaGriffonPlugin {
    // the plugin version
    String version = '0.3'
    // the version or versions of Griffon the plugin is designed for
    String griffonVersion = '1.2.0 > *'
    // the other plugins this plugin depends on
    // these plugin dependencies are for quick start, user may not need it!
    Map dependsOn = ['miglayout': '1.0.0', 'swingx-builder': '0.7']
    // resources that are included in plugin packaging
    List pluginIncludes = []
    // the plugin license
    String license = 'Apache Software License 2.0'
    // Toolkit compatibility. No value means compatible with all
    // Valid values are: swing, javafx, swt, pivot, qt
    List toolkits = []
    // Platform compatibility. No value means compatible with all
    // Valid values are:
    // linux, linux64, windows, windows64, macosx, macosx64, solaris
    List platforms = []
    // URL where documentation can be found
    String documentation = ''
    // URL where source can be found
    String source = 'https://github.com/JockiHendry/simple-jpa'
    // Map of Bnd directives and/or Manifest entries
    // see http://www.aqute.biz/Bnd/Bnd for reference
    Map manifest = [
            'Bundle-Description': 'Provides shortcuts for JPA operation'
    ]

    List authors = [
            [
                    name: 'Jocki Hendry',
                    email: 'jocki.hendry@gmail.com'
            ]
    ]
    String title = 'Provides an easy way to use JPA'
    // accepts Markdown syntax. See http://daringfireball.net/projects/markdown/ for details
    String description = '''
This plugin provides an easy way to use JPA in Griffon.

Scripts
-------

After installing simple-jpa, Griffon will have the following commands:

### create-simple-jpa

This is where everything begin.  Provided required information, this command will setup database in
your development environment (localhost).  To do this, you must tell simple-jpa what is the database name, user, and
password to be created and used by your application.  You will also need to provide root user password.
Root password will not be used in your application, but required to create and setup required database, user, and
privileges.

This command will create `persistence.xml` and `orm.xml` in `griffon-app/conf/metainf` folder. For your convenience,
this command will also generate `ValidationMessages.properties` in `griffon-app/i18n/ValidationMessages.properties`.
You can change this file contents if you want to customize Hibernate Validator messages.

Example:

    create-simple-jpa --user=steven --password=12345 --database=exercises --rootPassword=superadmin`

The command above will create user steven with password 12345.  It will also create exercises database and grant its
full privilleges to user steven. All required files to use JPA has been generated.

By the way, this version only support MySQL database!  If you didn't use MySQL, you will need to provide
required JDBC driver to run your application.  You will also setup your database manually, and you need to skip
database generation when executing this command:

    create-simple-jpa --user=steven --password=12345 --database=exercises --skip-database=true

The command above will not create database schema, but will still create `persistence.xml` with the required
database connection information.

### create-domain-class

This command will generate a domain class (JPA entity) skeleton and register this domain class in `persistence.xml`.
What you need to do is:

    create-domain-class Student Subject Score

Domain class will be generated in package `domain` by default.  To change this default package name,
you will need to add `griffon.simplejpa.model.package=newDomainPackage` in `Config.groovy`.

### generate-all

This command will create model, view, controller, then register a new Griffon MVCGroup with the same name as
domain class.  The generated MVCGroup will be able to perform CRUD (Create, Read, Update, and Delete) operations
for specified domain class.

Usage example:

    generate-all Student

By default, generate-all will not allow you to overwrite existing files.  But you can force simple-jpa to
overwrite existing files by adding --force-overwrite, for example, the following command
will always replace existing files:

    generate-all Student --force-overwrite

The command above will create `StudentModel`, `StudentView`, `StudentController` in package `project`.  It will also
create a new MVCGroup called `student`.  If you want to place the resulting class in a different package, you can
use `generatedPackage` parameter, for example:

    generate-all Student --generated-package=newPackage

To update startUp Group to the generated MVCGroup, use the following command:

    generate-all Student --set-startup

You will usually need to glue the MVCGroup together in one
main MVCGroup (that contains menu or toolbar to call available MVCGroup).  You can generate this kind of MVCGroup by
using the following command:

    generate-all --startup-group=StartupGroup

**Note**: First attribute found in domain model will be considered as natural primary key and will be used as
search key. You will need to make sure the first attribute defined in domain class is the natural primary key
(such as code, cardID, etc).

**Note**: If `griffon.simpleJpa.finder.alwaysExcludeSoftDeleted` in `Config.groovy` is true when calling this command,
the generated controller will always soft-delete (will not issue a real delete from database).

### install-templates

Files generated from scaffolding is based on a Groovy GString template.  This command will copy template files used
by generate-all scaffolding into your project in src/templates/artifacts folder.  generate-all command will then generate
files based on the templates in your project.

SimpleJpaDomainClass.groovy will be used by create-domain-class command.

SimpleJpaModel.groovy, SimpleJpaView.groovy, SimpleJpaController.groovy will be used by generate-all to generate model,
view, and controller for a domain class.

StartupModel.groovy, StartupView.groovy, StartupController.groovy will be used by generate-all to generate startup group.

Methods
-------

simple-jpa will automatically inject finders & JPA operation methods to Griffon's controller. You can use these methods
inside a controller.  Because these methods are public methods, you can also call them from view by referring
to the controller.

You can't have a method in controller with the same name as simple-jpa's methods.  If you think there will be conflict,
you can tell simple-jpa to add a prefix to all generated methods by adding the following line to `Config.groovy`:

    griffon.simplejpa.method.prefix = 'jpa' // this will generate methods such as 'jpaFindAllModel()', 'jpafindModelById(id)', etc.

By default, simple-jpa only inject methods to controller.  If you want these methods also got injected to another
Griffon's artifact type, such as service, you can add the following line to `Config.groovy`:

    griffon.simplejpa.injectTo = ['controller', 'service']

You must tell simple-jpa where to find domain class. By default, it will search in package `domain`, but you can
change this location by adding the following line to `Config.groovy`:

    griffon.simplejpa.model.package = 'new.package.with.models'

The following is list of simple-jpa's methods:

* **findAllModel()**
This method support optional configuration map.
Example: `findAllStudent()` will return List of all Student.

* **findModelById(id)**
Example: `findStudentById(1)` will return a Student with id = 1 or null if no id was found.
simple-jpa uses surrogate primary key where id is an auto increment number.
*Note*: if `griffon.simpleJpa.finder.alwaysExcludeSoftDeleted` is enabled in `Config.groovy`,
this method will check for `deleted` flag.  It will return null if record was found but it had been soft-deleted.

* **findModelByAttribute(value)**
This method support optional configuration map.
Example: `findStudentByName('steven')` will return List of all Student with name equals to steven.

* **findModelByAttribute(operator,value)**
This method support optional configuration map.
Supported operators are: *and*, *or*, *not*, *equal*, *notEqual*, *greaterThan*, *gt*, *greaterThanOrEqualTo*, *ge*,
*lessThan*, *lt*, *lessThanOrEqualTo*, *le*, *between*, *isNull*, *isNotNull*, *exists*, *like*, *notLike*, and *in*.
Example: `findStudentByName('like', '%steven%')` will return List of all Student with name containing steven.

* **findModelBy([attribute: value])**
This method support optional configuration map.
Example: `findStudent([name: 'steven', age: 27])` will return List of all Student with name equals to steven and age
equals to 27.

* **findModelByDsl(closure)**
This method support optional configuration map.
*Note*:  DSL Query should be line separated properly as seen in example.
Example:

    findStudentByDsl {
      name like ('%steven%')
      or()
      age gt(27)
    }

* **doNamedQueryOnModel(mapNamedParameter)**
To use this method, you must defined an JP QL in JPA entities by using `@NamedQuery` annotation.  For example:

    @NamedQuery(name="Student.FindSmartStudent", query="SELECT s.gpa FROM Student s WHERE s.gpa    > :smartLimit")
    class Student {
      // ...
    }

Name for JPA named query must be in format `Model.Name`.  You can then execute the named query in simple-jpa by using
the following statement:

    doFindSmartStudentOnStudent(['smartLimit':3.7])

* **executeQuery(JPQL)**
Example: `executeQuery('SELECT s FROM Student s')` will return a List of zero or more Students.

* **executeNativeQuery(SQL)**
Example: `executeNativeQuery('SELECT * FROM student_table')` will execute native SQL query and return a List of zero
or more Students.

* **persist(model)**
Example: `persist(student)` will persist the student in current persistence context.

* **merge(model)**
Example: `merge(student)` will return a new merged student and leave the passed student intact. This is default JPA behavior.

* **remove(model)**
Example: `remove(student)` will remove a student from persistence commit (delete it when transaction is committed).

* **getEntityManager()**
Example: `getEntityManager()` will return current EntityManager.

* **newEntityManager()**
Example: `newEntityManager()` will discard current EntityManager and create a new one.  This will sync EntityManager with database contents.

Finder methods that support configuration map can receive an Map as last argument.  The content of this Map can be any of:

* **page** (start from 1) and **pageSize**
For example: `findAllStudent(['page': 1, 'pageSize': 3]) will return first three students and `findAllStudent(['page': 2, 'pageSize': 3)` will return the next three students.
* **orderBy**
For example: `findAllStudent(['orderBy': 'name'])` will return all Student ordered by name in ascending direction.
* **orderDirection**
For example: `findAllStudent(['orderBy': 'name', 'orderDirection': 'desc'])` will return all Student ordered by name in descending direction.
* **notSoftDeleted**
For example: `findAllStudent(['notSoftDeleted': true])` will return all Student that was not soft deleted.
An object will be considered as not soft deleted if their `deleted` attribute is set to 'N'.  If `deleted` attribute is
set to other value (such as 'Y'), then the object will be considered as 'soft'-deleted.
*Note*: If `griffon.simpleJpa.finder.alwaysExcludeSoftDeleted` in `Config.groovy` is set to true,
`notSoftDeleted` will be assumed to be true for all finder methods that support configuration map.

Transaction
-----------

To use simple-jpa transaction management, you must disable auto commit in your `persistence.xml`:

    <property name="hibernate.connection.autocommit" value="false" />

You will also need to add `@SimpleJpaTransaction` annotation to controller class.  This will ensure
all methods (or closures) in controller are wrapped inside transaction.

simple-jpa will propagate transaction: if method A is calling method B and method C, all of these
method will be executed in one single transaction.  If one method issues a rollback, for example method C,
then all database operation from method A and method B will be cancelled.

Throwing any `Exception` will rollback transaction.  simple-jpa will also rollback and issue `onJpaError`
if JPA related error occured.  Here is an example for JPA error handling:

    onJpaError = { exception ->
       println "JPA operation erorr!"
    }

If you want to return and rollback without throwing Exception, you can call `return_failed()` manually.

simple-jpa will inject these transaction methods: `beginTransaction()`, `commitTransaction()` and `rollbackTransaction()`
to control JPA transaction manually.  But it is not recommended to call them if
you have added `@SimpleJpaTransaction` to controller.  Calling `beginTransaction(false)` will always start a new transaction,
while calling `beginTransaction()` or `beginTransaction(true)` will resume previous transaction.

Domain Model
------------

You should use `@DomainModel` annotation in your domain class to add the following attribute automatically:

* @Id @GeneratedValue(generator="uuid") @GenericGenerator(name="uuid", strategy="uuid2") String **id*** - *this is an autoincrement surrogate primary key*.
* @Type(type="org.jadira.usertype.dateandtime.joda.PersistentLocalDateTime") LocalDateTime **createdDate** - *this is for auditing*.
* @Type(type="org.jadira.usertype.dateandtime.joda.PersistentLocalDateTime") LocalDateTime **modifiedDate** - *this is for auditing*.
* String **deleted** = 'N' - *this is for soft-delete.*

Soft Delete
-----------

By default, attribute `deleted` with value 'N' means the object is not 'soft'-deleted.  Otherwise, it is 'soft'-deleted.

Soft delete is an operation to mark an object as deleted without really removing it physically from database.
For example, this will soft-delete a Student with id 3:

    softDeleteStudent(3)

Or, you can soft delete an entity:

    Student s = findStudentByName("steve")[0]
    softDelete(s)

You can use `notSoftDeleted` key in configuration map to tell simple-jpa finders to return only not soft-'deleted'
records (and will not return soft-deleted records).

You can also use `findModelByIdNotSoftDeleted(id)` if your query is based on id.  For example:

    findStudentByIdNotSoftDeleted(3)

Will return null if Student with id 3 is soft-deleted.

If you want finders to automatically exclude soft deleted object without explicitly specified by using `notSoftDeleted` key,
you can add the following line to `Config.groovy`:

    griffon.simpleJpa.finder.alwaysExcludeSoftDeleted = true

The configuration above will cause finder methods to ignore soft-deleted objects.  Note that by using this configuration,
there will be no way to select soft-deleted objects using finders, but you can still use custom JP QL or native SQL
to select all objects including soft-deleted objects.

Auditing
--------

simple-jpa currently support a very primitive auditing feature.  If you didn't using `create-simple-jpa` command, then you
will need to add the following content to `orm.xml` to enable simple-jpa auditing:

    <?xml version="1.0" encoding="UTF-8"?>
    <entity-mappings version="2.0" xmlns="http://java.sun.com/xml/ns/persistence/orm"
     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
     xsi:schemaLocation="http://java.sun.com/xml/ns/persistence/orm orm_2_0.xsd">
     <persistence-unit-metadata>
         <persistence-unit-defaults>
             <entity-listeners>
                 <entity-listener class="simplejpa.AuditingEntityListener" />
             </entity-listeners>
         </persistence-unit-defaults>
     </persistence-unit-metadata>
    </entity-mappings>

simple-jpa will automatically fill `createdDate` and `modifiedDate` for each object.

User Interface Validation & Converter
-------------------------------------

simple-jpa will automatically add an ObservableMap with name `errors` in all of model classes. The key for this map
represent an error path, and its value is an error message that will be displayed in view.

To indicate an error has happened to a field, you simply add new entry to `model.errors` with path name as key and
error message as value.  For example:

    model.errors['age'] = 'Your age is not valid!'

To remove error message, just set a null value or empty string for the corresponding path.  For example:

    model.errors['age'] = ''  // or
    model.errors.remove('age')

To display error messages, you need at least one errorLabel() node for an error path.  For example:

    panel {
        textField(id: 'name')
        errorLabel(path: 'name')
        textField(id: 'age')
        errorLabel(path: 'age')
    }

`errorLabel()` node will create a new JLabel that display only error message for that path.

You can also enable background highlighting on error for JComponent, by adding 'errorPath' attribute to your node.
For example, this will change JTextField background color if there is an error message on path 'name':

    textField(id: 'name', errorPath: 'name')

You can check if there is an error by calling `hasError()` method of model.  Example:

    def save = {
        if (model.hasError()) return
        // process normally
        // ...
    }

You can change default error message in `griffon-app/i18n/ValidationMessages.properties`.

Domain Model Validation
-----------------------

simple-jpa use Bean Validation API (JSR-303) with Hibernate Validator implementation for domain model validation.
To validate a domain class, you use `validate()` method inside a controller:

    validate(object)

It will return value `true` if no violations found or `false` if otherwise.  This method will add error messages
to `model.errors` if any of constraint violation is found.

If `model.errors` already contains errors before calling this method, then `validate()` will always return `false`
even when Hibernate Validator didn't found any violations.  You must clear `model.errors` manually.

Swing
-----

simple-jpa will add some new nodes to Swing builder to support template renderer. This features will allow you
easily present (render) domain model in JTable, JComboBox, or JList.  By default, they will render a domain model by
converting it to a String (calling `toString()` method).  This is not always desirable.  Inside a template renderer,
you can use numberFormat(), currencyFormat(), percentFormat() or titleCase(), for example:

   columnValues: ['${titleCase(value.productName)}', '${currencyFormat(value.price)}']

`evenTableModel()` can be used for a TableModel in `table()` node.  This node will use Glazed Lists. Usage example:

    table(rowSelectionAllowed: true, id: 'table') {
        eventTableModel(list: model.carList, columnNames: ["Name", "Type", "Year"],
            columnValues: ['${value.name}', '${value.type?:"Unknown"}', 'Year ${value.year}'])
    }

    table(rowSelectionAllowed: true, id: 'table') {
        eventTableModel(list: model.carList,
            columnNames: ["Name", "Type", "Year"],
            columnValues: ['${value.name}', '${value.type?:"Unknown"}', 'Year ${value.year}'],
            columnClasses: [String, String, Integer])
    }

`columnValues` attributes control how domain model will be displayed in each column.
The domain model will be binded as `value` inside the template. You can also use Groovy code inside the template by
using `<% ... %>`.

`templateRenderer()` will create a template renderer that can be used by JComboBox or JList.  For example:

    comboBox(model: model.selectedCar, renderer: templateRenderer(template: '${value?.type} - ${value?.name}'))

The example above will render domain object `Car(type: 'type', name: 'name')` as 'type - name'.
When user select this JComboBox, the selected value will still be the a Car object.

`dateTimePicker()` will create a simple date time picker.  This component supports LocalDate, LocalTime, LocalDateTime, and DateTime.  Usage example:

    dateTimePicker(id: 'birthDate', localDate: bind('birthDate', target: model, mutual: true), dateVisible: true, timeVisible: false)
    dateTimePicker(id: 'endTime', localDateTime: bind('endTime', target: model, mutual: true))
    dateTimePicker(id: 'expiredDate', dateTime: bind('expiredDate', target: model, mutual: true))

`tagChooser()` will create a simple many-to-many chooser.  This is an example of a form that use `tagChooser()`:

    panel(id: "form", layout: new MigLayout('', '[right][left][left,grow]',''), constraints: PAGE_END, focusCycleRoot: true) {
        label('Name:')
        textField(id: 'name', columns: 20, text: bind('name', target: model, mutual: true), errorPath: 'name')
        errorLabel(path: 'name', constraints: 'wrap')
        label('List Class Room:')
        tagChooser(model: model.classRoomList, templateString: '${value.name}', constraints: 'grow,push,span,wrap', errorPath: 'listClassRoom')
        errorLabel(path: 'listClassRoom', constraints: 'skip 1,grow,span,wrap')
    }

    // In the model, define a TagChooserModel like this:
    //    TagChooserModel classRoomList = new TagChooserModel()
    // To set choices / items in combobox:
    //    model.classRoomList.replaceValues(findAllClassRoom())
    // To get selected values:
    //    model.classRoomList.selectedValues

`numberTextField()` will generate a JFormattedTextField and bind its `value` property to current model.  Example:

    // This will create a JFormattedTextField, bind its 'value' to 'model.income' (specified in 'bindTo' attribute)
    // The created JFormattextTextField will use NumberFormat.getCurrencyInstance() formatter
    numberTextField(id: 'income', columns: 20, bindTo: 'income', type: 'currency')

Available types are: 'currency', 'percent', 'integer'.  If `type` is not specified, default number format will be used.

You can further customize NumberFormat used by `numberTextField()` by using 'nfXXX' attribute where 'XXX' is an attribute of NumberFormat.  Example:

    numberTextField(id: 'income', columns: 20, bindTo: 'income', type: 'currency',
        nfParseBigDecimal: true, nfMaximumFractionDigits: 5)

You can also use your own NumberFormat by passing it as 'decimalFormat' attribute's value.  Example:

    numberTextField(id: 'income', columns: 20, bindTo: 'income',
        decimalFormat: new DecimalFormat("#,###.00"), nfParseBigDecimal: true, nfMaximumFractionDigits: 5)

`maskTextField()` will generate a JFormattedTextField and bind its `value` property to current model.  The generated JFormattedTextField will use a MaskFormatter.  Example:

    maskTextField(id: 'phone', columns: 20, bindTo: 'phone', mask: '###-######')

You can further customize MaskFormatter used by `maskTextField()` by using 'mfXXX' attribute where 'XXX' is an attribute of MaskFormatter.  Example:

    maskTextField(id: 'phone', columns: 20, bindTo: 'phone', mask: '###-######', mfPlaceholderCharacter: '_')

You can also use your own MaskFormatter by passing it as 'maskFormatter' attribute's value:  Example:

    maskTextField(id: 'phone', columns: 20, bindTo: 'phone', maskFormatter: new MaskFormatter('###-######'))

Integration Testing
-------------------

simple-jpa uses dbUnit for integration testing to create consistent data for each test method.  generate-all command
will create class for integration test, with content like this:

    class StudentTest extends DbUnitTestCase {
       private static final Logger log = LoggerFactory.getLogger(StudentTest)

       protected void setUp() {
          super.setUp()
          setUpDatabase("student", "/project/data.xls")
       }

       protected void tearDown() {
          super.tearDown()
       }
    }

In the `setUp()` method, you must pass the correct parameters to `setUpDatabase()`: the name of MVCGroup that will be
tested and the location of the Excel file (this can also be an XML or CSV).  generate-all by default will generate
this Excel file in the correct location, so you will only need to find and open this Excel file.  Each sheet represent
a table, first row is for field names (each column represent a field) and the following rows are for data.  You can't
have a blank sheet.  You are not required to add all existing tables in the Excel file.

This is an example of a test method:

    public void testSave() {
        model.id = null
        model.name = "A5"
        model.location = "Class A5"
        controller.save()
        Thread.sleep(3000)

        List list = controller.findAllStudent()
        assertEquals(5, list.size())
        assertTrue(list.contains(new Student("A5", "Class A5")))
    }

You can use `view`, `model`, and `controller` variables to refer to view, model, and controller for the currently
being tested MVCGroup.  You can also create another MVCGroup by using `app` variable.


'''
}
