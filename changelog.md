# Hibernate ORM Changelog

## 8.0.0.Beta2 (September 23, 2026)

[Full changelog](https://hibernate.atlassian.net/projects/HHH/versions/39688)


### Bug
* [HHH-20896](https://hibernate.atlassian.net/browse/HHH-20896) - AdditionalMappingContributor with contributor name defined may not apply the XML mappings
* [HHH-20895](https://hibernate.atlassian.net/browse/HHH-20895) - Parameter.getParameterType() returns null for declared Criteria types and query parameters
* [HHH-20894](https://hibernate.atlassian.net/browse/HHH-20894) - StatelessSession upsert and upsertMultiple concurrency problem
* [HHH-20882](https://hibernate.atlassian.net/browse/HHH-20882) - Redact credentials from database connection info logging
* [HHH-20880](https://hibernate.atlassian.net/browse/HHH-20880) - Implement version locking for StatelessSession and EntityAgent
* [HHH-20879](https://hibernate.atlassian.net/browse/HHH-20879) - Correct exception conversion and transaction rollback handling for locking failures
* [HHH-20878](https://hibernate.atlassian.net/browse/HHH-20878) - Correct lock upgrades for managed entities returned by queries
* [HHH-20877](https://hibernate.atlassian.net/browse/HHH-20877) - Prevent read and write skew with LockModeType.OPTIMISTIC
* [HHH-20876](https://hibernate.atlassian.net/browse/HHH-20876) - StatelessSession: Interceptor#onLoad isn't called
* [HHH-20863](https://hibernate.atlassian.net/browse/HHH-20863) - ActionQueue.clear() does not clear transactionCompletionCallbacks on rollback, causing HHH90010101
* [HHH-20861](https://hibernate.atlassian.net/browse/HHH-20861) - HHH100503 logged at INFO for normal constraint violation scenarios
* [HHH-20860](https://hibernate.atlassian.net/browse/HHH-20860) - USE_SERVER_TRANSACTION_TIMESTAMPS=true doesn't work without TX
* [HHH-20857](https://hibernate.atlassian.net/browse/HHH-20857) - Bug with @Audited.Excluded and TABLE_PER_CLASS inheritance
* [HHH-20855](https://hibernate.atlassian.net/browse/HHH-20855) - SQL Server temporal rounding causing trouble with sub-micro input values
* [HHH-20849](https://hibernate.atlassian.net/browse/HHH-20849) - org.hibernate.query.range.Range#suffix wrongly expects pattern
* [HHH-20827](https://hibernate.atlassian.net/browse/HHH-20827) - Lazy @ManyToOne reference behaves eagerly when targeting an abstract @ConcreteProxy entity with bytecode enhancement disabled
* [HHH-20826](https://hibernate.atlassian.net/browse/HHH-20826) - Emit MERGE for on conflict do nothing on H2, Oracle, and SQL Server instead of silently dropping the conflict clause
* [HHH-20825](https://hibernate.atlassian.net/browse/HHH-20825) - Cache package-info resolution during metadata bootstrap
* [HHH-20822](https://hibernate.atlassian.net/browse/HHH-20822) - Follow-on pessimistic locking fails when an optional join-table row is absent
* [HHH-20816](https://hibernate.atlassian.net/browse/HHH-20816) - Ensure arguments to JSON functions don't allow SQL injection
* [HHH-20813](https://hibernate.atlassian.net/browse/HHH-20813) - Entity-level <sql-select> / <hql-select> were defined in the XSD but never processed
* [HHH-20806](https://hibernate.atlassian.net/browse/HHH-20806) - Join elimination in 7.x skips @SQLRestriction when querying by to-one association id
* [HHH-20805](https://hibernate.atlassian.net/browse/HHH-20805) - MySQL schema update fails, if foreignkey related index is unique
* [HHH-20804](https://hibernate.atlassian.net/browse/HHH-20804) - StatefulPersistenceContext.clear() does not release newEntityHolder
* [HHH-20801](https://hibernate.atlassian.net/browse/HHH-20801) - AnyType.guessEntityPersister uses the wrapped proxy instead of the unwrapped implementation in its fallback → UnknownEntityTypeException during flush logging
* [HHH-20800](https://hibernate.atlassian.net/browse/HHH-20800) -  HbmXmlTransformer emits invalid <transient> for inherited getters when a property-access entity extends an unmapped superclass
* [HHH-20795](https://hibernate.atlassian.net/browse/HHH-20795) -  <optimistic-locking> element in mapping XML is ignored (entity-level optimistic lock style not applied)
* [HHH-20791](https://hibernate.atlassian.net/browse/HHH-20791) - HbmXmlTransformer drops the <composite-id> generator
* [HHH-20788](https://hibernate.atlassian.net/browse/HHH-20788) - [Quarkus 3.20.5 / Hibernate ORM 6.6.40.Final] quarkus.otel.logs.enabled=true fails SessionFactory build for TABLE_PER_CLASS collections
* [HHH-20783](https://hibernate.atlassian.net/browse/HHH-20783) - hibernate.transactions{result="failure"} metric can transiently go negative due to non-atomic counter reads, breaking the whole Prometheus scrape on Micrometer 1.14+
* [HHH-20782](https://hibernate.atlassian.net/browse/HHH-20782) - @FilterJoinTable throws NPE when used with explicit HQL join
* [HHH-20779](https://hibernate.atlassian.net/browse/HHH-20779) - Envers: unwrap proxies in collection-change audit work units
* [HHH-20777](https://hibernate.atlassian.net/browse/HHH-20777) - HbmXmlTransformer fails to transform result set mappings with <return-scalar>
* [HHH-20776](https://hibernate.atlassian.net/browse/HHH-20776) - CriteriaBuilder.literal() mistypes an enum constant declared with a class body
* [HHH-20775](https://hibernate.atlassian.net/browse/HHH-20775) - hasHandwrittenMetamodel incorrectly skips Jakarta Data static metamodel generation on incremental builds
* [HHH-20774](https://hibernate.atlassian.net/browse/HHH-20774) - <dynamic-update> and <dynamic-insert> elements in XML mappings are silently ignored
* [HHH-20773](https://hibernate.atlassian.net/browse/HHH-20773) - @Basic(fetch=LAZY) field not served from 2LC for @Immutable entities with associations
* [HHH-20772](https://hibernate.atlassian.net/browse/HHH-20772) - ARRAY_AGG column in Jakarta Data query method cannot be mapped to result type property
* [HHH-20764](https://hibernate.atlassian.net/browse/HHH-20764) - Many-to-many order-by should fall back to join-table column names
* [HHH-20762](https://hibernate.atlassian.net/browse/HHH-20762) -  HbmXmlTransformer does not transfer order-by for <idbag> and <bag> collections
* [HHH-20752](https://hibernate.atlassian.net/browse/HHH-20752) -  XML <generic-generator> at entity-mappings level not resolved for collection-id generators
* [HHH-20750](https://hibernate.atlassian.net/browse/HHH-20750) - addInnerClass overwriting implementation with query metamodel for nested interfaces
* [HHH-20749](https://hibernate.atlassian.net/browse/HHH-20749) - HbmXmlTransformer: fix mapped-superclass generation for sibling entities with diverging mappings
* [HHH-20744](https://hibernate.atlassian.net/browse/HHH-20744) - UnknownTableReferenceException when querying the non-owning side of a one-to-one-mapping with a pessimistic lock mode
* [HHH-20743](https://hibernate.atlassian.net/browse/HHH-20743) - `hibernate-maven-plugin` `enhance` goal computes wrong class names when `fileSets` directory differs from `classesDirectory`
* [HHH-20726](https://hibernate.atlassian.net/browse/HHH-20726) - HbmXmlTransformer does not set discriminator column length when values exceed JPA default
* [HHH-20725](https://hibernate.atlassian.net/browse/HHH-20725) - EntityBinder, ToOneBinder and MapBinder do not read FK name from individual join column elements
* [HHH-20724](https://hibernate.atlassian.net/browse/HHH-20724) - HbmXmlTransformer loses secondary table key column name from nested <column> element
* [HHH-20723](https://hibernate.atlassian.net/browse/HHH-20723) - HbmXmlTransformer loses column name from nested <column> element in joined-subclass <key>
* [HHH-20720](https://hibernate.atlassian.net/browse/HHH-20720) - Hibernate processor: @Transactional annotation should be copied for all repositories, not just Jakarta Data
* [HHH-20718](https://hibernate.atlassian.net/browse/HHH-20718) - Invalid SQL when a to-one foreign key references an ancestor table in a JOINED hierarchy
* [HHH-20717](https://hibernate.atlassian.net/browse/HHH-20717) - HbmXmlTransformer does not set the required rename attribute for <import>
* [HHH-20716](https://hibernate.atlassian.net/browse/HHH-20716) - xml <hql-import> elements are parsed but never processed
* [HHH-20715](https://hibernate.atlassian.net/browse/HHH-20715) - HbmXmlTransformer does not generate mapped-superclass for entities with inherited members
* [HHH-20712](https://hibernate.atlassian.net/browse/HHH-20712) -  HbmXmlTransformer does not transfer on-delete="cascade" for many-to-one associations
* [HHH-20711](https://hibernate.atlassian.net/browse/HHH-20711) - HbmXmlTransformer does not support composite-id with id-class 
* [HHH-20709](https://hibernate.atlassian.net/browse/HHH-20709) - HbmXmlTransformer generates fetch-mode=JOIN for lazy collections causing eager initialization
* [HHH-20707](https://hibernate.atlassian.net/browse/HHH-20707) - Missing import for inner interface type in CDI accessor metamodel methods
* [HHH-20703](https://hibernate.atlassian.net/browse/HHH-20703) - HbmXmlTransformer does not resolve typedef alias for collection-type attribute
* [HHH-20699](https://hibernate.atlassian.net/browse/HHH-20699) - HbmXmlTransformer sets EAGER fetch on composite-id key-many-to-one instead of LAZY
* [HHH-20697](https://hibernate.atlassian.net/browse/HHH-20697) - HbmXmlTransformer generates spurious unique constraint on individual composite-id key-property column
* [HHH-20696](https://hibernate.atlassian.net/browse/HHH-20696) - Implicit name of list index columns not applied by MetadataBuilder
* [HHH-20694](https://hibernate.atlassian.net/browse/HHH-20694) -  HbmXmlTransformer fails to resolve mapped-by for inverse one-to-many when FK maps to composite-id key-property
* [HHH-20691](https://hibernate.atlassian.net/browse/HHH-20691) - Subclass not-null check constraint is inverted for null discriminator value
* [HHH-20690](https://hibernate.atlassian.net/browse/HHH-20690) - HbmXmlTransformer generates transient for mapped property when property name case differs from JavaBean convention
* [HHH-20687](https://hibernate.atlassian.net/browse/HHH-20687) - HbmXmlTransformer does not generate primary-key-join-column for shared PK one-to-one
* [HHH-20686](https://hibernate.atlassian.net/browse/HHH-20686) - HbmXmlTransformer does not transfer join tables for discriminator subclasses
* [HHH-20684](https://hibernate.atlassian.net/browse/HHH-20684) - HbmXmlTransformer does not generate transient markers for unmapped fields on discriminator subclasses
* [HHH-20683](https://hibernate.atlassian.net/browse/HHH-20683) -  HbmXmlTransformer does not set name on id element for dynamic entities with unnamed id
* [HHH-20682](https://hibernate.atlassian.net/browse/HHH-20682) - HbmXmlTransformer drops nullable column for natural-id properties without explicit column element
* [HHH-20675](https://hibernate.atlassian.net/browse/HHH-20675) - @FilterDef(applyToLoadByKey = true) breaks JOIN FETCH of a JOINED-inheritance to-one association: subclass table joins dropped from FROM while their columns remain in SELECT (invalid SQL)
* [HHH-20674](https://hibernate.atlassian.net/browse/HHH-20674) - HbmXmlTransformer does not transfer callable attribute for custom SQL statements
* [HHH-20673](https://hibernate.atlassian.net/browse/HHH-20673) -  HbmXmlTransformer drops unique and not-null constraints for properties without explicit column
* [HHH-20668](https://hibernate.atlassian.net/browse/HHH-20668) - AdditionalMappingContributor xml bindings lose contributor name
* [HHH-20666](https://hibernate.atlassian.net/browse/HHH-20666) - HbmXmlTransformer generates invalid java-type and jdbc-type-code for converted properties
* [HHH-20665](https://hibernate.atlassian.net/browse/HHH-20665) - In loadJackson3Modules, the ObjectMapper check incorrectly compares against a Jackson 2 instead of a Jackson 3 mapper class
* [HHH-20661](https://hibernate.atlassian.net/browse/HHH-20661) - HQL UNION fails when unioning java.util.Date from different attribute paths
* [HHH-20659](https://hibernate.atlassian.net/browse/HHH-20659) - HbmXmlTransformer loses immutability for properties using `imm_` type aliases
* [HHH-20658](https://hibernate.atlassian.net/browse/HHH-20658) - Null elements in inverse indexed one-to-many collections are silently compacted on write since 6.2 — list positions no longer survive a round-trip
* [HHH-20656](https://hibernate.atlassian.net/browse/HHH-20656) - HbmXmlTranformer should generate mapped-by for constrained one-to-one inverse side
* [HHH-20655](https://hibernate.atlassian.net/browse/HHH-20655) - HbmXmlTransformer drops index attributes from properties and many-to-one associations
* [HHH-20650](https://hibernate.atlassian.net/browse/HHH-20650) - CTE names/columns and generated recursive search/cycle columns are not quoted when they are reserved words
* [HHH-20649](https://hibernate.atlassian.net/browse/HHH-20649) -  HbmXmlTransformer loses not-null, unique, length, precision and scale on element collection columns
* [HHH-20648](https://hibernate.atlassian.net/browse/HHH-20648) -  HbmXmlTransformer generates duplicate transient declarations for inherited properties on subclass entities
* [HHH-20647](https://hibernate.atlassian.net/browse/HHH-20647) - Binding an NClob attribute fails on PostgreSQL with "Could not convert 'java.sql.NClob' to 'java.sql.Clob'"
* [HHH-20645](https://hibernate.atlassian.net/browse/HHH-20645) - HbmXmlTransformer loses insert="false"/update="false" for component properties without explicit column
* [HHH-20644](https://hibernate.atlassian.net/browse/HHH-20644) - HbmXmlTransformer crashes with AssertionFailure when shared embeddable has formula property
* [HHH-20640](https://hibernate.atlassian.net/browse/HHH-20640) - HbmXmlTransformer does not generate mapped-by for inverse many-to-many collections
* [HHH-20639](https://hibernate.atlassian.net/browse/HHH-20639) - HbmXmlTransformer drops filters defined on the <many-to-many> element
* [HHH-20638](https://hibernate.atlassian.net/browse/HHH-20638) - HbmXmlTransformer does not resolve HBM type names in filter-def parameters
* [HHH-20635](https://hibernate.atlassian.net/browse/HHH-20635) - HbmXmlTransformer does not set the access type on composite-element embeddables
* [HHH-20634](https://hibernate.atlassian.net/browse/HHH-20634) - Property based id generator configuration no longer working for EmbeddedId
* [HHH-20633](https://hibernate.atlassian.net/browse/HHH-20633) - Unnecessary cast to SessionFactoryImplementor in AuditLogFactory
* [HHH-20632](https://hibernate.atlassian.net/browse/HHH-20632) - Regression: UnknownTableReferenceException fetching an @Any discriminator through treat() (since 7.4.0, HHH-16730)
* [HHH-20627](https://hibernate.atlassian.net/browse/HHH-20627) - HbmXmlTransformer does not generate <attribute-override> when multiple components share the same embeddable class
* [HHH-20626](https://hibernate.atlassian.net/browse/HHH-20626) - HbmXmlTransformer does not generate <composite-user-type> for CompositeUserType components
* [HHH-20625](https://hibernate.atlassian.net/browse/HHH-20625) - Process composite-user-type XML registrations
* [HHH-20624](https://hibernate.atlassian.net/browse/HHH-20624) - StackOverflowError when creating entity manager with attribute converter hierarchy with generic parameter
* [HHH-20621](https://hibernate.atlassian.net/browse/HHH-20621) - NullPointerException in CacheEntityLoaderHelper.loadFromSessionCache with JOINED inheritance, lazy ManyToOne proxy to parent type, and join fetch on subtype query
* [HHH-20620](https://hibernate.atlassian.net/browse/HHH-20620) - The HbmXmlTransformer shoud add a map-key-type for User types and not for basic types

### Bug
* [HHH-20615](https://hibernate.atlassian.net/browse/HHH-20615) - The HbmXml transformer should set the correct access value for <embeddable>.
* [HHH-20614](https://hibernate.atlassian.net/browse/HHH-20614) - AnnotationBasedGenerator is not initialized when the interface is implemented by an (abstract) superclass instead of the concrete class (after upgrading to hibernate 7.3/7.4)
* [HHH-20606](https://hibernate.atlassian.net/browse/HHH-20606) - Single `@Id` from `@MappedSuperclass` is lost when an `@IdClass` entity shares the same superclass
* [HHH-20605](https://hibernate.atlassian.net/browse/HHH-20605) -  XML Mapping, property-ref on many-to-one is not propagated to the inverse one-to-many collection
* [HHH-20600](https://hibernate.atlassian.net/browse/HHH-20600) - HbmXmlTransformer does not generate <transient/> declarations for unmapped embeddable properties
* [HHH-20599](https://hibernate.atlassian.net/browse/HHH-20599) - HbmXmlTransformer sets wrong table attribute on composite-element columns causing secondary table error
* [HHH-20598](https://hibernate.atlassian.net/browse/HHH-20598) - HbmXmlTransformer converts sort="natural" as a comparator class name instead of <sort-natural/>
* [HHH-20596](https://hibernate.atlassian.net/browse/HHH-20596) - HbmXmlTransformer does not support key-many-to-one in non-aggregated composite-id
* [HHH-20593](https://hibernate.atlassian.net/browse/HHH-20593) -  HbmXmlTransformer does not convert one-to-one property-ref to mapped-by when the referenced property is a back-reference association
* [HHH-20591](https://hibernate.atlassian.net/browse/HHH-20591) - HbmXmlTransformer fails to resolve mapped-by for inverse one-to-many when back-reference is a key-many-to-one inside composite-id
* [HHH-20590](https://hibernate.atlassian.net/browse/HHH-20590) - HbmXmlTransformer does not transfer optimistic-lock attribute on collections
* [HHH-20588](https://hibernate.atlassian.net/browse/HHH-20588) - @Formula properties generate invalid SQL when used with paginated queries that include collection fetches
* [HHH-20587](https://hibernate.atlassian.net/browse/HHH-20587) - Log "Attempt to stop an already-stopped RegionFactory" is a warning but should just be debug
* [HHH-20586](https://hibernate.atlassian.net/browse/HHH-20586) - GlobalRegistrationsImpl is not extracting query hints correctly
* [HHH-20584](https://hibernate.atlassian.net/browse/HHH-20584) - HbmXmlTransformer does not generate transient mappings for unmapped entity properties
* [HHH-20583](https://hibernate.atlassian.net/browse/HHH-20583) - Spurious HHH90010101 / HHH90010108 warnings on every bulk operation under container-managed JTA (Hibernate 7.3 / WildFly)
* [HHH-20582](https://hibernate.atlassian.net/browse/HHH-20582) - Envers `ToOneRelationMetadataGenerator.checkMappedByAudited` fails when inverse `@OneToOne` mappedBy points to a property declared on a superclass `@Entity`
* [HHH-20581](https://hibernate.atlassian.net/browse/HHH-20581) - ORM XML reader does not process <table-expression> for subselect entity mappings
* [HHH-20580](https://hibernate.atlassian.net/browse/HHH-20580) -  HbmXmlTransformer should not add a table attribute for entities mapped to a <subselect>
* [HHH-20574](https://hibernate.atlassian.net/browse/HHH-20574) - ORM XML reader package-qualifies target-entity for dynamic entity associations
* [HHH-20573](https://hibernate.atlassian.net/browse/HHH-20573) - XML mapping, Exception in BasicValueBinder when processing collections on dynamic entities
* [HHH-20570](https://hibernate.atlassian.net/browse/HHH-20570) - missing option support with StatelessSession
* [HHH-20561](https://hibernate.atlassian.net/browse/HHH-20561) - NullPointerException in Query.setCacheRetrieveMode() and Query.setCacheStoreMode()
* [HHH-20556](https://hibernate.atlassian.net/browse/HHH-20556) - hibernate-processor fails if @OrderBy is used with constant from static metamodel
* [HHH-20550](https://hibernate.atlassian.net/browse/HHH-20550) - SqmSelectStatement.createCountQuery() doesn't copy CTE statements
* [HHH-20524](https://hibernate.atlassian.net/browse/HHH-20524) - Inline dirty checking + @DynamicUpdate: a dirty scalar sorting after a nested-embeddable path is omitted from the UPDATE (silent data loss)
* [HHH-20522](https://hibernate.atlassian.net/browse/HHH-20522) - Json array and object construction doesn't handle values of type UUID, timestamp, binary well
* [HHH-20515](https://hibernate.atlassian.net/browse/HHH-20515) - Adjust default for SessionCheckMode on Session.findMultiple()
* [HHH-20511](https://hibernate.atlassian.net/browse/HHH-20511) - Oracle JSON value handling of UUID broken
* [HHH-20509](https://hibernate.atlassian.net/browse/HHH-20509) - Hibernate Data Repositories generates a BasicRepository impl that can't 'saveAll' entities with null ids
* [HHH-20472](https://hibernate.atlassian.net/browse/HHH-20472) - Generated metamodel class missing `List` import
* [HHH-20467](https://hibernate.atlassian.net/browse/HHH-20467) - StackOverflow with Converters with unbounded Generics
* [HHH-20452](https://hibernate.atlassian.net/browse/HHH-20452) - @OneToMany mapping silently dropped when orm.xml contributes a partial overlay (entity-listeners only) to an entity whose @Id is inherited from a @MappedSuperclass
* [HHH-20438](https://hibernate.atlassian.net/browse/HHH-20438) - Basic-array body predicates fail with AssertionError from SqmMappingModelHelper.resolveSqmPath
* [HHH-20348](https://hibernate.atlassian.net/browse/HHH-20348) - `DataException` / `ClassCastException` when aggregating primitive
* [HHH-20343](https://hibernate.atlassian.net/browse/HHH-20343) - HTE (Bulk ID) temporary table ignores PhysicalNamingStrategy when using SequenceGenerator
* [HHH-20318](https://hibernate.atlassian.net/browse/HHH-20318) - Unable to update CLOB column
* [HHH-20223](https://hibernate.atlassian.net/browse/HHH-20223) - DB2 Json value handling of UUID, binary and timestamp with time zone broken
* [HHH-20146](https://hibernate.atlassian.net/browse/HHH-20146) - Cascade delete with bytecode enhancement throws transient exception
* [HHH-20043](https://hibernate.atlassian.net/browse/HHH-20043) - NodeBuilder#treat(Join, Class) wrongly assumes always singular joins
* [HHH-19930](https://hibernate.atlassian.net/browse/HHH-19930) - CascadeType on key-to-one attributes ignored when no id class is present
* [HHH-19569](https://hibernate.atlassian.net/browse/HHH-19569) - Hibernate 6 alias injection in native query problem when using Joined Inheritance
* [HHH-19566](https://hibernate.atlassian.net/browse/HHH-19566) - EntityFilterException not thrown (due to filter not applied)
* [HHH-19565](https://hibernate.atlassian.net/browse/HHH-19565) - @SQLRestriction leads to column being updated to null
* [HHH-19486](https://hibernate.atlassian.net/browse/HHH-19486) - SQLGrammarException when joining to subquery with Case expression
* [HHH-19485](https://hibernate.atlassian.net/browse/HHH-19485) - AssertionError when using Subquery with Case in Criteria API
* [HHH-19202](https://hibernate.atlassian.net/browse/HHH-19202) - array_intersects doesn't work with an array as a parameter
* [HHH-18911](https://hibernate.atlassian.net/browse/HHH-18911) - Usage of ConcreteProxy in lazy loaded ManyToOne reference
* [HHH-17020](https://hibernate.atlassian.net/browse/HHH-17020) - Can't use enum as string in join column when field is part of composite primary key
* [HHH-13347](https://hibernate.atlassian.net/browse/HHH-13347) - delimited-identifiers in orm.xml has no effect 
* [HHH-13010](https://hibernate.atlassian.net/browse/HHH-13010) - Metamodel Generator chooses wrong default access type when child entity in hierarchy hasn't own access type annotation
* [HHH-4451](https://hibernate.atlassian.net/browse/HHH-4451) - StatefulPersistenceContext.deserialize must re-inject field interceptors after reading the entities from the input stream

### Deprecation
* [HHH-20862](https://hibernate.atlassian.net/browse/HHH-20862) - Deprecate reflection optimizer and related property access APIs
* [HHH-20746](https://hibernate.atlassian.net/browse/HHH-20746) - Deprecate additional boot and mapping APIs removed in 9.0
* [HHH-20740](https://hibernate.atlassian.net/browse/HHH-20740) - Deprecate the ability to disable strict generator naming
* [HHH-20739](https://hibernate.atlassian.net/browse/HHH-20739) - Deprecate IdentifierGeneratorDefinition and friends
* [HHH-20738](https://hibernate.atlassian.net/browse/HHH-20738) - Deprecate GlobalRegistrations and friends
* [HHH-20734](https://hibernate.atlassian.net/browse/HHH-20734) - Deprecate SessionFactoryServiceRegistry, SessionFactoryServiceContributor and friends
* [HHH-20732](https://hibernate.atlassian.net/browse/HHH-20732) - Deprecate access to BootstrapContext on Integrator
* [HHH-20730](https://hibernate.atlassian.net/browse/HHH-20730) - Deprecate IdentifierGenerator extending ExportableProducer
* [HHH-20702](https://hibernate.atlassian.net/browse/HHH-20702) - Deprecate Component#setComponentClassName and Component#setDynamic
* [HHH-20700](https://hibernate.atlassian.net/browse/HHH-20700) - Deprecate broad arguments on UserTypeCreationContext
* [HHH-20678](https://hibernate.atlassian.net/browse/HHH-20678) - Deprecate methods on BootstrapContext
* [HHH-20676](https://hibernate.atlassian.net/browse/HHH-20676) - Deprecate MetadataSources, MetadataSourcesContributor , MetadataBuilder, MetadataBuilderInitializer, MetadataBuilderContributor, MetadataBuilderFactory
* [HHH-20672](https://hibernate.atlassian.net/browse/HHH-20672) - Deprecate MetadataBuildingOptions
* [HHH-20671](https://hibernate.atlassian.net/browse/HHH-20671) - Deprecate MetadataBuildingContext methods
* [HHH-20602](https://hibernate.atlassian.net/browse/HHH-20602) - Deprecate hibernate.mapping.default_list_semantics
* [HHH-20576](https://hibernate.atlassian.net/browse/HHH-20576) - deprecate @OptimisticLock

### Epic
* [HHH-19568](https://hibernate.atlassian.net/browse/HHH-19568) - summary of bugs related to the use @ManyToOne associations to entities with a @SQLRestriction

### Improvement
* [HHH-20909](https://hibernate.atlassian.net/browse/HHH-20909) - (Re)introduce client (extended) enhancement
* [HHH-20886](https://hibernate.atlassian.net/browse/HHH-20886) - Drop all OSGi metadata from artifacts
* [HHH-20883](https://hibernate.atlassian.net/browse/HHH-20883) - mutation SQL should consistently include the tenant id
* [HHH-20881](https://hibernate.atlassian.net/browse/HHH-20881) - Introduce TransactionConcurrency
* [HHH-20873](https://hibernate.atlassian.net/browse/HHH-20873) - Fix API->SPI violations on SessionFactory
* [HHH-20872](https://hibernate.atlassian.net/browse/HHH-20872) - Classify Interceptor as SPI
* [HHH-20868](https://hibernate.atlassian.net/browse/HHH-20868) - Move SQL AST and translators out of incubation
* [HHH-20867](https://hibernate.atlassian.net/browse/HHH-20867) - Implement support for JPA 4 distinction for class, package-descriptor and model-descriptor
* [HHH-20865](https://hibernate.atlassian.net/browse/HHH-20865) - Upgrade to hibernate-models 1.3
* [HHH-20856](https://hibernate.atlassian.net/browse/HHH-20856) - MySQLDialect#getCurrentTimestampSelection() should use now(6) for precision
* [HHH-20845](https://hibernate.atlassian.net/browse/HHH-20845) -  HbmXmlTransformer: Named queries inside <class> not prefixed with entity name 
* [HHH-20841](https://hibernate.atlassian.net/browse/HHH-20841) - Expose ClassDetailsRegistry and ModuleDetailsRegistry from Integrator.Context
* [HHH-20836](https://hibernate.atlassian.net/browse/HHH-20836) -  HbmXmlTransformer should transform the on-delete attribute from HBM <key> elements for <set>/<list>/<bag><key on-delete="cascade"/> 
* [HHH-20834](https://hibernate.atlassian.net/browse/HHH-20834) - HbmXmlTransformer: Map <key on-delete="..."> in <joined-subclass> to <on-delete> inside <entity>
* [HHH-20833](https://hibernate.atlassian.net/browse/HHH-20833) - Support @OnDelete annotation at the entity level in XML mappings
* [HHH-20831](https://hibernate.atlassian.net/browse/HHH-20831) - getLockMode() outside transactions
* [HHH-20829](https://hibernate.atlassian.net/browse/HHH-20829) - JPA requires IAE instead of QueryTypeMismatchException
* [HHH-20828](https://hibernate.atlassian.net/browse/HHH-20828) - @ExcludedFromVersioning and EntityAgent
* [HHH-20824](https://hibernate.atlassian.net/browse/HHH-20824) - EnumeratedValueConverter silently maps an unknown column value to null
* [HHH-20817](https://hibernate.atlassian.net/browse/HHH-20817) - HbmXmlTransformer translates callable <sql-query> to <named-native-query> instead of <named-stored-procedure-query>
* [HHH-20815](https://hibernate.atlassian.net/browse/HHH-20815) - Integrate Hibernate Accessor
* [HHH-20812](https://hibernate.atlassian.net/browse/HHH-20812) - Support <sql-select> / <hql-select> on collections in orm xml mapping 
* [HHH-20811](https://hibernate.atlassian.net/browse/HHH-20811) - HbmXmlTransformer, handle hbm polymorphism="explicit"
* [HHH-20808](https://hibernate.atlassian.net/browse/HHH-20808) - Support for bootstrap-safe and non-reusable beans in ManagedBeanRegistry
* [HHH-20792](https://hibernate.atlassian.net/browse/HHH-20792) - Flip the default for hibernate.type.java_time_use_direct_jdbc
* [HHH-20790](https://hibernate.atlassian.net/browse/HHH-20790) - Allow a generator to be declared on an <embedded-id> XML mapping
* [HHH-20767](https://hibernate.atlassian.net/browse/HHH-20767) - Improve cascade processing
* [HHH-20766](https://hibernate.atlassian.net/browse/HHH-20766) - Improve collection flush handling
* [HHH-20721](https://hibernate.atlassian.net/browse/HHH-20721) - JPQL IN-parameter binding throws/catches CoercionException on every execution
* [HHH-20719](https://hibernate.atlassian.net/browse/HHH-20719) - Allow contributors to define support for UUID-based id generation
* [HHH-20637](https://hibernate.atlassian.net/browse/HHH-20637) - HbmXmlTransformer does not transform <parent> declarations in component and composite-element mappings

### Improvement
* [HHH-20619](https://hibernate.atlassian.net/browse/HHH-20619) - Support SqlTypes constant for MapKeyJdbcType variant in XML mapping
* [HHH-20611](https://hibernate.atlassian.net/browse/HHH-20611) - Disallow quoted @Entity(name)
* [HHH-20610](https://hibernate.atlassian.net/browse/HHH-20610) - Default SessionCheckMode changed to ENABLED
* [HHH-20597](https://hibernate.atlassian.net/browse/HHH-20597) - Upgrade to hibernate-models 1.3
* [HHH-20595](https://hibernate.atlassian.net/browse/HHH-20595) - Make VersionJavaType not directly depend on SharedSessionContractImplementor
* [HHH-20572](https://hibernate.atlassian.net/browse/HHH-20572) - make EnabledFetchProfile an EntityManager.Option
* [HHH-20563](https://hibernate.atlassian.net/browse/HHH-20563) - missing covariant overrides on subtypes of CommonBuilder
* [HHH-20551](https://hibernate.atlassian.net/browse/HHH-20551) - Release JDBC resources after statement execution when no transaction is active
* [HHH-20538](https://hibernate.atlassian.net/browse/HHH-20538) - Improve AltibaseDialect compatibility with Hibernate ORM 8.0/7.4
* [HHH-20447](https://hibernate.atlassian.net/browse/HHH-20447) - Add since() and group() to @Incubating
* [HHH-20421](https://hibernate.atlassian.net/browse/HHH-20421) - NativeGenerator doesn't honor SequenceStyleGenerator's default increment_size
* [HHH-17990](https://hibernate.atlassian.net/browse/HHH-17990) - More advanced "proxy" classes (and tests) for session/statelessSession for use in other libraries
* [HHH-833](https://hibernate.atlassian.net/browse/HHH-833) - AbstractPersistentCollection should define a serialVersionUID

### New Feature
* [HHH-20893](https://hibernate.atlassian.net/browse/HHH-20893) - Add @DefaultListSemantics
* [HHH-20842](https://hibernate.atlassian.net/browse/HHH-20842) - Update Spanner emulator to 1.5.57 and enable supported features
* [HHH-20807](https://hibernate.atlassian.net/browse/HHH-20807) - AdjustableSettings API
* [HHH-20802](https://hibernate.atlassian.net/browse/HHH-20802) - Support annotations on module descriptors (module-info)
* [HHH-20751](https://hibernate.atlassian.net/browse/HHH-20751) - HbmXmlTransformer does not transform <idbag> collection-id elements
* [HHH-20748](https://hibernate.atlassian.net/browse/HHH-20748) - New @SPI annotation to clarify expectations around SPI providers
* [HHH-20692](https://hibernate.atlassian.net/browse/HHH-20692) - Copy jakarta.annotations.security annotations from Data Repository interfaces to implementations
* [HHH-20657](https://hibernate.atlassian.net/browse/HHH-20657) - Add <mutable> element to <basic> in mapping XSD
* [HHH-20651](https://hibernate.atlassian.net/browse/HHH-20651) - JP4 @PostCreate and @PreClose events
* [HHH-20636](https://hibernate.atlassian.net/browse/HHH-20636) - XSD add support for @Parent
* [HHH-20618](https://hibernate.atlassian.net/browse/HHH-20618) - XSD add support for @MapKeyJavaType, @MapKeyJdbcType, @MapKeyJdbcTypeCode
* [HHH-20571](https://hibernate.atlassian.net/browse/HHH-20571) - introduce EnabledFilter
* [HHH-20559](https://hibernate.atlassian.net/browse/HHH-20559) - query options
* [HHH-20558](https://hibernate.atlassian.net/browse/HHH-20558) - StatementBatchSize session option
* [HHH-20539](https://hibernate.atlassian.net/browse/HHH-20539) - subselect fetching as a FetchOption
* [HHH-20473](https://hibernate.atlassian.net/browse/HHH-20473) - package-level @EntityListeners
* [HHH-19555](https://hibernate.atlassian.net/browse/HHH-19555) - @SQLRestriction @JoinTable @ManyToOne
* [HHH-12016](https://hibernate.atlassian.net/browse/HHH-12016) - Support non-primary table columns in @SQLRestriction

### Proposal
* [HHH-20685](https://hibernate.atlassian.net/browse/HHH-20685) - Support Kotlin's covariant List in @Find repository parameters
* [HHH-20654](https://hibernate.atlassian.net/browse/HHH-20654) - Support for alternative nullability annotations in processor

### Remove Feature
* [HHH-20770](https://hibernate.atlassian.net/browse/HHH-20770) - Drop support for custom CascadeStyle and CascadeAction implementations
* [HHH-20769](https://hibernate.atlassian.net/browse/HHH-20769) - Drop LOCK cascading
* [HHH-20760](https://hibernate.atlassian.net/browse/HHH-20760) - Stop publishing relocation poms from `org.hibernate`

### Task
* [HHH-20851](https://hibernate.atlassian.net/browse/HHH-20851) - Upgrade to ant 1.10.18
* [HHH-20848](https://hibernate.atlassian.net/browse/HHH-20848) - Drop meaningless "provided" dependency to ant in hibernate-envers
* [HHH-20765](https://hibernate.atlassian.net/browse/HHH-20765) - Move cascade handling into a dedicated package 
* [HHH-20747](https://hibernate.atlassian.net/browse/HHH-20747) - Reorganize stuff needed for Dialect implementors to make them SPI
* [HHH-20736](https://hibernate.atlassian.net/browse/HHH-20736) - Tune the content of javadocs to reduce the size of files published to Maven Central
* [HHH-20653](https://hibernate.atlassian.net/browse/HHH-20653) - JPA4 BatchSize as renamed to BatchFetch
* [HHH-20642](https://hibernate.atlassian.net/browse/HHH-20642) - Handle renaming of Panache Next to Quarkus Data in hibernate-processor and add name SPI
* [HHH-20630](https://hibernate.atlassian.net/browse/HHH-20630) - Reverse engineering DTD is resolved over the network instead of from the classpath
* [HHH-20609](https://hibernate.atlassian.net/browse/HHH-20609) - Guard against connection leaks
* [HHH-20604](https://hibernate.atlassian.net/browse/HHH-20604) - Allow Hibernate Reactive to call NativeQueryImpl#resolveNonSelectQueryPlan
* [HHH-20594](https://hibernate.atlassian.net/browse/HHH-20594) - Allow Hibernate Reactive to ovveride a NonSelectQueryPlan

## 8.0.0.Beta1 (June 16, 2026)

[Full changelog](https://hibernate.atlassian.net/projects/HHH/versions/37640)


### Bug
* [HHH-20579](https://hibernate.atlassian.net/browse/HHH-20579) - Remove org.moditect.jfrunit dependency
* [HHH-20566](https://hibernate.atlassian.net/browse/HHH-20566) - HbmXmlTransformer uses wrong inheritance strategy for nested joined-subclass and union-subclass
* [HHH-20564](https://hibernate.atlassian.net/browse/HHH-20564) - HbmXmlTransformer does not transfer the generated attribute on basic properties
* [HHH-20557](https://hibernate.atlassian.net/browse/HHH-20557) - Dynamic XML mapping does not work with embeddable mappings
* [HHH-20544](https://hibernate.atlassian.net/browse/HHH-20544) - Remove proxy and polymorphism XML mapping attributes from entity mappings
* [HHH-20543](https://hibernate.atlassian.net/browse/HHH-20543) - The mutable="true" attribute of <natural-id> in xml mapping is not applied
* [HHH-20537](https://hibernate.atlassian.net/browse/HHH-20537) - XML embeddable mapping ignores access attribute when processing attributes
* [HHH-20536](https://hibernate.atlassian.net/browse/HHH-20536) - <read> and <write> element inside <attribute-override> are silently ignored in XML mappings
* [HHH-20518](https://hibernate.atlassian.net/browse/HHH-20518) - After updating from 7.3.6 to 7.4.0 hibernate enhance fails
* [HHH-20517](https://hibernate.atlassian.net/browse/HHH-20517) - FOR UPDATE ... SKIP LOCKED on Oracle 23+ fails if there's a lob column
* [HHH-20514](https://hibernate.atlassian.net/browse/HHH-20514) - XML mapping does not apply optimistic-lock attribute for <many-to-many> associations
* [HHH-20513](https://hibernate.atlassian.net/browse/HHH-20513) - XML entity mapping ignores `<mutable>false</mutable>` configuration
* [HHH-20504](https://hibernate.atlassian.net/browse/HHH-20504) - DataException (Parameter is not set) when updating only a collection of a versioned entity with with a @SQLUpdate
* [HHH-20502](https://hibernate.atlassian.net/browse/HHH-20502) - Validator-derived 'not null' constraints not propagated to history table
* [HHH-20491](https://hibernate.atlassian.net/browse/HHH-20491) - Persisting and then removing OneToOne entities in same flush throws StaleObjectStateException
* [HHH-20489](https://hibernate.atlassian.net/browse/HHH-20489) - ResultSetMapping/resultClass ignored in StoredProcedureQuery.getSingleResult(..)
* [HHH-20488](https://hibernate.atlassian.net/browse/HHH-20488) - Find by "IdClass" fails under JPA compliance mode enabled
* [HHH-20487](https://hibernate.atlassian.net/browse/HHH-20487) - setLockMode/Scope should throw IllegalStateException instead of UnsupportedOperationException
* [HHH-20484](https://hibernate.atlassian.net/browse/HHH-20484) - HbmXmlTransformer Generates Invalid <generic-generator> for <generator class="native"/>
* [HHH-20483](https://hibernate.atlassian.net/browse/HHH-20483) - HbmXmlTransformer removes quotes from table names
* [HHH-20481](https://hibernate.atlassian.net/browse/HHH-20481) - HbmXmlTransformer should normalize <map-key> type attribute values to Java type names during XML Conversion 
* [HHH-20477](https://hibernate.atlassian.net/browse/HHH-20477) - HbmXmlTransformer Ignores Cascade Attributes for <many-to-many> Mappings
* [HHH-20466](https://hibernate.atlassian.net/browse/HHH-20466) - Remove orphan-removal attribute for many-to-many xml mapping
* [HHH-20465](https://hibernate.atlassian.net/browse/HHH-20465) - Full Stateless Session mutation support for @Audited entities
* [HHH-20454](https://hibernate.atlassian.net/browse/HHH-20454) - HbmXmlTransformer generates an incorrect mapping for an HBM file containing a <many-to-one> association with a property-ref attribute.
* [HHH-20453](https://hibernate.atlassian.net/browse/HHH-20453) - Combining @Temporal entity mappings and a @Changelog leads to CCE
* [HHH-20451](https://hibernate.atlassian.net/browse/HHH-20451) - Hbm <map-key-many-to-many> is ignored during HbmXmlTransformer conversion
* [HHH-20434](https://hibernate.atlassian.net/browse/HHH-20434) - Selecting a MapAttribute should return map values and not the map itself
* [HHH-20425](https://hibernate.atlassian.net/browse/HHH-20425) - Identifier not detected while `@Id` is placed in `@MappedSuperClass` and combined with `@Access`
* [HHH-20357](https://hibernate.atlassian.net/browse/HHH-20357) - Component.sortProperties() reorders Envers originalId key, breaking joined audit-table FK/joins for JOINED inheritance
* [HHH-20324](https://hibernate.atlassian.net/browse/HHH-20324) - AnnotationException: overrides mapping specified using '@JoinColumnOrFormula' thrown when join column is overridden via XML mapping
* [HHH-20182](https://hibernate.atlassian.net/browse/HHH-20182) - OracleLegacyDialect try to deploy an unknown time type
* [HHH-20042](https://hibernate.atlassian.net/browse/HHH-20042) - unnecessary quoting of implicit column names when table name is quoted
* [HHH-19567](https://hibernate.atlassian.net/browse/HHH-19567) - EntityGraph.removeAttributeNode() should suppress EAGER fetching
* [HHH-9912](https://hibernate.atlassian.net/browse/HHH-9912) - ProcedureCall and multiple result-set mappings

### Deprecation
* [HHH-20578](https://hibernate.atlassian.net/browse/HHH-20578) - Deprecate SessionFactoryBuilder SPI
* [HHH-20406](https://hibernate.atlassian.net/browse/HHH-20406) - Deprecate hibernate-envers

### Improvement
* [HHH-20575](https://hibernate.atlassian.net/browse/HHH-20575) - Update HikariCP to 7.1.0
* [HHH-20554](https://hibernate.atlassian.net/browse/HHH-20554) - Upgrade to Jandex 3.6
* [HHH-20553](https://hibernate.atlassian.net/browse/HHH-20553) - Upgrade to hibernate-models 1.2
* [HHH-20548](https://hibernate.atlassian.net/browse/HHH-20548) - Update Micrometer to 1.17.0
* [HHH-20535](https://hibernate.atlassian.net/browse/HHH-20535) - Update Jackson 2 to 2.22.0
* [HHH-20534](https://hibernate.atlassian.net/browse/HHH-20534) - Update c3p0 to 0.14.1
* [HHH-20533](https://hibernate.atlassian.net/browse/HHH-20533) - Update Agroal to 3.2
* [HHH-20532](https://hibernate.atlassian.net/browse/HHH-20532) - Update EHCache to 3.12.0
* [HHH-20531](https://hibernate.atlassian.net/browse/HHH-20531) - Update Alitbase JDBC driver to 8.1.0.0.3
* [HHH-20528](https://hibernate.atlassian.net/browse/HHH-20528) - Upgrade Oracle Test Pilot Setup GitHub action to v1.0.24
* [HHH-20527](https://hibernate.atlassian.net/browse/HHH-20527) - Raise CUBRIDDialect minimum version to 10.2 and align capability flags
* [HHH-20508](https://hibernate.atlassian.net/browse/HHH-20508) - sort out all the Fetch[Mode|Style] enums
* [HHH-20503](https://hibernate.atlassian.net/browse/HHH-20503) - Support for JPA 4 @Fetch
* [HHH-20496](https://hibernate.atlassian.net/browse/HHH-20496) - Deprecate ValidationMode.DDL
* [HHH-20495](https://hibernate.atlassian.net/browse/HHH-20495) - Make sure we release resources (if needed) after stateless session operations
* [HHH-20493](https://hibernate.atlassian.net/browse/HHH-20493) - Complete the implementation of jakarta.persistence.Parameter
* [HHH-20492](https://hibernate.atlassian.net/browse/HHH-20492) - Closing ProcedureCall should not try resolving outputs 
* [HHH-20486](https://hibernate.atlassian.net/browse/HHH-20486) - Account for new validation group options introduced by Jakarta Persistence 4 and the change in their default values
* [HHH-20476](https://hibernate.atlassian.net/browse/HHH-20476) - EntityExistsException in JPA 4
* [HHH-20471](https://hibernate.atlassian.net/browse/HHH-20471) - IF [NOT] EXISTS DDL for 19.28+ versions of Oracle Database
* [HHH-20470](https://hibernate.atlassian.net/browse/HHH-20470) - Update Geolatte to 1.12
* [HHH-20469](https://hibernate.atlassian.net/browse/HHH-20469) - Update Agroal to 3.1.2
* [HHH-20464](https://hibernate.atlassian.net/browse/HHH-20464) - Support for custom EntityManager.CreationOption, EntityManager.Option, EntityAgent.CreationOption and EntityAgent.Option
* [HHH-20463](https://hibernate.atlassian.net/browse/HHH-20463) - Update Jackson 3 to 3.1.3
* [HHH-20462](https://hibernate.atlassian.net/browse/HHH-20462) - Update Jackson 2 to 2.21.3
* [HHH-20461](https://hibernate.atlassian.net/browse/HHH-20461) - Bugfix updates for various JDBC drivers
* [HHH-20460](https://hibernate.atlassian.net/browse/HHH-20460) - Update SQL Server JDBC driver to 13.4.0.jre11
* [HHH-20459](https://hibernate.atlassian.net/browse/HHH-20459) - Update Informix JDBC driver to 15.0.1.1
* [HHH-20458](https://hibernate.atlassian.net/browse/HHH-20458) - Update SAP HANA JDBC driver to 2.28.7
* [HHH-20457](https://hibernate.atlassian.net/browse/HHH-20457) - Update DB2 JDBC driver to 12.1.4.0
* [HHH-20456](https://hibernate.atlassian.net/browse/HHH-20456) - Update dom4j to 2.2.0
* [HHH-20455](https://hibernate.atlassian.net/browse/HHH-20455) - Update Log4j to 2.26.0
* [HHH-20450](https://hibernate.atlassian.net/browse/HHH-20450) - Branch Autonomous databases for Oracle Test Pilot
* [HHH-20449](https://hibernate.atlassian.net/browse/HHH-20449) - treat static queries as named queries
* [HHH-20445](https://hibernate.atlassian.net/browse/HHH-20445) - implement support for Jakarta Data 1.1
* [HHH-20442](https://hibernate.atlassian.net/browse/HHH-20442) - Add support for collection immutability in XML mapping
* [HHH-20437](https://hibernate.atlassian.net/browse/HHH-20437) - Review the list of Jakarta Validation constraints that should influence the DDL
* [HHH-20436](https://hibernate.atlassian.net/browse/HHH-20436) - Complete the implementation to support new lifecycle events (@PreMerge,@Pre/PostUpsert ...)
* [HHH-20435](https://hibernate.atlassian.net/browse/HHH-20435) - Sync the updates from Jakarta Persistence 4 XSDs to corresponding ORM extended equivalents
* [HHH-20431](https://hibernate.atlassian.net/browse/HHH-20431) - SchemaManagementAction in PersistenceConfiguration
* [HHH-20430](https://hibernate.atlassian.net/browse/HHH-20430) - JPA exception conversion for EntityAgent
* [HHH-20423](https://hibernate.atlassian.net/browse/HHH-20423) - Relax the strict JPQL compliance and make `select` optional
* [HHH-20420](https://hibernate.atlassian.net/browse/HHH-20420) - Upgrade Oracle JDBC driver to version 23.26.2.0.0 and Oracle Jackson OSON JDBC Extension to version 1.0.6
* [HHH-20415](https://hibernate.atlassian.net/browse/HHH-20415) - get rid of IntegralDataTypeHolder
* [HHH-20403](https://hibernate.atlassian.net/browse/HHH-20403) - Update Jandex to 3.5.3
* [HHH-20402](https://hibernate.atlassian.net/browse/HHH-20402) - Update JBoss Logging to 3.6.3
* [HHH-20401](https://hibernate.atlassian.net/browse/HHH-20401) - Update c3p0 to 0.13.0
* [HHH-20340](https://hibernate.atlassian.net/browse/HHH-20340) - Do not issue any statement when performing DDL and there is nothing to do
* [HHH-20295](https://hibernate.atlassian.net/browse/HHH-20295) - Add a true observer alternative to StatementInspector
* [HHH-20210](https://hibernate.atlassian.net/browse/HHH-20210) - Redesign ProcedureCall and Outputs
* [HHH-20141](https://hibernate.atlassian.net/browse/HHH-20141) - Add Session#getReference based on natural-id
* [HHH-20139](https://hibernate.atlassian.net/browse/HHH-20139) - Revamp scanning in lieu of JPA 4 changes
* [HHH-20135](https://hibernate.atlassian.net/browse/HHH-20135) - Implement support for <default-to-one-fetch-type/>
* [HHH-20134](https://hibernate.atlassian.net/browse/HHH-20134) - Implement support for @ExcludedFromVersioning
* [HHH-20097](https://hibernate.atlassian.net/browse/HHH-20097) - infer @Immutable for final fields of immutable type
* [HHH-20062](https://hibernate.atlassian.net/browse/HHH-20062) - `Class` overload for `jarFileUrl`
* [HHH-19951](https://hibernate.atlassian.net/browse/HHH-19951) - introduce DetachedObjectException
* [HHH-19509](https://hibernate.atlassian.net/browse/HHH-19509) - throw when session methods are invoked from a callback
* [HHH-19424](https://hibernate.atlassian.net/browse/HHH-19424) - Continue switching tests using hbm.xml to use mapping.xml
* [HHH-19417](https://hibernate.atlassian.net/browse/HHH-19417) - FetchProfile not properly overridden in mapping.xml
* [HHH-17922](https://hibernate.atlassian.net/browse/HHH-17922) - Redesign ActionQueue
* [HHH-12235](https://hibernate.atlassian.net/browse/HHH-12235) - Translate GenericJdbcException Oracle error code 08177

### New Feature
* [HHH-20526](https://hibernate.atlassian.net/browse/HHH-20526) - @Asynchronous repositories

### New Feature
* [HHH-20525](https://hibernate.atlassian.net/browse/HHH-20525) - Jakarta Data events for stateful repos
* [HHH-20516](https://hibernate.atlassian.net/browse/HHH-20516) - enforce discriminator-based multi-tenancy using native RLS
* [HHH-20512](https://hibernate.atlassian.net/browse/HHH-20512) - bytecode enhancer should unfinal entity classes and their methods
* [HHH-20510](https://hibernate.atlassian.net/browse/HHH-20510) - Bring back hibernate-ucp module
* [HHH-20505](https://hibernate.atlassian.net/browse/HHH-20505) - support CacheStoreMode, CacheRetrieveMode, BatchSize as FetchOptions
* [HHH-20490](https://hibernate.atlassian.net/browse/HHH-20490) - introduce MutationOrSelectionQuery
* [HHH-20475](https://hibernate.atlassian.net/browse/HHH-20475) - JPA4 converted parameters
* [HHH-20426](https://hibernate.atlassian.net/browse/HHH-20426) - @NamedNativeQuery result set mapping members
* [HHH-20389](https://hibernate.atlassian.net/browse/HHH-20389) - Flush-time (eventuality-based) bidirectional association management
* [HHH-20374](https://hibernate.atlassian.net/browse/HHH-20374) - Trigger entity listeners on changes to owned collections and owned associations
* [HHH-20372](https://hibernate.atlassian.net/browse/HHH-20372) - Support improvements to @Index and @XxxTable
* [HHH-20371](https://hibernate.atlassian.net/browse/HHH-20371) - Support for @EntityResult, @ColumnResult, and @ConstructorResult on a @StaticNativeQuery method
* [HHH-20370](https://hibernate.atlassian.net/browse/HHH-20370) - Support for @StaticQueryOptions
* [HHH-20369](https://hibernate.atlassian.net/browse/HHH-20369) - Allow entity listener to declare multiple callback methods of the same event type, for different entity types
* [HHH-20368](https://hibernate.atlassian.net/browse/HHH-20368) - Support for @Discoverable
* [HHH-20367](https://hibernate.atlassian.net/browse/HHH-20367) - Support for StatementOrTypedQuery
* [HHH-20366](https://hibernate.atlassian.net/browse/HHH-20366) - Support extended flush control
* [HHH-20364](https://hibernate.atlassian.net/browse/HHH-20364) - Support for creation of Criteria from JPQL strings
* [HHH-20363](https://hibernate.atlassian.net/browse/HHH-20363) - Support for expanded criteria and metamodel types
* [HHH-20362](https://hibernate.atlassian.net/browse/HHH-20362) - Support for @EntityListener
* [HHH-20342](https://hibernate.atlassian.net/browse/HHH-20342) - Implementation of "Safe Mode" for AI Assistant

### Proposal
* [HHH-20521](https://hibernate.atlassian.net/browse/HHH-20521) - EntityManager and EntityAgent injection in Jakarta Data
* [HHH-17558](https://hibernate.atlassian.net/browse/HHH-17558) - HQL queries returning detached collections

### Remove Feature
* [HHH-20568](https://hibernate.atlassian.net/browse/HHH-20568) - Make hibernate-micrometer dependency to Micrometer provided (compileOnly)
* [HHH-20545](https://hibernate.atlassian.net/browse/HHH-20545) - Remove remaining parts of the deprecated org.hibernate.transform package
* [HHH-20485](https://hibernate.atlassian.net/browse/HHH-20485) - remove @Cascade and CascadeType
* [HHH-20474](https://hibernate.atlassian.net/browse/HHH-20474) - clean up deprecated methods of Graph and RootGraph
* [HHH-20446](https://hibernate.atlassian.net/browse/HHH-20446) - Remove the requirement for setter/getter implementations to be Serializable
* [HHH-20429](https://hibernate.atlassian.net/browse/HHH-20429) - kill off FlushModeType
* [HHH-20413](https://hibernate.atlassian.net/browse/HHH-20413) - removal of many deprecated SPIs for H8
* [HHH-20412](https://hibernate.atlassian.net/browse/HHH-20412) - rework GenericGenerator
* [HHH-20136](https://hibernate.atlassian.net/browse/HHH-20136) - Remove Session.replicate() methods
* [HHH-20083](https://hibernate.atlassian.net/browse/HHH-20083) - Remove ExecuteUpdateResultCheckStyle and friends
* [HHH-18183](https://hibernate.atlassian.net/browse/HHH-18183) - Remove @Comment / @Comments

### Sub-task
* [HHH-20439](https://hibernate.atlassian.net/browse/HHH-20439) - enable ci for spanner

### Task
* [HHH-20482](https://hibernate.atlassian.net/browse/HHH-20482) - Avoid mutating any final field in tests
* [HHH-20375](https://hibernate.atlassian.net/browse/HHH-20375) - Prepare for JPA 4.0 M2 development
* [HHH-19990](https://hibernate.atlassian.net/browse/HHH-19990) - Move FindMultipleOptions enums as inner

## 8.0.0.Alpha1 (February 02, 2026)

[Full changelog](https://hibernate.atlassian.net/projects/HHH/versions/32326)


### Improvement
* [HHH-20129](https://hibernate.atlassian.net/browse/HHH-20129) - Move JPA callbacks to EntityPersister
* [HHH-20074](https://hibernate.atlassian.net/browse/HHH-20074) - Adapt Hibernate Query contracts to new JPA 4.0 structure

### Task
* [HHH-20028](https://hibernate.atlassian.net/browse/HHH-20028) - Update to Jakarta Persistence 4.0
