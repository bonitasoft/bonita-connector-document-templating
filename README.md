bonita-connector-document-templating
=======================
Insert document properties inside docx

How to design report
====================
##### Using Word (docx): 
* Insertion > QuickPart > Field...
* Select FusionField and use a template (see [Velocity templating language](http://velocity.apache.org/)) as **field name** (eg: ${name}, ${user.Name}...etc)
* Click OK

##### Using LibreOffice (odt): 
* Insert > Fields > More fields...
* Select UserField and use a template (see [Velocity templating language](http://velocity.apache.org/)) as **value** (eg: ${name}, ${user.Name}...etc)
* Choose Text format
* Click Insert
