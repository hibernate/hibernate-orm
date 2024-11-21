
### <a name="jpa-32"></a> Jakarta Persistence 3.2

7.0 migrates to Jakarta Persistence 3.2 which can be fairly disruptive.
See the [Migration Guide](https://docs.jboss.org/hibernate/orm/7.0/migration-guide/migration-guide.html#jpa-32) for details.

See [this blog post](https://in.relation.to/2024/04/01/jakarta-persistence-3/) for a summary of the changes in 3.2

- [TCK Results](https://ci.hibernate.org/view/ORM/job/hibernate-orm-tck-3.2/job/wip%252F7.0/24/) with Java 17
- [TCK Results](https://ci.hibernate.org/view/ORM/job/hibernate-orm-tck-3.2/job/wip%252F7.0/25/) with Java 21

### <a name="java-17"></a> Java 17

Version 3.2 of Jakarta Persistence requires Java 17.  
Hibernate 7.0 therefore baselines on Java 17 whereas previous versions baseline on Java 11.

### <a name="model-validations"></a> Domain Model Validations

7.0 does much more validation of an application's domain model and especially its mapping details, e.g.

* illegal combinations such as `@Basic` and `@ManyToOne` on the same attribute
* misplaced annotations such as an annotated getter method with FIELD access
* stricter following of JavaBean conventions

See the [Migration Guide](https://docs.jboss.org/hibernate/orm/7.0/migration-guide/migration-guide.html#model-validation) for details.


### <a name="mapping-xml"></a> mapping.xsd

Hibernate 7.0 provides a new XSD that represents an "extension" of the Jakarta Persistence orm.xsd weaving in Hibernate-specific mapping features.  
The namespace for this extended mapping is `http://www.hibernate.org/xsd/orm/mapping`

For applications using Hibernate's legacy `hbm.xml` format, we provide a tool to help with the transformation.
See the [Migration Guide](https://docs.jboss.org/hibernate/orm/7.0/migration-guide/migration-guide.html#hbm-transform) for details.


### <a name="hibernate-models"></a> Hibernate Models

7.0 migrates from [Hibernate Commons Annotations](https://github.com/hibernate/hibernate-commons-annotations/) (HCANN) to the new [Hibernate Models](https://github.com/hibernate/hibernate-models) project for low-level processing of an application domain model, reading annotations and weaving in XML mapping documents.
See the [Migration Guide](https://docs.jboss.org/hibernate/orm/7.0/migration-guide/migration-guide.html#hibernate-models) for details.


### <a name="json-and-xml-functions"></a> JSON and XML functions

Support for most of the JSON and XML functions that the SQL standard specifies was added to HQL/Criteria.
The implementations retain the SQL standard semantics and will throw an error if emulation on a database is impossible.

New functions include:

* construction functions like `json_array()`, `json_object()`, `xmlelement()` and `xmlforest()`
* query functions like `json_value()`, `json_query()` and `xmlquery()`
* aggregation functions like `json_agg()`, `json_object_agg()` and `xmlagg()`
* manipulation functions like `json_set()`, `json_mergepatch()`
* any many more

> The functions are incubating/tech-preview - to use them in HQL it is necessary to enable the `hibernate.query.hql.json_functions_enabled` and `hibernate.query.hql.xml_functions_enabled` configuration settings.


### <a name="set-returning-functions"></a> Set-returning Functions

A set-returning function is a new type of function that can return rows and is exclusive to the `from` clause.
The concept is known in many different database SQL dialects and is sometimes referred to as table valued function or table function.

Custom set-returning functions can be registered via a `FunctionContributor`.
Out-of-the-box, some common set-returning functions are already supported or emulated

* `unnest()` - allows to turn an array into rows
* `generate_series()` - can be used to create a series of values as rows
* `json_table()` - turns a JSON document into rows
* `xmltable()` - turns an XML document into rows

### <a name="any-discriminator"></a> @AnyDiscriminatorImplicitValues

The new  `@AnyDiscriminatorImplicitValues` offers 2 related improvements for the mapping of discriminator values
for `@Any` and `ManyToAny` associations.

First, it allows control over how Hibernate determines the discriminator value to store in the database for
implicit discriminator mappings.  Historically, Hibernate would always use the full name of the associated
entity.

Second, it allows mixing of explicit and implicit value strategies.

See the [Migration Guide](https://docs.jboss.org/hibernate/orm/7.0/userguide/html_single/Hibernate_User_Guide.html#associations-any) for details.

### <a name=cleanup"></a> Clean-up

A lot of deprecated contracts and behavior has been removed.
See the [Migration Guide](https://docs.jboss.org/hibernate/orm/7.0/migration-guide/migration-guide.html#cleanup) for details.

