Working list of changes for 5.0
===================================

* Switch from Configuration to ServiceRegistry+Metadata for SessionFactory building
* `org.hibernate.hql.spi.MultiTableBulkIdStrategy#prepare` contract has been changed to account for Metadata
* (proposed) `org.hibernate.persister.spi.PersisterFactory` contract, specifically building CollectionPersisters)
  has been changed to account for Metadata
* extract `org.hibernate.engine.jdbc.env.spi.JdbcEnvironment` from `JdbcServices`; create
  `org.hibernate.engine.jdbc.env` package and moved a few contracts there.
* Introduction of `org.hibernate.boot.model.relational.ExportableProducer` which will effect any
 `org.hibernate.id.PersistentIdentifierGenerator` implementations
* Change to signature of `org.hibernate.id.Configurable` to accept `ServiceRegistry` rather than just `Dialect`
* Removed deprecated `org.hibernate.id.TableGenerator` id-generator
* Removed deprecated `org.hibernate.id.TableHiLoGenerator` (hilo) id-generator
* Deprecated `org.hibernate.id.SequenceGenerator` and its subclasses
* cfg.xml files are again fully parsed and integrated (events, security, etc)
* Removed the deprecated `org.hibernate.cfg.AnnotationConfiguration`
* `Integrator` contract
* `Configuration` is  no longer `Serializable`
* `org.hibernate.dialect.Dialect.getQuerySequencesString` expected to retrieve catalog, schema, and increment values as well
* properties loaded from cfg.xml through EMF did not previously prefix names with "hibernate." this is now made consistent.
* removed AuditConfiguration in preference for new `org.hibernate.envers.boot.internal.EnversService`
* changed AuditStrategy method parameters from (removed) AuditConfiguration to (new) EnversService
* Built-in `org.hibernate.type.descriptor.sql.SqlTypeDescriptor` implementations no longer auto-register themselves
    with `org.hibernate.type.descriptor.sql.SqlTypeDescriptorRegistry`.  Applications using custom SqlTypeDescriptor
    implementations extending the built-in ones and relying on that behavior should be updated to call
    `SqlTypeDescriptorRegistry#addDescriptor` themselves.
* The JDBC type for "big_integer" (org.hibernate.type.BigIntegerType) properties has changed from 
    java.sql.Types,NUMERIC to java.sql.Types.BIGINT.
* Moving `org.hibernate.hql.spi.MultiTableBulkIdStrategy` and friends to new `org.hibernate.hql.spi.id` package
    and sub-packages
* Changes to "property access" contracts, including 
* Valid `hibernate.cache.default_cache_concurrency_strategy` setting values are now defined via
    `org.hibernate.cache.spi.access.AccessType#getExternalName` rather than the `org.hibernate.cache.spi.access.AccessType`
    enum names; this is more consistent with other Hibernate settings
* For ids defined as UUID with generation, for some databases it is required to explicitly set the `@Column( length=16 )`
    in order to generate BINARY(16) so that comparisons properly work.
* For EnumType mappings defined in hbm.xml where the user wants name-mapping (`javax.persistence.EnumType#STRING`) 
    the configuration must explicitly state that using either the `useNamed` (true) setting or by specifying the `type`
    setting set to the value 12 (VARCHAR JDBC type code).
    

TODOs
=====
* Still need to go back and make all "persistent id generators" to properly implement ExportableProducer
* Add a setting to "consistently apply" naming strategies.  E.g. use the "join column" methods from hbm.xml binding.
* Along with this ^^ consistency setting, split the implicit naming strategy for join columns into multiple methods - one for each usage:
   * many-to-one
   * one-to-one
   * etc


Blog items
==========
* New bootstrapping API - better determinism, better integration
* Java 8 Support (though still compatible with Java 6).
* hibernate-spatial
* Ability to handle additional Java types for id attributes marked as `GenerationType#AUTO`.  Built-in support
    for Number and UUID.  Expandable via new `org.hibernate.boot.model.IdGeneratorStrategyInterpreter` extension
* Expanded support for AttributeConverters.
    * fully supported for non-`@Enumerated` enum values
    * applicable in conjunction with `@Nationalized` support
    * called to handle null values
    * settable in hbm.xml by using `type="converter:fully.qualified.AttributeConverterName"`
    * integrated with hibernate-envers
    * collection values, map keys
* scanning support for non-JPA usage
* naming strategy
* OSGi improvements, Karaf feature file published


Proposals for discussion
========================
* Currently there is a "post-binding" hook to allow validation of the bound model (PersistentClass,
Property, Value, etc).  However, the top-level entry points are currently the only possible place
(per contract) to throw exceptions when a validation fails".  I'd like to instead consider a pattern
where each level is asked to validate itself.  Given the current model, however, this is unfortunately
not a win-win situation.  `org.hibernate.boot.model.source.internal.hbm.ModelBinder#createManyToOneAttribute`
illustrates one such use case where this would be worthwhile, and also can illustrate how pushing the
validation (and exception throwing down) can be less than stellar given the current model.  In the process
of binding a many-to-one, we need to validate that any many-to-one that defines "delete-orphan" cascading
is a "logical one-to-one".  There are 2 ways a many-to-one can be marked as a "logical one-to-one"; first
is at the `<many-to-one/>` level; the other is through a singular `<column/>` that is marked as unique.
Occasionally the binding of the column(s) of a many-to-one need to be delayed until a second pass, which
means that sometimes we cannot perform this check immediately from the `#createManyToOneAttribute` method.
What would be ideal would be to check this after all binding is complete.  In current code, this could be
either an additional SecondPass or done in a `ManyToOne#isValid` override of `SimpleValue#isValid`.  The
`ManyToOne#isValid` approach illustrates the conundrum... In the `ManyToOne#isValid` call we know the real
reason the validation failed (non-unique many-to-one marked for orphan delete) but not the property name/path.
Simply returning false from `ManyToOne#isValid` would instead lead to a misleading exception message, which
would at least have the proper context to know the property name/path.
* Consider an additional "naming strategy contract" specifically for logical naming.  This would be non-pluggable, and
would be the thing that generates the names we use to cross-reference and locate tables, columns, etc.
