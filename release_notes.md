### 0.1.2

First release into griffon artifacts portal.

### 0.1.3

1.  Add `--force-overwrite' to generate-all command so user can overwrite existing files.
2.  Add `install-templates` command for customizing templates used by generate-all scaffolding.
3.  Fixes groovy.lang.MissingPropertyException: No such property: editor.background for generated view with JXDatePicker.

### 0.1.4

1.  Fixes groovy.lang.MissingMethodException: No signature of method when using simple-jpa validation.
2.  Add 'page' and 'pageSize' to configuration map for dynamic finders.
3.  Add 'newEntityManager()' method to force creating a new persistence context.