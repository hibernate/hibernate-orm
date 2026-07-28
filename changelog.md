# Hibernate ORM Changelog

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
