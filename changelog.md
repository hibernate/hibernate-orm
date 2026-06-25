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

## 7.4.0.CR1 (May 07, 2026)

[Full changelog](https://hibernate.atlassian.net/projects/HHH/versions/37741)


### Bug
* [HHH-20358](https://hibernate.atlassian.net/browse/HHH-20358) - PRIVILEGED_CLI is not set to "sudo" when Podman is aliased as Docker
* [HHH-20356](https://hibernate.atlassian.net/browse/HHH-20356) - NPE when mapping embedded property as @MappedSuperclass
* [HHH-20355](https://hibernate.atlassian.net/browse/HHH-20355) - use of static calendar in TimestampUtcAsJdbcTimestampJdbcType and friends
* [HHH-20353](https://hibernate.atlassian.net/browse/HHH-20353) - unhelpful error with @ManyToAny
* [HHH-20350](https://hibernate.atlassian.net/browse/HHH-20350) - Unexpected physical naming of columns for embeddables in Hibernate 7 
* [HHH-20336](https://hibernate.atlassian.net/browse/HHH-20336) - JdbcResourceLocalTransactionCoordinatorImpl does not trigger afterCompletionCallback if it encounters a TransactionException
* [HHH-20326](https://hibernate.atlassian.net/browse/HHH-20326) - ClassCastException when joining array within embeddable
* [HHH-20323](https://hibernate.atlassian.net/browse/HHH-20323) - with `hibernate.temporal.use_server_transaction_timestamps`, session obtains a database timestamp even when not needed
* [HHH-20321](https://hibernate.atlassian.net/browse/HHH-20321) - import.sql not executed when there are no @Entity classes
* [HHH-20313](https://hibernate.atlassian.net/browse/HHH-20313) - Criteria query comparing double path to integer expression containing integer parameter fails
* [HHH-20308](https://hibernate.atlassian.net/browse/HHH-20308) - JSON embeddable array access not working because Column reports wrong SQL type code
* [HHH-20307](https://hibernate.atlassian.net/browse/HHH-20307) - Informix count distinct tuple emulation is broken
* [HHH-20305](https://hibernate.atlassian.net/browse/HHH-20305) - Lock timout on Informix broken
* [HHH-20303](https://hibernate.atlassian.net/browse/HHH-20303) - Power function usage for PostgreSQLTruncRoundFunction breaks CockroachDB
* [HHH-20300](https://hibernate.atlassian.net/browse/HHH-20300) - OptionalTableUpdateWithUpsertOperation can fail on PostgreSQL 14
* [HHH-20297](https://hibernate.atlassian.net/browse/HHH-20297) - SQM parameter use in multiple type-incompatible contexts binds with wrong JdbcMapping
* [HHH-20287](https://hibernate.atlassian.net/browse/HHH-20287) - DataException ( Parameter is not set) when updating only the version of an Entity with a PartitionKey
* [HHH-20283](https://hibernate.atlassian.net/browse/HHH-20283) - key-based pagination appears to be broken
* [HHH-20281](https://hibernate.atlassian.net/browse/HHH-20281) - Missing temporal precision for parameter coercion leading to ClassCastException
* [HHH-20274](https://hibernate.atlassian.net/browse/HHH-20274) - Avoid mutating SqmSelectClause during type validation
* [HHH-20273](https://hibernate.atlassian.net/browse/HHH-20273) - Failed to set List type field in Embeddable record
* [HHH-20272](https://hibernate.atlassian.net/browse/HHH-20272) - JDBC locking pre-actions are not executed before the statement
* [HHH-20271](https://hibernate.atlassian.net/browse/HHH-20271) - SybaseASE reports wrong lock metadata
* [HHH-20267](https://hibernate.atlassian.net/browse/HHH-20267) - Hibernate processor: infinite generation of repositories when extending PanacheRepository
* [HHH-20266](https://hibernate.atlassian.net/browse/HHH-20266) - Some unnesting array functions miss ordering on index on aggregation
* [HHH-20265](https://hibernate.atlassian.net/browse/HHH-20265) - StackOverflowError with multi-level generic inheritance hierarchy and attribute converters
* [HHH-20260](https://hibernate.atlassian.net/browse/HHH-20260) - Session#find only logs LockTimeoutException instead of throwing it on PostgreSQL
* [HHH-20259](https://hibernate.atlassian.net/browse/HHH-20259) - DdlTypeRegistry#addSqlType doesn't handle different type codes registered to same DDL type properly
* [HHH-20253](https://hibernate.atlassian.net/browse/HHH-20253) - ClassCastException when using hibernate-enhance-maven-plugin plugin
* [HHH-20251](https://hibernate.atlassian.net/browse/HHH-20251) - NPE: query with fetch graph and read-only hint on bytecode enhanced entities
* [HHH-20244](https://hibernate.atlassian.net/browse/HHH-20244) - Sybase and MySQL connection locking problems
* [HHH-20243](https://hibernate.atlassian.net/browse/HHH-20243) - float/double literals with no fractional part
* [HHH-20242](https://hibernate.atlassian.net/browse/HHH-20242) - `IndexOutOfBoundsException` while trying to pretty-print `AntlrError`
* [HHH-20241](https://hibernate.atlassian.net/browse/HHH-20241) - `@Query` with invalid HQL in Jakarta Data repository was able to compile
* [HHH-20238](https://hibernate.atlassian.net/browse/HHH-20238) - Data types are not forwarded from HQL to SQL properly
* [HHH-20234](https://hibernate.atlassian.net/browse/HHH-20234) - DatasourceConnectionProviderImpl extends DriverManagerConnectionProvider instead of DatasourceConnectionProvider
* [HHH-20233](https://hibernate.atlassian.net/browse/HHH-20233) - parse issues with query validation
* [HHH-20230](https://hibernate.atlassian.net/browse/HHH-20230) - [Metamodel Generator] AnnotationMetaEntity fails to compile repository methods with unbounded wildcard Sort<?>
* [HHH-20224](https://hibernate.atlassian.net/browse/HHH-20224) - Exception when calling treat() twice
* [HHH-20221](https://hibernate.atlassian.net/browse/HHH-20221) - Jakarta Data `@Find` method with `@OrderBy` that returns `TypedQuery` does not compile
* [HHH-20216](https://hibernate.atlassian.net/browse/HHH-20216) - The documentation task 'documentation:renderUserGuideHtml' is failing
* [HHH-20209](https://hibernate.atlassian.net/browse/HHH-20209) - Race Condition in JavaTypeRegistry causing SemanticException during parallel UNION queries with projection.
* [HHH-20206](https://hibernate.atlassian.net/browse/HHH-20206) - @Nullable @HQL method does fail to compile
* [HHH-20199](https://hibernate.atlassian.net/browse/HHH-20199) - Regression Hibernate 7: Using an AdditionalMappingContributor leads to a rescan of Entities that breaks when @Converters are present 
* [HHH-20194](https://hibernate.atlassian.net/browse/HHH-20194) - attribute validation broken in HQL validation for some queries
* [HHH-20191](https://hibernate.atlassian.net/browse/HHH-20191) - Docs for annotation processor are wrong and missing documented default values
* [HHH-20183](https://hibernate.atlassian.net/browse/HHH-20183) - Incorrect type for fields in generated metamodel classes, when field type is the same as Jakarta class
* [HHH-20181](https://hibernate.atlassian.net/browse/HHH-20181) - two minor issues with DDL constraints
* [HHH-20168](https://hibernate.atlassian.net/browse/HHH-20168) - Hibernate JsonWithArrayEmbeddableTest.testUpdateMultipleAggregateMembers test fail with Oracle 19c
* [HHH-20161](https://hibernate.atlassian.net/browse/HHH-20161) - Hibernate ArrayToStringWithArrayAggregateTest test fail with Oracle 19c
* [HHH-20155](https://hibernate.atlassian.net/browse/HHH-20155) - StatelessSession.upsert() and discriminator column
* [HHH-20154](https://hibernate.atlassian.net/browse/HHH-20154) - @NaturalId(modifiable=false) => updatable=false
* [HHH-20133](https://hibernate.atlassian.net/browse/HHH-20133) - @Formula and @JoinFormula with {p.*} expansion in native queries
* [HHH-20126](https://hibernate.atlassian.net/browse/HHH-20126) - NPE when querying with a lockMode/lockScope
* [HHH-20092](https://hibernate.atlassian.net/browse/HHH-20092) - SqlTypes.JSON on SQL Server expects varchar(max) even with use_nationalized_character_data=true
* [HHH-20053](https://hibernate.atlassian.net/browse/HHH-20053) - HQL grammar ambiguity for NOT keyword
* [HHH-20004](https://hibernate.atlassian.net/browse/HHH-20004) - stop @Column(columnDefinition) leaking out of DDL generation
* [HHH-19885](https://hibernate.atlassian.net/browse/HHH-19885) - Wrong mapping of legacy XML "access" attribute in HbmXmlTransformer
* [HHH-19818](https://hibernate.atlassian.net/browse/HHH-19818) - NPE with stateless insert when Envers is enabled
* [HHH-19609](https://hibernate.atlassian.net/browse/HHH-19609) - @PartitionKey and StatelessSession.update()
* [HHH-19607](https://hibernate.atlassian.net/browse/HHH-19607) - TableReference not found comparing embedded of associated entity in where clause of a query
* [HHH-19567](https://hibernate.atlassian.net/browse/HHH-19567) - EntityGraph.removeAttributeNode() should suppress EAGER fetching
* [HHH-19429](https://hibernate.atlassian.net/browse/HHH-19429) - ConcurrentModificationException observed while executing JPQL update query with VERSIONED clause
* [HHH-19380](https://hibernate.atlassian.net/browse/HHH-19380) - unhelpful exception when mixing nodes from different SQM trees
* [HHH-19355](https://hibernate.atlassian.net/browse/HHH-19355) - bug in HQL trunc() function on Oracle
* [HHH-19056](https://hibernate.atlassian.net/browse/HHH-19056) - MapsId with null embeddable fails
* [HHH-19020](https://hibernate.atlassian.net/browse/HHH-19020) - NullPointerException in ClassPropertyHolder.addPropertyToMappedSuperclass()
* [HHH-18774](https://hibernate.atlassian.net/browse/HHH-18774) - two bad bugs in cascade refresh
* [HHH-18742](https://hibernate.atlassian.net/browse/HHH-18742) - StatelessSession.upsert() and @NaturalId fields
* [HHH-18529](https://hibernate.atlassian.net/browse/HHH-18529) - two issues with AccessType defaulting
* [HHH-17786](https://hibernate.atlassian.net/browse/HHH-17786) - StatelessSession#upsert fails to insert updatable=false columns
* [HHH-16523](https://hibernate.atlassian.net/browse/HHH-16523) - hibernate.cache.use_minimal_puts appears to be missing from H6
* [HHH-16442](https://hibernate.atlassian.net/browse/HHH-16442) - trunc() function with offset datetimes on Oracle
* [HHH-16167](https://hibernate.atlassian.net/browse/HHH-16167) - generators and embeddable classes
* [HHH-12986](https://hibernate.atlassian.net/browse/HHH-12986) - ConfigLoader does not close file when loading hibernate.cfg.xml
* [HHH-12590](https://hibernate.atlassian.net/browse/HHH-12590) - Postgres subselect: ERROR: subquery in FROM must have an alias
* [HHH-9414](https://hibernate.atlassian.net/browse/HHH-9414) - MarkerObject reference checking is wrong after deserialization

### Deprecation
* [HHH-20377](https://hibernate.atlassian.net/browse/HHH-20377) - Deprecate org.hibernate.query.QueryFlushMode

### Improvement
* [HHH-20404](https://hibernate.atlassian.net/browse/HHH-20404) - Processor should ignore package private entities in other packages
* [HHH-20398](https://hibernate.atlassian.net/browse/HHH-20398) - a couple of minor improvements to audited stuff
* [HHH-20296](https://hibernate.atlassian.net/browse/HHH-20296) - Speed up some HQL tests when only SELECT are performed
* [HHH-20290](https://hibernate.atlassian.net/browse/HHH-20290) - Improve OTP job durations in CI
* [HHH-20282](https://hibernate.atlassian.net/browse/HHH-20282) - @Parent annotation should not depend on existence of getter/setter
* [HHH-20261](https://hibernate.atlassian.net/browse/HHH-20261) - Actionable, detailed error message when projecting a native query to an incorrectly-structured custom class/record
* [HHH-20256](https://hibernate.atlassian.net/browse/HHH-20256) - Make ByteBuddy class generation build-reproducible
* [HHH-20246](https://hibernate.atlassian.net/browse/HHH-20246) - Upgrade Oracle JDBC driver to version 23.26.1.0.0
* [HHH-20240](https://hibernate.atlassian.net/browse/HHH-20240) - Annotation processor provides too less information to find HQL problems
* [HHH-20228](https://hibernate.atlassian.net/browse/HHH-20228) - Make the sortedTypeContributors comparator static
* [HHH-20227](https://hibernate.atlassian.net/browse/HHH-20227) - Avoid using reflection and parsing annotations when initializing the Hibernate and JPA annotation descriptors
* [HHH-20215](https://hibernate.atlassian.net/browse/HHH-20215) - Improve token lookahead performance in SQL Template rendering
* [HHH-20214](https://hibernate.atlassian.net/browse/HHH-20214) - Integrate Envers readers on top of @Temporal
* [HHH-20213](https://hibernate.atlassian.net/browse/HHH-20213) - Update to GeoLatte 1.11
* [HHH-20196](https://hibernate.atlassian.net/browse/HHH-20196) - Refactor to improve performance, remove redundancy and improve readability
* [HHH-20177](https://hibernate.atlassian.net/browse/HHH-20177) - Update log4j to 2.25.3
* [HHH-20157](https://hibernate.atlassian.net/browse/HHH-20157) - Add pre-relocation capabilities to relocated artifacts
* [HHH-20153](https://hibernate.atlassian.net/browse/HHH-20153) - Treat CreationTimestamp/UpdateTimestamp generator fields as not-null
* [HHH-20148](https://hibernate.atlassian.net/browse/HHH-20148) - Avoid depending on module java.desktop
* [HHH-20145](https://hibernate.atlassian.net/browse/HHH-20145) - Automatically align Hibernate artifact versions for Gradle consumers
* [HHH-20130](https://hibernate.atlassian.net/browse/HHH-20130) - unique key constraint for @ElementCollection Set
* [HHH-20123](https://hibernate.atlassian.net/browse/HHH-20123) - Address the split package issue in hibernate-micrometer and hibernate-core

### Improvement
* [HHH-20122](https://hibernate.atlassian.net/browse/HHH-20122) - Update Agroal to 3.0.1
* [HHH-20120](https://hibernate.atlassian.net/browse/HHH-20120) - Remove default max_fetch_depth from MySQLDialect
* [HHH-20115](https://hibernate.atlassian.net/browse/HHH-20115) - Pass a ROOT locale to loggers to avoid unnecessary class lookups
* [HHH-20090](https://hibernate.atlassian.net/browse/HHH-20090) - Use ROWID for MERGE statement if possible
* [HHH-20020](https://hibernate.atlassian.net/browse/HHH-20020) - A proxy associated with an @Any mapping is always considered dirty.
* [HHH-19951](https://hibernate.atlassian.net/browse/HHH-19951) - introduce DetachedObjectException
* [HHH-19933](https://hibernate.atlassian.net/browse/HHH-19933) - pagination and join fetch
* [HHH-19849](https://hibernate.atlassian.net/browse/HHH-19849) - Add an SPI that allows attaching session-scoped "extensions" to the session/statelesssession implementors
* [HHH-19371](https://hibernate.atlassian.net/browse/HHH-19371) - use a StatelessSession for temporary sessions
* [HHH-19247](https://hibernate.atlassian.net/browse/HHH-19247) - Make it possible to order type contributions analogous to function contributions
* [HHH-18938](https://hibernate.atlassian.net/browse/HHH-18938) - BatchBuilder, StatisticsFactory, MutationExecutorService, SchemaManagementTool as Java services
* [HHH-18908](https://hibernate.atlassian.net/browse/HHH-18908) - missing logging for READ_ONLY and NONSTRICT_READ_WRITE
* [HHH-18812](https://hibernate.atlassian.net/browse/HHH-18812) - MySQLSqlAstTranslator.visitCastTarget circumvents the machinery in DdlType
* [HHH-18210](https://hibernate.atlassian.net/browse/HHH-18210) - Up-front validation of generator types
* [HHH-16730](https://hibernate.atlassian.net/browse/HHH-16730) - joining @Any and @ManyToAny mappings
* [HHH-15492](https://hibernate.atlassian.net/browse/HHH-15492) - mappedBy referencing @JoinFormula
* [HHH-15048](https://hibernate.atlassian.net/browse/HHH-15048) - Allow @Nationalized at the class or package level
* [HHH-9897](https://hibernate.atlassian.net/browse/HHH-9897) - @OneToMany association with @JoinFormula throws NPE
* [HHH-9662](https://hibernate.atlassian.net/browse/HHH-9662) - IllegalArgumentException when composite ID has IDENTITY generated value
* [HHH-6044](https://hibernate.atlassian.net/browse/HHH-6044) - Support post-insert generators with partially generated composite ids
* [HHH-5247](https://hibernate.atlassian.net/browse/HHH-5247) - Support indexes and unique-keys in schema validator

### New Feature
* [HHH-20201](https://hibernate.atlassian.net/browse/HHH-20201) - allow @MapsId to target an @EmbeddedId within an @IdClass
* [HHH-20104](https://hibernate.atlassian.net/browse/HHH-20104) - temporal data based on global revision number
* [HHH-20077](https://hibernate.atlassian.net/browse/HHH-20077) - temporal entities
* [HHH-20044](https://hibernate.atlassian.net/browse/HHH-20044) - Support Spanner PostgreSQL dialect in Hibernate
* [HHH-19971](https://hibernate.atlassian.net/browse/HHH-19971) - @Any and @ManyToAny needs a cascade member as replacement for deprecated Cascade annotation
* [HHH-19956](https://hibernate.atlassian.net/browse/HHH-19956) - Add support for InterSystems IRIS
* [HHH-19879](https://hibernate.atlassian.net/browse/HHH-19879) - Move Hibernate Tools' reveng module to Hibernate ORM and merge the relevant ant/gradle/maven plugins
* [HHH-15383](https://hibernate.atlassian.net/browse/HHH-15383) - emulate FULL JOIN using UNION on some dialects
* [HHH-13284](https://hibernate.atlassian.net/browse/HHH-13284) - Allow refreshing multiple entities based on query
* [HHH-12997](https://hibernate.atlassian.net/browse/HHH-12997) - @OneToMany does not work with @JoinFormula

### Remove Feature
* [HHH-20284](https://hibernate.atlassian.net/browse/HHH-20284) - Remove support for database versions that are unsupported by vendors 7.4 edition

### Sub-task
* [HHH-20352](https://hibernate.atlassian.net/browse/HHH-20352) - enable parallel testing and test fixes for hibernate-envrers and hibernate-jache
* [HHH-20351](https://hibernate.atlassian.net/browse/HHH-20351) - Re-enable UNNEST WITH ORDINALITY tests in Spanner PG
* [HHH-20338](https://hibernate.atlassian.net/browse/HHH-20338) - Fix upsert and use standard on conflict clause 
* [HHH-20337](https://hibernate.atlassian.net/browse/HHH-20337) - Fix newly failing tests in Spanner PG dialect
* [HHH-20325](https://hibernate.atlassian.net/browse/HHH-20325) - Fix Spanner PG Locking support
* [HHH-20316](https://hibernate.atlassian.net/browse/HHH-20316) - Fix hibernate-envers tests for Spanner PG dialects
* [HHH-20310](https://hibernate.atlassian.net/browse/HHH-20310) - Fixes for SpannerDialect 
* [HHH-20298](https://hibernate.atlassian.net/browse/HHH-20298) - Move SpannerPostgreSQLDialect to hibernate-core
* [HHH-20292](https://hibernate.atlassian.net/browse/HHH-20292) - Fix container memory issue
* [HHH-20286](https://hibernate.atlassian.net/browse/HHH-20286) - Fixes for SpannerDialect
* [HHH-20285](https://hibernate.atlassian.net/browse/HHH-20285) - Remove support for PostgreSQL Advanced Server versions older than 14
* [HHH-20279](https://hibernate.atlassian.net/browse/HHH-20279) - Remove support for PostgreSQL versions older than 14
* [HHH-20268](https://hibernate.atlassian.net/browse/HHH-20268) - Fix table without primary key issue
* [HHH-20262](https://hibernate.atlassian.net/browse/HHH-20262) - Fix Spanner Tests Part 3
* [HHH-20252](https://hibernate.atlassian.net/browse/HHH-20252) - Spanner feature updates
* [HHH-20250](https://hibernate.atlassian.net/browse/HHH-20250) - Fix Spanner Tests Part 2
* [HHH-20208](https://hibernate.atlassian.net/browse/HHH-20208) - Fix SpannerDialect functions
* [HHH-20198](https://hibernate.atlassian.net/browse/HHH-20198) - Refactor ToOneIdMapper.java
* [HHH-20197](https://hibernate.atlassian.net/browse/HHH-20197) - Refactor CustomRunner.java
* [HHH-20175](https://hibernate.atlassian.net/browse/HHH-20175) - Fix Spanner PG Tests
* [HHH-20171](https://hibernate.atlassian.net/browse/HHH-20171) - Configure Exception handling in Spanner PG dialect
* [HHH-20160](https://hibernate.atlassian.net/browse/HHH-20160) - Fix Literal Rendering for Spanner Dialect
* [HHH-20140](https://hibernate.atlassian.net/browse/HHH-20140) - Support generating Integer PK 

### Task
* [HHH-20347](https://hibernate.atlassian.net/browse/HHH-20347) - Replace google-java-format with Eclipse JDT formatter to avoid compatibility issues
* [HHH-20341](https://hibernate.atlassian.net/browse/HHH-20341) - Embedded MavenCli in integration tests cannot resolve plugins on Jenkins CI
* [HHH-20335](https://hibernate.atlassian.net/browse/HHH-20335) - Test against JDK 27
* [HHH-20334](https://hibernate.atlassian.net/browse/HHH-20334) - Upgrade to Log4j 2.25.4
* [HHH-20311](https://hibernate.atlassian.net/browse/HHH-20311) - Upgrade to hibernate-modules 1.1.1
* [HHH-20301](https://hibernate.atlassian.net/browse/HHH-20301) - Upgrade ByteBuddy to 1.18.8
* [HHH-20270](https://hibernate.atlassian.net/browse/HHH-20270) - Force use of org.checkerframework:checker-qual-android
* [HHH-20232](https://hibernate.atlassian.net/browse/HHH-20232) - Update c3p0 to 0.12.0

## 7.3.0.CR2 (February 03, 2026)

[Full changelog](https://hibernate.atlassian.net/projects/HHH/versions/37404)


### Bug
* [HHH-20121](https://hibernate.atlassian.net/browse/HHH-20121) - NPE when logging loaded values in follow-on locking post action
* [HHH-20118](https://hibernate.atlassian.net/browse/HHH-20118) - Vector operator SQL templates miss parenthesis around
* [HHH-20107](https://hibernate.atlassian.net/browse/HHH-20107) - SparseFloatVector and SparseDoubleVector accept invalid size <= 0
* [HHH-20103](https://hibernate.atlassian.net/browse/HHH-20103) - @ElementCollection mapping should respect @JoinColumn(foreignKey)
* [HHH-20102](https://hibernate.atlassian.net/browse/HHH-20102) - spec says LockTimeoutException + QueryTimeoutException should not be thrown on PostgreSQL
* [HHH-20101](https://hibernate.atlassian.net/browse/HHH-20101) - Error persisting child entity of abstract generic entity
* [HHH-19715](https://hibernate.atlassian.net/browse/HHH-19715) - read-only mode and collections

### Improvement
* [HHH-20127](https://hibernate.atlassian.net/browse/HHH-20127) - Avoid initializing the BigDecimal class when not strictly necessary
* [HHH-19902](https://hibernate.atlassian.net/browse/HHH-19902) - PostgreSQL failed statements mark tx for rollback
* [HHH-19610](https://hibernate.atlassian.net/browse/HHH-19610) - Make GraphParser support treatedSubGraph from root

### New Feature
* [HHH-19045](https://hibernate.atlassian.net/browse/HHH-19045) - Bytecode enhancer should add a default constructor if it's missing

### Sub-task
* [HHH-20112](https://hibernate.atlassian.net/browse/HHH-20112) - Skip concurrent modification related tests
* [HHH-20111](https://hibernate.atlassian.net/browse/HHH-20111) - Avoid harcoded column definition in tests
* [HHH-20110](https://hibernate.atlassian.net/browse/HHH-20110) - Handle TIMESTAMP and NUMERIC column types in Spanner PG
* [HHH-20109](https://hibernate.atlassian.net/browse/HHH-20109) - Handle Column types for Spanner PG
* [HHH-20108](https://hibernate.atlassian.net/browse/HHH-20108) - Fix connection URL in Spanner PG
* [HHH-20100](https://hibernate.atlassian.net/browse/HHH-20100) - Fix Foreign key issue
* [HHH-20099](https://hibernate.atlassian.net/browse/HHH-20099) - Handle ON CONFLICT clause for Spanner
* [HHH-20098](https://hibernate.atlassian.net/browse/HHH-20098) - Support FORWARD_ONLY scrollable resultset

## 8.0.0.Alpha1 (February 02, 2026)

[Full changelog](https://hibernate.atlassian.net/projects/HHH/versions/32326)


### Improvement
* [HHH-20129](https://hibernate.atlassian.net/browse/HHH-20129) - Move JPA callbacks to EntityPersister
* [HHH-20074](https://hibernate.atlassian.net/browse/HHH-20074) - Adapt Hibernate Query contracts to new JPA 4.0 structure

### Task
* [HHH-20028](https://hibernate.atlassian.net/browse/HHH-20028) - Update to Jakarta Persistence 4.0

## 7.3.0.CR1 (January 23, 2026)

[Full changelog](https://hibernate.atlassian.net/projects/HHH/versions/35980)


### Bug
* [HHH-20095](https://hibernate.atlassian.net/browse/HHH-20095) - New SchemaValidator nullability check should only consider explicitly declared nullability
* [HHH-20088](https://hibernate.atlassian.net/browse/HHH-20088) - HbmXmlTransform does not consider the <set >, <map>, <list>, <bag> and <array> outer-join value to determine the fetch mode. 
* [HHH-20087](https://hibernate.atlassian.net/browse/HHH-20087) - NPE with StatelessSession + Bean Validation
* [HHH-20079](https://hibernate.atlassian.net/browse/HHH-20079) - HbmXmlTransform does not create <sql-join-table-restriction> for ManyToMany attributes
* [HHH-20072](https://hibernate.atlassian.net/browse/HHH-20072) - Javadoc cannot be generated without Jackson 3 dependencies
* [HHH-20069](https://hibernate.atlassian.net/browse/HHH-20069) - `DB2iDialect.rowId` causes an error in merge queries
* [HHH-20055](https://hibernate.atlassian.net/browse/HHH-20055) - <synchronize> is ignored in orm.xml
* [HHH-20041](https://hibernate.atlassian.net/browse/HHH-20041) - DB2 for z IN tuple list predicate performs badly
* [HHH-20037](https://hibernate.atlassian.net/browse/HHH-20037) - MappedSuperClasses can be enhanced more than once resulting in Duplicate annotation interface org...EnhancementInfo Exception
* [HHH-20032](https://hibernate.atlassian.net/browse/HHH-20032) - SubSequence.subSequence violates CharSequence contract for start == end == length()
* [HHH-20027](https://hibernate.atlassian.net/browse/HHH-20027) - Fix failing parsing of PostgreSQL canonical lock_timeout formats (0, ms, s, min, h)
* [HHH-20021](https://hibernate.atlassian.net/browse/HHH-20021) - Binding GregorianCalendar parameter fails with "Type registration was corrupted"
* [HHH-20015](https://hibernate.atlassian.net/browse/HHH-20015) - Hibernate Maven Plugin 7.x does not include maven project dependencies in the enhancement classpath
* [HHH-20006](https://hibernate.atlassian.net/browse/HHH-20006) - Outcome of getSingleResult changed since 6.0
* [HHH-20002](https://hibernate.atlassian.net/browse/HHH-20002) - Ensure physical JDBC connection is released when closing LogicalConnectionManagedImpl
* [HHH-19999](https://hibernate.atlassian.net/browse/HHH-19999) - Caching APIs use Comparator<?> for version comparison in Hibernate ORM 7.2.0.Final
* [HHH-19932](https://hibernate.atlassian.net/browse/HHH-19932) - NullPointerException in SqmInterpretationsKey::toString
* [HHH-19929](https://hibernate.atlassian.net/browse/HHH-19929) - DB2iDialect problem with supportsRowValueConstructorSyntaxInInSubQuery
* [HHH-19861](https://hibernate.atlassian.net/browse/HHH-19861) - EntityNotFoundException when using @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED) on FK fields
* [HHH-19390](https://hibernate.atlassian.net/browse/HHH-19390) - obsolete documentation for bytecode enhancer
* [HHH-19192](https://hibernate.atlassian.net/browse/HHH-19192) - Bulk delete of owner with soft-delete element-collection physically deletes the collection rows
* [HHH-18835](https://hibernate.atlassian.net/browse/HHH-18835) - AssertionError when executing insert-select
* [HHH-18040](https://hibernate.atlassian.net/browse/HHH-18040) - MySQL update/delete statement issue with subqueries
* [HHH-12678](https://hibernate.atlassian.net/browse/HHH-12678) - PersistentClass.checkColumnDuplication fails for columns with same name from distinct tables
* [HHH-12100](https://hibernate.atlassian.net/browse/HHH-12100) - Misleading exception message in JTASessionContext
* [HHH-9035](https://hibernate.atlassian.net/browse/HHH-9035) - Globally quoted identifiers and id class not working properly with quoted join columns error Unable to find logical column name from physical name JobPositon+id in table JOB+Position
* [HHH-7287](https://hibernate.atlassian.net/browse/HHH-7287) - Problem in caching proper natural-id-values when obtaining result by naturalIdQuery

### Deprecation
* [HHH-20073](https://hibernate.atlassian.net/browse/HHH-20073) - deprecate ByteArrayJavaType and CharacterArrayJavaType

### Improvement
* [HHH-20076](https://hibernate.atlassian.net/browse/HHH-20076) - @Version columns should be NOT NULL
* [HHH-20059](https://hibernate.atlassian.net/browse/HHH-20059) - TiDB: don't propagate readonly to server
* [HHH-20058](https://hibernate.atlassian.net/browse/HHH-20058) - TiDB: Enable shared lock promotion
* [HHH-20054](https://hibernate.atlassian.net/browse/HHH-20054) - Optimize BasicTypeImpl allocation
* [HHH-20030](https://hibernate.atlassian.net/browse/HHH-20030) - FilterImpl: cache validate() results and harden deserialization restore
* [HHH-20026](https://hibernate.atlassian.net/browse/HHH-20026) - Stop hibernate-maven-plugin execution on errors
* [HHH-20025](https://hibernate.atlassian.net/browse/HHH-20025) - Align Generator with UserType
* [HHH-20017](https://hibernate.atlassian.net/browse/HHH-20017) - Refactor AnnotationBasedGenerator to align with AnnotationBasedUserType
* [HHH-20013](https://hibernate.atlassian.net/browse/HHH-20013) - remove dependency to Classmate
* [HHH-20012](https://hibernate.atlassian.net/browse/HHH-20012) - Update the TiDBDialect
* [HHH-20011](https://hibernate.atlassian.net/browse/HHH-20011) - Upgrade minmum version supported for cockroach and upgrade db testing to 25
* [HHH-20009](https://hibernate.atlassian.net/browse/HHH-20009) - db.sh fails to start PostgreSQL 17 and 18 on Apple Silicon (ARM64)
* [HHH-19997](https://hibernate.atlassian.net/browse/HHH-19997) - Update TiDB testing environment
* [HHH-19992](https://hibernate.atlassian.net/browse/HHH-19992) - auto-generate check constraint on @OrderColumn
* [HHH-19950](https://hibernate.atlassian.net/browse/HHH-19950) - Alternative to pre-set parameters offered for deprecated DynamicParameterizedType
* [HHH-19919](https://hibernate.atlassian.net/browse/HHH-19919) - Indexed collection initializers should resolveInstance instead of resolveKey
* [HHH-19897](https://hibernate.atlassian.net/browse/HHH-19897) - Hibernate Envers audited associations mapped-by non-audited ones
* [HHH-19890](https://hibernate.atlassian.net/browse/HHH-19890) - Support a FormatMapper for Jackson3
* [HHH-19867](https://hibernate.atlassian.net/browse/HHH-19867) - Add support for Oracle 26ai in OTP
* [HHH-19804](https://hibernate.atlassian.net/browse/HHH-19804) - Documentation should recommend `annotationProcessorPathsUseDepMgmt` in maven-compiler-plugin when using hibernate-processor
* [HHH-18984](https://hibernate.atlassian.net/browse/HHH-18984) - Remove deprecated Gradle API usage in Hibernate Gradle Plugin
* [HHH-18461](https://hibernate.atlassian.net/browse/HHH-18461) - Improve dialects to generate sql contains offset clause if first result of native query is explicit 0
* [HHH-14584](https://hibernate.atlassian.net/browse/HHH-14584) - Allow PhysicalNamingStrategy implementations to detect when a name is implicit or explicit
* [HHH-7202](https://hibernate.atlassian.net/browse/HHH-7202) - Early detection of bad targetEntity
* [HHH-6882](https://hibernate.atlassian.net/browse/HHH-6882) - Expose CollectionPersister from AbstractCollectionEvent
* [HHH-6598](https://hibernate.atlassian.net/browse/HHH-6598) - Immutable entities should not have up-to-date checks performed on a flush

### New Feature
* [HHH-20060](https://hibernate.atlassian.net/browse/HHH-20060) - TenantCredentialsMapper
* [HHH-20029](https://hibernate.atlassian.net/browse/HHH-20029) - hibernate.connection.login_timeout
* [HHH-19993](https://hibernate.atlassian.net/browse/HHH-19993) - Introduce UserTypeCreationContext and AnnotationBasedUserType
* [HHH-19989](https://hibernate.atlassian.net/browse/HHH-19989) - RemovalsMode.EXCLUDE
* [HHH-19978](https://hibernate.atlassian.net/browse/HHH-19978) - Support type variable members also in abstract entities
* [HHH-19880](https://hibernate.atlassian.net/browse/HHH-19880) - Move Hibernate Tools' `hibernate-assistant` module to Hibernate ORM
* [HHH-19826](https://hibernate.atlassian.net/browse/HHH-19826) - Add array_reverse and array_sort functions
* [HHH-19541](https://hibernate.atlassian.net/browse/HHH-19541) - "exists" queries
* [HHH-18998](https://hibernate.atlassian.net/browse/HHH-18998) - Supply UserType and UserCollectionType with type-safe config via annotation
* [HHH-17657](https://hibernate.atlassian.net/browse/HHH-17657) - Support named enum types on h2
* [HHH-16383](https://hibernate.atlassian.net/browse/HHH-16383) - NaturalIdClass

### Sub-task
* [HHH-20091](https://hibernate.atlassian.net/browse/HHH-20091) - Fix CTE Rendering Syntax for Spanner
* [HHH-20086](https://hibernate.atlassian.net/browse/HHH-20086) - Handle no escape character in Spanner PG dialect
* [HHH-20066](https://hibernate.atlassian.net/browse/HHH-20066) - Support only JSON Aggregation for Spanner PG Dialect
* [HHH-20050](https://hibernate.atlassian.net/browse/HHH-20050) - Create Spanner PostgreSQL dialect
* [HHH-20045](https://hibernate.atlassian.net/browse/HHH-20045) - Add support to create unique index when unique column is not supported
* [HHH-20038](https://hibernate.atlassian.net/browse/HHH-20038) - Fixes for SpannerDialect-3
* [HHH-20034](https://hibernate.atlassian.net/browse/HHH-20034) - Fixes for SpannerDialect-2
* [HHH-20033](https://hibernate.atlassian.net/browse/HHH-20033) - Introduce Dialect#getSetOperatorSqlString to support Spanner's explicit DISTINCT requirement
* [HHH-20003](https://hibernate.atlassian.net/browse/HHH-20003) - Spanner temporary table exporter
* [HHH-19991](https://hibernate.atlassian.net/browse/HHH-19991) - Fixes for SpannerDialect 
* [HHH-19983](https://hibernate.atlassian.net/browse/HHH-19983) - Config for running tests on Spanner emualtor 

## 7.2.0.CR4 (December 10, 2025)

[Full changelog](https://hibernate.atlassian.net/projects/HHH/versions/36476)


### Bug
* [HHH-19980](https://hibernate.atlassian.net/browse/HHH-19980) - In JTA after-completion callbacks may get ignored
* [HHH-19979](https://hibernate.atlassian.net/browse/HHH-19979) - processor should handle @NamedEntityGraph with defaulted name
* [HHH-19975](https://hibernate.atlassian.net/browse/HHH-19975) - Calling entityManager.find(clazz, id) with null id throws NullPointerException
* [HHH-19972](https://hibernate.atlassian.net/browse/HHH-19972) - OptionalTableUpdateOperation can fail on PostgreSQL < 15 and CockroachDB
* [HHH-19963](https://hibernate.atlassian.net/browse/HHH-19963) - Wrong references in entity fields with circular associations
* [HHH-19958](https://hibernate.atlassian.net/browse/HHH-19958) - `<generated>` tag in orm.xml is not implemented
* [HHH-19955](https://hibernate.atlassian.net/browse/HHH-19955) - Thread-safety issue in EntityEntryContext resulting in NullPointerException for Session.contains() calls.
* [HHH-19843](https://hibernate.atlassian.net/browse/HHH-19843) - Bean Validation may fail on operations with stateless session
* [HHH-18871](https://hibernate.atlassian.net/browse/HHH-18871) - Nested NativeQuery mappings causing 'Could not locate TableGroup' exception after migration
* [HHH-18217](https://hibernate.atlassian.net/browse/HHH-18217) - StatelessSession.upsert() for entity with all-null non-id fields, or no non-id field

### Improvement
* [HHH-19943](https://hibernate.atlassian.net/browse/HHH-19943) - Comparison of generic nested EmbeddedId's fails for JPQL and Criteria API
* [HHH-19215](https://hibernate.atlassian.net/browse/HHH-19215) - Extends Dialect#addQueryHints to support straight_join syntax

## 7.2.0.CR3 (November 25, 2025)

[Full changelog](https://hibernate.atlassian.net/projects/HHH/versions/36014)


### Bug
* [HHH-19939](https://hibernate.atlassian.net/browse/HHH-19939) - broken caching for MERGE and REFRESH load plans
* [HHH-19937](https://hibernate.atlassian.net/browse/HHH-19937) - duplicate version check after refresh
* [HHH-19936](https://hibernate.atlassian.net/browse/HHH-19936) - Parameter casts for by-id lookups don't take column definition into account
* [HHH-19932](https://hibernate.atlassian.net/browse/HHH-19932) - NullPointerException in SqmInterpretationsKey::toString
* [HHH-19926](https://hibernate.atlassian.net/browse/HHH-19926) - NullPointerException when executing JPQL IN clause with null parameter on entity association
* [HHH-19925](https://hibernate.atlassian.net/browse/HHH-19925) - Locking root(s) should be based on select-clause, not from-clause
* [HHH-19924](https://hibernate.atlassian.net/browse/HHH-19924) - Session#find with LockMode.UPGRADE_NOWAIT casues AssetionError when no entity with the provided id exists in the database
* [HHH-19922](https://hibernate.atlassian.net/browse/HHH-19922) - org.hibernate.orm:hibernate-platform:pom:7.1.7.Final is missing
* [HHH-19918](https://hibernate.atlassian.net/browse/HHH-19918) - Avoid reflection when instantiating known FormatMapper
* [HHH-19910](https://hibernate.atlassian.net/browse/HHH-19910) - EntityInitializer#resolveInstance wrongly initializes existing detached instance
* [HHH-19906](https://hibernate.atlassian.net/browse/HHH-19906) - JsonGeneratingVisitor#visit doesn't handle plural types correctly
* [HHH-19905](https://hibernate.atlassian.net/browse/HHH-19905) - Implicit join re-use with nested inner and left joins causes ParsingException
* [HHH-19895](https://hibernate.atlassian.net/browse/HHH-19895) - hibernate-core 6.6.30.Final breaks compatibility on entities with composite keys for multiple variants of DB2
* [HHH-19758](https://hibernate.atlassian.net/browse/HHH-19758) - HQL parse failure with SLL can lead to wrong parse
* [HHH-19749](https://hibernate.atlassian.net/browse/HHH-19749) - [Oracle] Merge with @SecondaryTable may generate invalid NUMERIC type casts
* [HHH-19739](https://hibernate.atlassian.net/browse/HHH-19739) - Exceptions during load of entity with different persistent fields with same name
* [HHH-19240](https://hibernate.atlassian.net/browse/HHH-19240) - Significant increase in heap allocation for queries after migrating Hibernate ORM 6.5 to 6.6
* [HHH-19038](https://hibernate.atlassian.net/browse/HHH-19038) - Hibernate.get does not work on detached entities
* [HHH-18860](https://hibernate.atlassian.net/browse/HHH-18860) - Updates on SecondaryTable cause incorrect cast for BigDecimal
* [HHH-16991](https://hibernate.atlassian.net/browse/HHH-16991) - EnhancedUserType cannot be used when defining relations
* [HHH-14032](https://hibernate.atlassian.net/browse/HHH-14032) - Locales with scripts are not round-tripped properly

### Deprecation
* [HHH-19941](https://hibernate.atlassian.net/browse/HHH-19941) - deprecate Session.contains(String, Object)

### Improvement
* [HHH-19953](https://hibernate.atlassian.net/browse/HHH-19953) - Relax scopes of methods in EntityInitializerImpl (for Hibernate Reactive)
* [HHH-19827](https://hibernate.atlassian.net/browse/HHH-19827) - Apply the unified Hibernate Documentation theme

### New Feature
* [HHH-19931](https://hibernate.atlassian.net/browse/HHH-19931) - SchemaManager.truncateTable()

### Task
* [HHH-19944](https://hibernate.atlassian.net/browse/HHH-19944) - Upgrade MS SQL JDBC driver to remediate CVE-2025-59250
* [HHH-19940](https://hibernate.atlassian.net/browse/HHH-19940) - Include maven plugin in the release staging directory
* [HHH-19921](https://hibernate.atlassian.net/browse/HHH-19921) - Drop last JUnit 4 usages in Hibernate Envers 
* [HHH-19916](https://hibernate.atlassian.net/browse/HHH-19916) - More drop JUnit 4 usage work

Note: Please refer to JIRA to learn more about each issue.

## 7.2.0.CR2 (November 10, 2025)

[Full changelog](https://hibernate.atlassian.net/projects/HHH/versions/35584)

### Bug
* [HHH-19914](https://hibernate.atlassian.net/browse/HHH-19914) - JPA_COMPLIANCE setting ignored in @Jpa
* [HHH-19896](https://hibernate.atlassian.net/browse/HHH-19896) - Criteria select case expressions: NPE when second query contains an otherwise case expression but not the first query
* [HHH-19888](https://hibernate.atlassian.net/browse/HHH-19888) - FetchPlusOffsetParameterBinder fails to apply static offset for root pagination
* [HHH-19887](https://hibernate.atlassian.net/browse/HHH-19887) - Wrong ClassLoader used for Jackson Module discovery
* [HHH-19883](https://hibernate.atlassian.net/browse/HHH-19883) - JOIN TREAT ignores predicates
* [HHH-19874](https://hibernate.atlassian.net/browse/HHH-19874) - NullPointerException in org.hibernate.sql.ast.tree.from.TableGroup.resolveTableReference
* [HHH-19872](https://hibernate.atlassian.net/browse/HHH-19872) - CriteriaUpdate error updating generic field in @MappedSuperclass
* [HHH-19868](https://hibernate.atlassian.net/browse/HHH-19868) - RowTransformerConstructorImpl throws NullPointerException when TupleMetadata is null
* [HHH-19865](https://hibernate.atlassian.net/browse/HHH-19865) - In JTA after-completion callbacks may get ignored
* [HHH-19862](https://hibernate.atlassian.net/browse/HHH-19862) - Can't update converted column throught CriteriaUpdate#set( SingularAttribute<? super T, Y> attribute, X value);
* [HHH-19857](https://hibernate.atlassian.net/browse/HHH-19857) - When one of the lazy attributes is `null` the other ones may not get initialized correctly
* [HHH-19851](https://hibernate.atlassian.net/browse/HHH-19851) - Session#findMultiple(EntityGraph...) fails for dynamic entities
* [HHH-19848](https://hibernate.atlassian.net/browse/HHH-19848) - NPE when using MySQLLegacyDialect
* [HHH-19840](https://hibernate.atlassian.net/browse/HHH-19840) - JDBC batching is not working with @CreationTimestamp and @UpdateTimestamp from Hibernate 7
* [HHH-19824](https://hibernate.atlassian.net/browse/HHH-19824) - DB2zDialect does not use correct querySequenceString on a DB2 on zOs
* [HHH-18691](https://hibernate.atlassian.net/browse/HHH-18691) - Envers cannot read revisions with records

### Improvement
* [HHH-19899](https://hibernate.atlassian.net/browse/HHH-19899) - Proxy generation fails with @ConcreteProxy and sealed abstract entities
* [HHH-19892](https://hibernate.atlassian.net/browse/HHH-19892) - signature of Session.merge() accepting EntityGraph
* [HHH-19884](https://hibernate.atlassian.net/browse/HHH-19884) - Hibernate testing class-template lifecycle listeners
* [HHH-19852](https://hibernate.atlassian.net/browse/HHH-19852) - Better detection of Oracle extended maximum string size
* [HHH-19830](https://hibernate.atlassian.net/browse/HHH-19830) - Use of markdown for Javadoc
* [HHH-18286](https://hibernate.atlassian.net/browse/HHH-18286) - Add option to fail bootstrapping on failure to access JDBC DatabaseMetadata

### Sub-task
* [HHH-19871](https://hibernate.atlassian.net/browse/HHH-19871) - Drop JUnit 4 usage in Hibernate Envers

### Task
* [HHH-19894](https://hibernate.atlassian.net/browse/HHH-19894) - Use Java 25 for building
* [HHH-19889](https://hibernate.atlassian.net/browse/HHH-19889) - Mention third-party dialects throughout the documentation
* [HHH-19873](https://hibernate.atlassian.net/browse/HHH-19873) - Upgrade build to Gradle 9.1
* [HHH-19869](https://hibernate.atlassian.net/browse/HHH-19869) - Regroup all dialect information under a single guide
* [HHH-19846](https://hibernate.atlassian.net/browse/HHH-19846) - Drop JUnit 4 usage


## 7.2.0.CR1 (October 08, 2025)

[Full changelog](https://hibernate.atlassian.net/projects/HHH/versions/34530)

### Bug
* [HHH-19831](https://hibernate.atlassian.net/browse/HHH-19831) - LeakingStatementCachingTest hangs
* [HHH-19813](https://hibernate.atlassian.net/browse/HHH-19813) - Incorrect default values for 'enableDirtyTracking' and 'enableLazyInitialization' in hibernate-maven-plugin
* [HHH-19795](https://hibernate.atlassian.net/browse/HHH-19795) - SqmFunctionRegistry does not provide a way to escape ? in registerPattern
* [HHH-19790](https://hibernate.atlassian.net/browse/HHH-19790) - Fix Gradle Worker ID gaps for parallel testing
* [HHH-19781](https://hibernate.atlassian.net/browse/HHH-19781) - Subsequent uses of Criteria SelectionSpecification lead to duplicated specifications
* [HHH-19780](https://hibernate.atlassian.net/browse/HHH-19780) - OracleDatabaseCleaner must not fail when not finding an object to drop
* [HHH-19775](https://hibernate.atlassian.net/browse/HHH-19775) - type(:entity) in JPQL
* [HHH-19771](https://hibernate.atlassian.net/browse/HHH-19771) - transaction context sharing between stateful and stateless sessions 
* [HHH-19759](https://hibernate.atlassian.net/browse/HHH-19759) - joining a map key of basic type
* [HHH-19740](https://hibernate.atlassian.net/browse/HHH-19740) - Collection table deletion for table per class subclass entity fails with UnknownTableReferenceException
* [HHH-19738](https://hibernate.atlassian.net/browse/HHH-19738) - JDBC password logged when specified via jakarta.persistence.jdbc.password
* [HHH-19723](https://hibernate.atlassian.net/browse/HHH-19723) - Hibernate-testing depends on outdated Jakarta libraries, leading to compilation issues for Jakarta Data repositories
* [HHH-19721](https://hibernate.atlassian.net/browse/HHH-19721) - Jakarta Data is missing from hibernate-platform (BOM)
* [HHH-19718](https://hibernate.atlassian.net/browse/HHH-19718) - SQL @Formula with function call nested inside cast()
* [HHH-19716](https://hibernate.atlassian.net/browse/HHH-19716) - Collection event listeners may be missing collection owners  in the persistent collection (PersistentCollection#getOwner==null)
* [HHH-19706](https://hibernate.atlassian.net/browse/HHH-19706) - Composite @Id with a generic part in @MappedSuperclass
* [HHH-19671](https://hibernate.atlassian.net/browse/HHH-19671) - Wrong warning aboute illegal use of @Embeddable with callback on entities with @IdClass
* [HHH-19659](https://hibernate.atlassian.net/browse/HHH-19659) - (Kotlin / Panache) Entity with @IdClass and @PrePersist/@PreUpdate misinterpreted as @Embeddable
* [HHH-19630](https://hibernate.atlassian.net/browse/HHH-19630) - Hibernate Processor may fail if the return type is a single entity and annotated with multiple annotations
* [HHH-19629](https://hibernate.atlassian.net/browse/HHH-19629) - Hibernate Processor may fail when repository method parameter has more than one annotation (e.g. multiple constraints)
* [HHH-19627](https://hibernate.atlassian.net/browse/HHH-19627) - MetaModel fields not generated for java.sql.Clob fields
* [HHH-19276](https://hibernate.atlassian.net/browse/HHH-19276) - Native query with enum list param leads to OutOfMemory
* [HHH-18993](https://hibernate.atlassian.net/browse/HHH-18993) - [Docs] Possible outdated documentation and update suggestions in the user guide
* [HHH-14082](https://hibernate.atlassian.net/browse/HHH-14082) - Hibernate cannot determine it's core version in modular configuration
* [HHH-19085](https://hibernate.atlassian.net/browse/HHH-19085) - NPE when using null value in CriteriaUpdate
* [HHH-19393](https://hibernate.atlassian.net/browse/HHH-19393) - Hibernate Envers can not handle ID class if it is implemented as Java record


### Deprecation
* [HHH-19751](https://hibernate.atlassian.net/browse/HHH-19751) - Deprecate AzureSQLServerDialect for removal

### Improvement
* [HHH-19832](https://hibernate.atlassian.net/browse/HHH-19832) - Upgrade to JUnit 6.0
* [HHH-19829](https://hibernate.atlassian.net/browse/HHH-19829) - Deprecate MultiIdentifierLoadAccess and byMultipleIds
* [HHH-19810](https://hibernate.atlassian.net/browse/HHH-19810) - Remove Joda-Time and use LocalDate of jdk in testing
* [HHH-19808](https://hibernate.atlassian.net/browse/HHH-19808) - Automatic closing for child session with shared connection/tx
* [HHH-19801](https://hibernate.atlassian.net/browse/HHH-19801) - SchemaTruncator should reset sequences
* [HHH-19791](https://hibernate.atlassian.net/browse/HHH-19791) - Avoid dropping/creating database objects when data is only read (SELECT)
* [HHH-19782](https://hibernate.atlassian.net/browse/HHH-19782) - Oracle support for locking across joins
* [HHH-19776](https://hibernate.atlassian.net/browse/HHH-19776) - Migrate all logging to the newer style "subsystem" logging
* [HHH-19774](https://hibernate.atlassian.net/browse/HHH-19774) - Automatic flushing for child session with shared connection/tx
* [HHH-19772](https://hibernate.atlassian.net/browse/HHH-19772) - change to semantics of interceptor reuse with shared session builders
* [HHH-19762](https://hibernate.atlassian.net/browse/HHH-19762) - Enable Parallel Testing for OTP
* [HHH-19757](https://hibernate.atlassian.net/browse/HHH-19757) - many registry operations have unbound type parameter in return type
* [HHH-19755](https://hibernate.atlassian.net/browse/HHH-19755) - Improve AntlrPlugin
* [HHH-19754](https://hibernate.atlassian.net/browse/HHH-19754) - Migrate XjcPlugin to using direct Java calls
* [HHH-19752](https://hibernate.atlassian.net/browse/HHH-19752) - Allow setting MariaDB/MySQL storage engine not only using System properties but also configuration properties
* [HHH-19744](https://hibernate.atlassian.net/browse/HHH-19744) - Clarify and document the wildfly-transaction-client integration
* [HHH-19743](https://hibernate.atlassian.net/browse/HHH-19743) - Deprecate JBossStandAloneJtaPlatform in favor of a new, clearly Narayana-specific implementation
* [HHH-19737](https://hibernate.atlassian.net/browse/HHH-19737) - Support Envers with StatelessSession
* [HHH-19733](https://hibernate.atlassian.net/browse/HHH-19733) - Extract constants often reused in ByteBubby processing
* [HHH-19730](https://hibernate.atlassian.net/browse/HHH-19730) - Avoid bulk-delete collection cleanup when delete cascaded
* [HHH-19717](https://hibernate.atlassian.net/browse/HHH-19717) - CockroachDB supports insert and update returning clause
* [HHH-19711](https://hibernate.atlassian.net/browse/HHH-19711) - Improve mapped-by + join-column exception message with the full property path
* [HHH-19702](https://hibernate.atlassian.net/browse/HHH-19702) - ConnectionProviders don't need "Impl" in name
* [HHH-19694](https://hibernate.atlassian.net/browse/HHH-19694) - Enhanced support for older Informix
* [HHH-19690](https://hibernate.atlassian.net/browse/HHH-19690) - Migrate Atlas to OTP
* [HHH-19679](https://hibernate.atlassian.net/browse/HHH-19679) - Support binary, float16 and sparse vector types
* [HHH-19602](https://hibernate.atlassian.net/browse/HHH-19602) - Adjust JdbcOperation to allow more-than-one statement
* [HHH-19556](https://hibernate.atlassian.net/browse/HHH-19556) - improvements to SQM equality
* [HHH-19554](https://hibernate.atlassian.net/browse/HHH-19554) - Support for lock timeout as Connection setting
* [HHH-19514](https://hibernate.atlassian.net/browse/HHH-19514) - Follow-on locking locks more than it should
* [HHH-19513](https://hibernate.atlassian.net/browse/HHH-19513) - Follow-on locking does not lock element-collection tables
* [HHH-19388](https://hibernate.atlassian.net/browse/HHH-19388) - Process <database-object> in mapping.xml
* [HHH-18546](https://hibernate.atlassian.net/browse/HHH-18546) - Clean up any hanging BulkOperationCleanupAction after-txn callbacks on Session close
* [HHH-14892](https://hibernate.atlassian.net/browse/HHH-14892) - Parallel test with GRADLE
* [HHH-13843](https://hibernate.atlassian.net/browse/HHH-13843) - Performance schema migration
* [HHH-9636](https://hibernate.atlassian.net/browse/HHH-9636) - Have JPA PessimisticLockScope.EXTENDED propagate the same LockModeType
* [HHH-19767](https://hibernate.atlassian.net/browse/HHH-19767) - Include license file in the META-INF of published artifacts
* [HHH-19827](https://hibernate.atlassian.net/browse/HHH-19827) - Apply the unified Hibernate Documentation theme

### New Feature
* [HHH-19794](https://hibernate.atlassian.net/browse/HHH-19794) - SchemaManager.resynchronizeGenerators()
* [HHH-19735](https://hibernate.atlassian.net/browse/HHH-19735) - Add vector support for SQL Server
* [HHH-19710](https://hibernate.atlassian.net/browse/HHH-19710) - Add vector support for SAP HANA Cloud
* [HHH-19708](https://hibernate.atlassian.net/browse/HHH-19708) - support for read/write replicas
* [HHH-19705](https://hibernate.atlassian.net/browse/HHH-19705) - Add vector support for DB2
* [HHH-19257](https://hibernate.atlassian.net/browse/HHH-19257) - Introduce @EmbeddedTable
* [HHH-18973](https://hibernate.atlassian.net/browse/HHH-18973) - Add Vector support type for MySQL
* [HHH-3404](https://hibernate.atlassian.net/browse/HHH-3404) - Regular expression matching in HQL
* [HHH-19676](https://hibernate.atlassian.net/browse/HHH-19676) - validate content of JPA @OrderBy in Processor

### Sub-task
* [HHH-19820](https://hibernate.atlassian.net/browse/HHH-19820) - Make sure that the enhancer is not doing anything if all the enablement parameters are false
* [HHH-19817](https://hibernate.atlassian.net/browse/HHH-19817) - Add the JavaDoc Style documentation appropriate for Maven Plugins
* [HHH-19816](https://hibernate.atlassian.net/browse/HHH-19816) - Add Integration Tests for the Maven Enhance Plugin without using Maven Invoker
* [HHH-19815](https://hibernate.atlassian.net/browse/HHH-19815) - Review and Complete the documentation of the Enhance Plugin in the User's Guide
* [HHH-19814](https://hibernate.atlassian.net/browse/HHH-19814) - Document the Enhance Plugin changes in the Migration Guide
* [HHH-19336](https://hibernate.atlassian.net/browse/HHH-19336) - Proper implementation for JPA PessimisticLockScope

### Task
* [HHH-19812](https://hibernate.atlassian.net/browse/HHH-19812) - Improvements on the Maven Enhance Plugin
* [HHH-19811](https://hibernate.atlassian.net/browse/HHH-19811) - Upgrade H2 to 2.4.240
* [HHH-19728](https://hibernate.atlassian.net/browse/HHH-19728) - Update SQL Server CI testing to 2025
* [HHH-19726](https://hibernate.atlassian.net/browse/HHH-19726) - Update MariaDB CI testing to 12.0
* [HHH-19009](https://hibernate.atlassian.net/browse/HHH-19009) - Correction of the inheritance syntax used with GraphParser in EntityGraphsTest tests
* [HHH-19793](https://hibernate.atlassian.net/browse/HHH-19793) - Temporary disconnect ATP-S from OTP
* [HHH-19806](https://hibernate.atlassian.net/browse/HHH-19806) - Make LoggingReportTask more resilient to missing annotation values


## 7.1.0.Final (August 08, 2025)

[Full changelog](https://hibernate.atlassian.net/projects/HHH/versions/34726)

### Improvement
* [HHH-19693](https://hibernate.atlassian.net/browse/HHH-19693) - Upgrade Oracle JDBC driver to version 23.9.0.25.07

### New Feature
* [HHH-19682](https://hibernate.atlassian.net/browse/HHH-19682) - Add Support for GaussDB Lock Timeout (lockwait_timeout)


## 7.1.0.CR2 (August 06, 2025)

[Full changelog](https://hibernate.atlassian.net/projects/HHH/versions/34594)

### Bug
* [HHH-19673](https://hibernate.atlassian.net/browse/HHH-19673) - hibernate-gradle-plugin 7.0.0.CR1 wrongly requires Java 21+
* [HHH-19669](https://hibernate.atlassian.net/browse/HHH-19669) - hql insert fails for entity with join inheritance on Oracle

### Improvement
* [HHH-19686](https://hibernate.atlassian.net/browse/HHH-19686) - Add implementation dependency to agroal-pool from hibernate-agroal
* [HHH-19685](https://hibernate.atlassian.net/browse/HHH-19685) - Rework multi-table handling code to allow caching and help Hibernate Reactive
* [HHH-19672](https://hibernate.atlassian.net/browse/HHH-19672) - Add overloads of #find accepting entity-name

### Task
* [HHH-19683](https://hibernate.atlassian.net/browse/HHH-19683) - Add method in AbstractEntityPersister to check if field name is not lazy
* [HHH-19670](https://hibernate.atlassian.net/browse/HHH-19670) - Upgrade MySQL testing to 9.4


## 7.1.0.CR1 (July 30, 2025)

[Full changelog](https://hibernate.atlassian.net/projects/HHH/versions/32362)

### Bug
* [HHH-19649](https://hibernate.atlassian.net/browse/HHH-19649) - QualifiedNameParser inconsistent between parsing and formatting
* [HHH-19645](https://hibernate.atlassian.net/browse/HHH-19645) - Some database specific tests are missing RequiresDialect annotation
* [HHH-19640](https://hibernate.atlassian.net/browse/HHH-19640) - @UpdateTimestamp behavior change between Hibernate 5.6 and 6.6
* [HHH-19631](https://hibernate.atlassian.net/browse/HHH-19631) - NPE in AbstractPersistentCollection because "persister" is null, when accessing unfetched lazy collection in event
* [HHH-19626](https://hibernate.atlassian.net/browse/HHH-19626) - Hibernate processor may fail to process entities with generics
* [HHH-19616](https://hibernate.atlassian.net/browse/HHH-19616) - @ManyToOne(optional=false) results in a not null constraint for SINGLE_TABLE subclass
* [HHH-19604](https://hibernate.atlassian.net/browse/HHH-19604) - Session.isDirty always return true when @Immutable entity is loaded
* [HHH-19585](https://hibernate.atlassian.net/browse/HHH-19585) - Object relationship mapping issues | java.lang.NullPointerException: Cannot invoke "java.lang.Comparable.compareTo(Object)" because "one" is null
* [HHH-19583](https://hibernate.atlassian.net/browse/HHH-19583) - JsonJavaType is missing parameterized info on the type arguments
* [HHH-19579](https://hibernate.atlassian.net/browse/HHH-19579) - Criteria update join - Column 'code' in SET is ambiguous
* [HHH-19578](https://hibernate.atlassian.net/browse/HHH-19578) - @Delete queries for reactive repositories
* [HHH-19574](https://hibernate.atlassian.net/browse/HHH-19574) - metamodel population for inner classes
* [HHH-19572](https://hibernate.atlassian.net/browse/HHH-19572) - NullPointerException when using where(List<Predicate> restrictions) method
* [HHH-19571](https://hibernate.atlassian.net/browse/HHH-19571) - CloningPropertyCall causes non-deterministic bytecode for AccessOptimizer
* [HHH-19570](https://hibernate.atlassian.net/browse/HHH-19570) - HQL with jpamodelgen fails compilation when querying by natural id named `id`
* [HHH-19549](https://hibernate.atlassian.net/browse/HHH-19549) - Hibernate Processor: When embeddable is annotated by @Access enclosing element must not change access type
* [HHH-19547](https://hibernate.atlassian.net/browse/HHH-19547) - Misleading exception message at DefaultFlushEntityEventListener - mangled ID - misplaced Entity and EntityEntry ID
* [HHH-19533](https://hibernate.atlassian.net/browse/HHH-19533) - Implement equals() and hashCode() for NativeQueryConstructorTransformer
* [HHH-19531](https://hibernate.atlassian.net/browse/HHH-19531) - Jakarta Data implementation casts StatlessSession to *Implementor interfaces
* [HHH-19529](https://hibernate.atlassian.net/browse/HHH-19529) - Check bytecode generated classes with stable names class loaders
* [HHH-19528](https://hibernate.atlassian.net/browse/HHH-19528) - Version xml mapping is ignored
* [HHH-19523](https://hibernate.atlassian.net/browse/HHH-19523) - NPE when using stateless session and having a PreCollectionRecreateEventListener registered
* [HHH-19522](https://hibernate.atlassian.net/browse/HHH-19522) - upserts fail silently instead of throwing StaleObjectStateException
* [HHH-19495](https://hibernate.atlassian.net/browse/HHH-19495) - [Hibernate 7] - Extra update when mixing entity annotation with XML when a collection is dirty
* [HHH-19457](https://hibernate.atlassian.net/browse/HHH-19457) - Inheritance with type JOINED not working in a related entity
* [HHH-19396](https://hibernate.atlassian.net/browse/HHH-19396) - Cannot select the same column twice (with different aliases) while using CTE
* [HHH-19391](https://hibernate.atlassian.net/browse/HHH-19391) - Query plan caching for criteria queries fails with entity parameters
* [HHH-19368](https://hibernate.atlassian.net/browse/HHH-19368) - Group by and single-table inheritance sub-select query error
* [HHH-19261](https://hibernate.atlassian.net/browse/HHH-19261) - OracleDialect getQueryHintString incorrectly joins supplied hints
* [HHH-19168](https://hibernate.atlassian.net/browse/HHH-19168) - Disallow re-enhancement of entities with different configuration
* [HHH-19031](https://hibernate.atlassian.net/browse/HHH-19031) - Loading an Entity a second time when it contains an embedded object causes IllegalArgumentException
* [HHH-18936](https://hibernate.atlassian.net/browse/HHH-18936) - remove parent with @OnDelete(CASCADE) leads to TransientObjectException
* [HHH-18909](https://hibernate.atlassian.net/browse/HHH-18909) - NPE for cached entity with array
* [HHH-18837](https://hibernate.atlassian.net/browse/HHH-18837) - Oracle epoch extraction doesn't work with dates
* [HHH-18820](https://hibernate.atlassian.net/browse/HHH-18820) - QueryInterpretationCache and criteria SQM trees
* [HHH-18818](https://hibernate.atlassian.net/browse/HHH-18818) - CteInsertHandler doesn't update PooledOptimizer after batch inserting
* [HHH-18473](https://hibernate.atlassian.net/browse/HHH-18473) - Default target column qualifier support error
* [HHH-18311](https://hibernate.atlassian.net/browse/HHH-18311) - No longer able to configure which SqmMultiTableInsertStrategy/SqmMultiTableMutationStrategy to use
* [HHH-11866](https://hibernate.atlassian.net/browse/HHH-11866) - CustomEntityDirtinessStrategy#resetDirty is not called after entity initialization
* [HHH-9812](https://hibernate.atlassian.net/browse/HHH-9812) - @Formula with SQL cast function
* [HHH-8535](https://hibernate.atlassian.net/browse/HHH-8535) - Generating an ID with org.hibernate.id.enhanced.TableGenerator can hang the application if HIBERNATE_SEQUENCES has NULL value

### Deprecation
* [HHH-19661](https://hibernate.atlassian.net/browse/HHH-19661) - Deprecate enhancement support for "extended" enhancement
* [HHH-19660](https://hibernate.atlassian.net/browse/HHH-19660) - Deprecate enhancement support for automatic association management 
* [HHH-19500](https://hibernate.atlassian.net/browse/HHH-19500) - layer-breakers in UserType
* [HHH-19483](https://hibernate.atlassian.net/browse/HHH-19483) - deprecate EntityEntryFactory
* [HHH-19015](https://hibernate.atlassian.net/browse/HHH-19015) - Deprecate Session#byId in favor of FindOptions

### Improvement
* [HHH-19668](https://hibernate.atlassian.net/browse/HHH-19668) - Reduce size of collections held in ParameterInterpretationImpl before storing them in the cache
* [HHH-19667](https://hibernate.atlassian.net/browse/HHH-19667) - Support H2 2.2.220 for update clause enhancements
* [HHH-19663](https://hibernate.atlassian.net/browse/HHH-19663) - Add arrayToString/collectionToString variant that accepts default element
* [HHH-19662](https://hibernate.atlassian.net/browse/HHH-19662) - Define granularity of EnhancementContext options matching reality
* [HHH-19654](https://hibernate.atlassian.net/browse/HHH-19654) - Upgrade hibernate-models to 1.0.1
* [HHH-19653](https://hibernate.atlassian.net/browse/HHH-19653) - Log JDBC fetch size
* [HHH-19646](https://hibernate.atlassian.net/browse/HHH-19646) - Improve validation of configuration settings for the Query Plan Cache
* [HHH-19644](https://hibernate.atlassian.net/browse/HHH-19644) - Enforce usage of SharedDriverManagerConnectionProviderImpl connection pool for hibernate-envers tests
* [HHH-19643](https://hibernate.atlassian.net/browse/HHH-19643) - Redirect some hibernate-envers tests to H2 as they use an H2 configured hibernate configuration file
* [HHH-19641](https://hibernate.atlassian.net/browse/HHH-19641) - Upgrade to JUnit 5.13.4
* [HHH-19638](https://hibernate.atlassian.net/browse/HHH-19638) - Document the Ant enhancement task in User Guide tooling section
* [HHH-19632](https://hibernate.atlassian.net/browse/HHH-19632) - Give access to optional parameter info in SqlAstTranslator
* [HHH-19623](https://hibernate.atlassian.net/browse/HHH-19623) - Support GaussDB column check function
* [HHH-19622](https://hibernate.atlassian.net/browse/HHH-19622) - Include hibernate-vector in the platform pom 
* [HHH-19619](https://hibernate.atlassian.net/browse/HHH-19619) - Add TeradataDialect
* [HHH-19617](https://hibernate.atlassian.net/browse/HHH-19617) - Remove EXECUTE ANY TYPE privilege for Oracle
* [HHH-19612](https://hibernate.atlassian.net/browse/HHH-19612) - fetchgraph and loadgraph hints should accept the name of a named EntityGraph
* [HHH-19611](https://hibernate.atlassian.net/browse/HHH-19611) - logging cleanup
* [HHH-19608](https://hibernate.atlassian.net/browse/HHH-19608) - automatically include the @PartitionKey in the primary key constraint
* [HHH-19598](https://hibernate.atlassian.net/browse/HHH-19598) - Support GaussDB procedure
* [HHH-19595](https://hibernate.atlassian.net/browse/HHH-19595) - Implement upsert support for HSQLDB and EDB (v >= 15)
* [HHH-19593](https://hibernate.atlassian.net/browse/HHH-19593) - ResourceRegistryStandardImpl triggers identity hashcode on Statements and ResultSets
* [HHH-19591](https://hibernate.atlassian.net/browse/HHH-19591) - Refactor verifyMeterNotFoundException to use assertThrows
* [HHH-19553](https://hibernate.atlassian.net/browse/HHH-19553) - Remove CREATE ANY INDEX privilege for Oracle
* [HHH-19544](https://hibernate.atlassian.net/browse/HHH-19544) - Allow to plug-in a third party cache implementation for internal caches
* [HHH-19537](https://hibernate.atlassian.net/browse/HHH-19537) - Simplify amount of computations performed during LazyAttributeLoadingInterceptor ctor
* [HHH-19536](https://hibernate.atlassian.net/browse/HHH-19536) - Reduce computations during EnhancementAsProxyLazinessInterceptor constructor
* [HHH-19532](https://hibernate.atlassian.net/browse/HHH-19532) - Add unwrap() method to StatelessSession/SharedSessionContract
* [HHH-19526](https://hibernate.atlassian.net/browse/HHH-19526) - Avoid generating a QueryInterpretationCache.Key when the query cache is disabled
* [HHH-19508](https://hibernate.atlassian.net/browse/HHH-19508) - order collection updates to put deletes first
* [HHH-19506](https://hibernate.atlassian.net/browse/HHH-19506) - General Firebird dialect improvements (main)
* [HHH-19503](https://hibernate.atlassian.net/browse/HHH-19503) - Track a Dialect's level of support for locking joined tables
* [HHH-19501](https://hibernate.atlassian.net/browse/HHH-19501) - Session#lock w/ pessimistic locks for scopes
* [HHH-19494](https://hibernate.atlassian.net/browse/HHH-19494) - MERGE on DB2
* [HHH-19487](https://hibernate.atlassian.net/browse/HHH-19487) - AbstractEntityPersister should not be directly involved in dirty checking
* [HHH-19484](https://hibernate.atlassian.net/browse/HHH-19484) - simpler customization of ImplicitNamingStrategyJpaCompliantImpl
* [HHH-19466](https://hibernate.atlassian.net/browse/HHH-19466) - exposure of SPI types via JpaMetamodel
* [HHH-19362](https://hibernate.atlassian.net/browse/HHH-19362) - Improve JsonHelper to handle more org.hibernate.metamodel.mapping types for serialization
* [HHH-19297](https://hibernate.atlassian.net/browse/HHH-19297) - Register json functions in SingleStore community dialect
* [HHH-19283](https://hibernate.atlassian.net/browse/HHH-19283) - Hibernate Gradle Plugin configuration cache support
* [HHH-18613](https://hibernate.atlassian.net/browse/HHH-18613) - @View optional column names
* [HHH-17751](https://hibernate.atlassian.net/browse/HHH-17751) - Support force-increment locking with database-generated version timestamp 
* [HHH-17404](https://hibernate.atlassian.net/browse/HHH-17404) - Support reading/writing from/to special source/target in FormatMapper 
* [HHH-16548](https://hibernate.atlassian.net/browse/HHH-16548) - allow directory/archive scanning via Configuration API
* [HHH-16283](https://hibernate.atlassian.net/browse/HHH-16283) - Integrate ParameterMarkerStrategy into NativeQuery

### New Feature
* [HHH-19652](https://hibernate.atlassian.net/browse/HHH-19652) - GaussDB locking support
* [HHH-19614](https://hibernate.atlassian.net/browse/HHH-19614) - check constraint to enforce correct nullability in single table mappings
* [HHH-19587](https://hibernate.atlassian.net/browse/HHH-19587) - Easier access to LobHelper
* [HHH-19580](https://hibernate.atlassian.net/browse/HHH-19580) - allow SchemaManager to target a specified schema/catalog
* [HHH-19559](https://hibernate.atlassian.net/browse/HHH-19559) - built-in implementation of schema-based multitenancy, using Connection.setSchema()
* [HHH-19535](https://hibernate.atlassian.net/browse/HHH-19535) - Interceptor and merge()
* [HHH-19493](https://hibernate.atlassian.net/browse/HHH-19493) - criteria support for id() and version() functions
* [HHH-19459](https://hibernate.atlassian.net/browse/HHH-19459) - Locking scope and follow-on
* [HHH-19365](https://hibernate.atlassian.net/browse/HHH-19365) - Add GaussDB dialect support 
* [HHH-18708](https://hibernate.atlassian.net/browse/HHH-18708) - HQL string() function

### Remove Feature
* [HHH-17889](https://hibernate.atlassian.net/browse/HHH-17889) - Remove support for database versions that are unsupported by vendors 7.1 edition

### Sub-task
* [HHH-19226](https://hibernate.atlassian.net/browse/HHH-19226) - Remove support for SQL Server versions older than 2014
* [HHH-19225](https://hibernate.atlassian.net/browse/HHH-19225) - Remove support for MariaDB versions older than 10.6
* [HHH-18641](https://hibernate.atlassian.net/browse/HHH-18641) - Remove support for DB2i versions older than 7.2
* [HHH-18639](https://hibernate.atlassian.net/browse/HHH-18639) - Remove support for DB2 versions older than 11.1
* [HHH-18366](https://hibernate.atlassian.net/browse/HHH-18366) - Informix concat pipe operator error

### Task
* [HHH-19639](https://hibernate.atlassian.net/browse/HHH-19639) - Add getter for EmbeddableFetchImpl#nullIndicatorResult
* [HHH-19603](https://hibernate.atlassian.net/browse/HHH-19603) - Update the links to Configurations.adoc in the reference guide
* [HHH-19548](https://hibernate.atlassian.net/browse/HHH-19548) - Upgrade to ByteBuddy 1.17.5
* [HHH-19518](https://hibernate.atlassian.net/browse/HHH-19518) - Make hibernate-maven-plugin configuration parameters non-readonly
* [HHH-19504](https://hibernate.atlassian.net/browse/HHH-19504) - Javadoc error SecondaryRow#owned
* [HHH-19496](https://hibernate.atlassian.net/browse/HHH-19496) - Use Javadoc styles from hibernate-asciidoctor-theme
* [HHH-19309](https://hibernate.atlassian.net/browse/HHH-19309) - Switch to Central Publishing Portal API for publishing to Maven Central


## 7.0.0.Final (May 19, 2025)

[Full changelog](https://hibernate.atlassian.net/projects/HHH/versions/33439)

### Task
* [HHH-19474](https://hibernate.atlassian.net/browse/HHH-19474) - Release 7.0


## 7.0.0.CR2 (May 14, 2025)

[Full changelog](https://hibernate.atlassian.net/projects/HHH/versions/33340)

### Bug
* [HHH-19425](https://hibernate.atlassian.net/browse/HHH-19425) - incorrect class literals in Processor-generated code
* [HHH-19389](https://hibernate.atlassian.net/browse/HHH-19389) - use of @Struct on databases without UDTs
* [HHH-19386](https://hibernate.atlassian.net/browse/HHH-19386) - MutationSepectification#getResultType should return null

### Deprecation
* [HHH-19440](https://hibernate.atlassian.net/browse/HHH-19440) - Deprecate exposing of LockOptions

### Improvement
* [HHH-19460](https://hibernate.atlassian.net/browse/HHH-19460) - mis-named <embedded-id/> leads to NPE
* [HHH-19456](https://hibernate.atlassian.net/browse/HHH-19456) - Upgrade to hibernate-models 1.0.0.CR3
* [HHH-19449](https://hibernate.atlassian.net/browse/HHH-19449) - how does client obtain a BindableType
* [HHH-19448](https://hibernate.atlassian.net/browse/HHH-19448) - API/SPI split for BindableType/BindingContext
* [HHH-19447](https://hibernate.atlassian.net/browse/HHH-19447) - org.hibernate.query.procedure
* [HHH-19445](https://hibernate.atlassian.net/browse/HHH-19445) - methods of ProcedureCall accept BasicTypeReference
* [HHH-19444](https://hibernate.atlassian.net/browse/HHH-19444) - [SQLiteDialect] Fix ViolatedConstraintNameExtractor
* [HHH-19442](https://hibernate.atlassian.net/browse/HHH-19442) - ProcedureCall should not extend NameableQuery
* [HHH-19438](https://hibernate.atlassian.net/browse/HHH-19438) - move OutputableType
* [HHH-19428](https://hibernate.atlassian.net/browse/HHH-19428) - Support @ListIndexBase in mapping.xml
* [HHH-19422](https://hibernate.atlassian.net/browse/HHH-19422) - Introduce @CollectionIdJavaClass
* [HHH-19420](https://hibernate.atlassian.net/browse/HHH-19420) - Support batch-size for collections in mapping.xml
* [HHH-19399](https://hibernate.atlassian.net/browse/HHH-19399) - setting to enable logging of SQLExceptions
* [HHH-19397](https://hibernate.atlassian.net/browse/HHH-19397) - LIMIT clause does not work without ORDER BY clause
* [HHH-19324](https://hibernate.atlassian.net/browse/HHH-19324) - Switch tests using hbm.xml to use mapping.xml
* [HHH-19310](https://hibernate.atlassian.net/browse/HHH-19310) - Simplified declaration of type for basic mappings in XML
* [HHH-19299](https://hibernate.atlassian.net/browse/HHH-19299) - <element-collection/> with LIST classification interpreted as BAG
* [HHH-19209](https://hibernate.atlassian.net/browse/HHH-19209) - Verify and fix ID class generation for inner classes

### New Feature
* [HHH-19450](https://hibernate.atlassian.net/browse/HHH-19450) - Have processor generate EnabledFetchProfile for all discovered profiles


## 7.0.0.CR1 (April 24, 2025)

[Full changelog](https://hibernate.atlassian.net/projects/HHH/versions/33078)

### Bug
* [HHH-19375](https://hibernate.atlassian.net/browse/HHH-19375) - fix check for presence of Quarkus in reactive case
* [HHH-19374](https://hibernate.atlassian.net/browse/HHH-19374) - repositories should always be @Dependent
* [HHH-19345](https://hibernate.atlassian.net/browse/HHH-19345) - EntityManager#remove checks for entity state are too strict
* [HHH-19334](https://hibernate.atlassian.net/browse/HHH-19334) - CCE arising from tuple passed to HQL position() function
* [HHH-19330](https://hibernate.atlassian.net/browse/HHH-19330) - Error in lockstring generation in PostgreSQL 
* [HHH-19320](https://hibernate.atlassian.net/browse/HHH-19320) - Assigned id value is not passed into BeforeExecutionGenerator#generate() method when allowAssignedIdentifiers() is true and id has been assigned
* [HHH-19318](https://hibernate.atlassian.net/browse/HHH-19318) - follow-on locking and StatelessSession
* [HHH-19314](https://hibernate.atlassian.net/browse/HHH-19314) - StackOverflowException when using onConflict with createCriteriaInsertValues and createCriteriaInsertSelect
* [HHH-19306](https://hibernate.atlassian.net/browse/HHH-19306) - Composite generator may not respect the event types of generators it consits of
* [HHH-19301](https://hibernate.atlassian.net/browse/HHH-19301) - Must import FQCN when generating metamodel class for inner Jakarta Data repository interface
* [HHH-19291](https://hibernate.atlassian.net/browse/HHH-19291) - Expressions.nullExpresion() in querydsl result in NPE in SqmExpressible with named parameters
* [HHH-19280](https://hibernate.atlassian.net/browse/HHH-19280) - ResourceRegistryStandardImpl#close(java.sql.Statement) is called on already closed statements
* [HHH-19279](https://hibernate.atlassian.net/browse/HHH-19279) - @Basic not implicit optional
* [HHH-19248](https://hibernate.atlassian.net/browse/HHH-19248) - Return of deleted entities doesn't work with naturalid multiloading
* [HHH-19208](https://hibernate.atlassian.net/browse/HHH-19208) - Javadoc of org.hibernate.cfg.QuerySettings.QUERY_PLAN_CACHE_ENABLED mentions that the query plan cache is disabled by default, but it is enabled by default
* [HHH-19207](https://hibernate.atlassian.net/browse/HHH-19207) - JPA OrderBy annotated relation not ordered when using entity graph with criteria api
* [HHH-19059](https://hibernate.atlassian.net/browse/HHH-19059) - Bytecode enhancement fails when inherited fields are mapped using property access in subclass
* [HHH-18991](https://hibernate.atlassian.net/browse/HHH-18991) - Restrictions should use JDBC parameters
* [HHH-18920](https://hibernate.atlassian.net/browse/HHH-18920) - Enum parameters in Jakarta Data repository method return type constructor are not properly matched
* [HHH-18745](https://hibernate.atlassian.net/browse/HHH-18745) - Unnecessary joins when use TREAT operator 
* [HHH-14694](https://hibernate.atlassian.net/browse/HHH-14694) - Use stable proxy names to avoid managing proxy state and memory leaks
* [HHH-9127](https://hibernate.atlassian.net/browse/HHH-9127) - L2 cache stores stale data when an entity is locked with OPTIMISTIC_FORCE_INCREMENT lock type

### Deprecation
* [HHH-19357](https://hibernate.atlassian.net/browse/HHH-19357) - deprecate hibernate.discard_pc_on_close

### Improvement
* [HHH-19378](https://hibernate.atlassian.net/browse/HHH-19378) - find by multiple ids with EntityGraph
* [HHH-19364](https://hibernate.atlassian.net/browse/HHH-19364) - Introduce QuerySpecification
* [HHH-19358](https://hibernate.atlassian.net/browse/HHH-19358) - Add a "What's New" document for series
* [HHH-19352](https://hibernate.atlassian.net/browse/HHH-19352) - move legacy LimitHandlers to community dialects module
* [HHH-19350](https://hibernate.atlassian.net/browse/HHH-19350) - SessionBuilder exposes SPI types
* [HHH-19349](https://hibernate.atlassian.net/browse/HHH-19349) - rework ImmutableEntityUpdateQueryHandlingMode and immutable_entity_update_query_handling_mode
* [HHH-19340](https://hibernate.atlassian.net/browse/HHH-19340) - Make TypedParameterValue a record
* [HHH-19325](https://hibernate.atlassian.net/browse/HHH-19325) - Upgrade to Jandex 3.3.0
* [HHH-19317](https://hibernate.atlassian.net/browse/HHH-19317) - Mark org.hibernate.boot.models as incubating
* [HHH-19300](https://hibernate.atlassian.net/browse/HHH-19300) - more ConstraintKinds
* [HHH-19286](https://hibernate.atlassian.net/browse/HHH-19286) - Ignoring auto-applied conversions on special mappings
* [HHH-19284](https://hibernate.atlassian.net/browse/HHH-19284) - Extract Duplicated Vector Function Registration Logic
* [HHH-19278](https://hibernate.atlassian.net/browse/HHH-19278) - fixes to logic in MultiIdEntityLoaders
* [HHH-19096](https://hibernate.atlassian.net/browse/HHH-19096) - Adjust `SelectionQuery#setEntityGraph(..)` to accept entity graphs of supertypes
* [HHH-19001](https://hibernate.atlassian.net/browse/HHH-19001) - Map ConstraintType to UNIQUE on ConstraintViolationException
* [HHH-18896](https://hibernate.atlassian.net/browse/HHH-18896) - Use binary_float/binary_double on Oracle for Java float/double
* [HHH-18008](https://hibernate.atlassian.net/browse/HHH-18008) - Ability to clear persistence context for a specific type
* [HHH-17002](https://hibernate.atlassian.net/browse/HHH-17002) - Query plan caching for CriteriaQuery based on query structure
* [HHH-16972](https://hibernate.atlassian.net/browse/HHH-16972) - Reorganize parts of org.hibernate.query.sqm

### New Feature
* [HHH-19327](https://hibernate.atlassian.net/browse/HHH-19327) - overload SF.addNamedQuery() to take TypedQuery and return TypedQueryReference
* [HHH-19319](https://hibernate.atlassian.net/browse/HHH-19319) - StatelessSession.findMultiple() accepting a LockMode
* [HHH-19303](https://hibernate.atlassian.net/browse/HHH-19303) - validate @Id fields against @IdClass in Processor
* [HHH-19298](https://hibernate.atlassian.net/browse/HHH-19298) - add convenience overloads of StatelessSession.get() which default GraphSemantic.LOAD
* [HHH-19296](https://hibernate.atlassian.net/browse/HHH-19296) - overload createSelectionQuery() to accept an EntityGraph instead of a result class
* [HHH-19115](https://hibernate.atlassian.net/browse/HHH-19115) - Implement support for ordered loading by multiple natural-id values
* [HHH-18563](https://hibernate.atlassian.net/browse/HHH-18563) - Add foreign key target tables to affected tables (update query set-clause)
* [HHH-16643](https://hibernate.atlassian.net/browse/HHH-16643) - @NamedFetchGraph annotation


## 7.0.0.Beta5 (March 21, 2025)

[Full changelog](https://hibernate.atlassian.net/projects/HHH/versions/32581)

### Bug
* [HHH-19266](https://hibernate.atlassian.net/browse/HHH-19266) - inconsistencies in ScrollableResults
* [HHH-19259](https://hibernate.atlassian.net/browse/HHH-19259) - Static metamodel for id/timestamp not set (and triggers warnings) for DefaultRevisionEntity and similar
* [HHH-19258](https://hibernate.atlassian.net/browse/HHH-19258) - Remove @Entity annotation from default revision entities contributed internally by Envers
* [HHH-19254](https://hibernate.atlassian.net/browse/HHH-19254) - The return value of st_envelope() is not recognised as a geometry type on MariaDB 
* [HHH-19246](https://hibernate.atlassian.net/browse/HHH-19246) - Fetch join makes partially covered EntityGraph ineffective
* [HHH-19232](https://hibernate.atlassian.net/browse/HHH-19232) - BeanValidationEventListener not called if only associated collection is updated via getter
* [HHH-19227](https://hibernate.atlassian.net/browse/HHH-19227) - errors in class OracleSDOFunctionDescriptors
* [HHH-19220](https://hibernate.atlassian.net/browse/HHH-19220) - ClassCastException: class org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer$1 cannot be cast to class java.lang.String
* [HHH-19206](https://hibernate.atlassian.net/browse/HHH-19206) - Bytecode-enhanced dirty checking ineffective if entity's embedded ID set manually (to same value)
* [HHH-19195](https://hibernate.atlassian.net/browse/HHH-19195) - Embeddable inheritance: discriminator values are not hierarchically ordered
* [HHH-19173](https://hibernate.atlassian.net/browse/HHH-19173) - PostgreSQLLegacySqlAstTranslator does not implement visitInArrayPredicates
* [HHH-19143](https://hibernate.atlassian.net/browse/HHH-19143) - javadoc for 7 links to JPA 3.1 javadoc instead of 3.2
* [HHH-19140](https://hibernate.atlassian.net/browse/HHH-19140) - Enhanced entities with AccessType.PROPERTY does not work well with inheritance
* [HHH-19134](https://hibernate.atlassian.net/browse/HHH-19134) - Hibernate processor - find by id fails for entity with composite identifier with @IdClass
* [HHH-19126](https://hibernate.atlassian.net/browse/HHH-19126) - Plural valued paths should be collection-typed instead of element typed
* [HHH-19118](https://hibernate.atlassian.net/browse/HHH-19118) - The columnDefinition field of joinColumn does not take effect 
* [HHH-19116](https://hibernate.atlassian.net/browse/HHH-19116) - Error when using fk() function on left joined many-to-one association and is null predicate
* [HHH-19110](https://hibernate.atlassian.net/browse/HHH-19110) - Flush operation fails with "UnsupportedOperationException: compare() not implemented for EntityType"
* [HHH-19109](https://hibernate.atlassian.net/browse/HHH-19109) - Hibernate Data Repositories are @RequestScoped
* [HHH-19097](https://hibernate.atlassian.net/browse/HHH-19097) - CoercionException for empty CHAR(1) from MySQL when used with MySQLLegacyDialect
* [HHH-19091](https://hibernate.atlassian.net/browse/HHH-19091) - Nested entity classes not properly handled by jpamodelgen
* [HHH-19005](https://hibernate.atlassian.net/browse/HHH-19005) - High memory usage for JSON string literals in BasicFormatterImpl
* [HHH-18946](https://hibernate.atlassian.net/browse/HHH-18946) - Startup issues with HANA in failover situations
* [HHH-18780](https://hibernate.atlassian.net/browse/HHH-18780) - Performance regression on Postgres with polymorphic query due to incorrect casts of null columns
* [HHH-17151](https://hibernate.atlassian.net/browse/HHH-17151) - NPE when binding null parameter in native query with explicit TemporalType
* [HHH-11801](https://hibernate.atlassian.net/browse/HHH-11801) - AbstractPersistentCollection.SetProxy does not implement equals()

### Deprecation
* [HHH-19274](https://hibernate.atlassian.net/browse/HHH-19274) - Deprecate MetadataBuilder#applyIndexView and friends
* [HHH-19265](https://hibernate.atlassian.net/browse/HHH-19265) - deprecate hibernate.jdbc.use_scrollable_resultset
* [HHH-19253](https://hibernate.atlassian.net/browse/HHH-19253) - deprecate use of lifecycle callbacks on embeddables
* [HHH-19063](https://hibernate.atlassian.net/browse/HHH-19063) - Drop forms of SchemaNameResolver performing reflection

### Improvement
* [HHH-19271](https://hibernate.atlassian.net/browse/HHH-19271) - support HINT_FETCH_PROFILE in SelectionQuery
* [HHH-19260](https://hibernate.atlassian.net/browse/HHH-19260) - Move feature supports methods from the SqlAstTranslator base impl to Dialect
* [HHH-19252](https://hibernate.atlassian.net/browse/HHH-19252) - overriding @Id generation declared by @MappedSuperclass
* [HHH-19223](https://hibernate.atlassian.net/browse/HHH-19223) - Upgrade JBoss Logging Tools (processor) to 3.0.4.Final
* [HHH-19219](https://hibernate.atlassian.net/browse/HHH-19219) - Informix Catalog and schema support
* [HHH-19210](https://hibernate.atlassian.net/browse/HHH-19210) - Propagate exceptions from building a ValidatorFactory
* [HHH-19205](https://hibernate.atlassian.net/browse/HHH-19205) - Do not recreate the validator on each BeanValidationEventListener#validate call
* [HHH-19196](https://hibernate.atlassian.net/browse/HHH-19196) - Upgrade to JUnit 5.12.0
* [HHH-19145](https://hibernate.atlassian.net/browse/HHH-19145) - Relicense Hibernate ORM under ASL
* [HHH-19142](https://hibernate.atlassian.net/browse/HHH-19142) - StatelessSession.findMultiple() and second-level cache
* [HHH-19089](https://hibernate.atlassian.net/browse/HHH-19089) - Lower collection pre-sizing limit to avoid excessive memory usage
* [HHH-18724](https://hibernate.atlassian.net/browse/HHH-18724) - Support Hibernate Validator @ConstraintComposition(OR) for 'not null' DDL generation
* [HHH-18723](https://hibernate.atlassian.net/browse/HHH-18723) - Support @SQLRestriction in class marked as @MappedSuperclass
* [HHH-18482](https://hibernate.atlassian.net/browse/HHH-18482) - Provide access to a mutable ClassDetailsRegistry from the Integrator
* [HHH-17325](https://hibernate.atlassian.net/browse/HHH-17325) - @SoftDelete with timestamp
* [HHH-15271](https://hibernate.atlassian.net/browse/HHH-15271) - Don't initialize lazy associations via merge
* [HHH-4396](https://hibernate.atlassian.net/browse/HHH-4396) - Ability to patternize embedded column names

### New Feature
* [HHH-19237](https://hibernate.atlassian.net/browse/HHH-19237) - Expand graph language to optionally specify entity
* [HHH-19217](https://hibernate.atlassian.net/browse/HHH-19217) - Expose GraphParser#parse on SessionFactory
* [HHH-19216](https://hibernate.atlassian.net/browse/HHH-19216) - NamedEntityGraph annotation supporting Hibernate parseable format


## 7.0.0.Beta4 (February 12, 2025)

[Full changelog](https://hibernate.atlassian.net/projects/HHH/versions/32482)

### Bug
* [HHH-19107](https://hibernate.atlassian.net/browse/HHH-19107) - Entities with @EmbeddedId not supported with CrudRepository
* [HHH-19106](https://hibernate.atlassian.net/browse/HHH-19106) - @Transaction(TxType) not working with Hibernate Data Repositories
* [HHH-19104](https://hibernate.atlassian.net/browse/HHH-19104) - Envers is keeping references to classes and thus classloaders
* [HHH-19079](https://hibernate.atlassian.net/browse/HHH-19079) - ComponentType.replace can cause ArrayIndexOutOfBoundsException when used with embeddable inheritance
* [HHH-19072](https://hibernate.atlassian.net/browse/HHH-19072) - The hibernate.session_factory_name configuration property no longer works in Hibernate 7.0.0
* [HHH-19069](https://hibernate.atlassian.net/browse/HHH-19069) - Performance regression for wide inheritance models
* [HHH-19034](https://hibernate.atlassian.net/browse/HHH-19034) - Wrong reuse of a Join 
* [HHH-19033](https://hibernate.atlassian.net/browse/HHH-19033) - Move of Derby to community dialects is incomplete
* [HHH-19017](https://hibernate.atlassian.net/browse/HHH-19017) - Class Cast Exception for PersistentAttributeInterceptable
* [HHH-19011](https://hibernate.atlassian.net/browse/HHH-19011) - @ElementCollection comment overrides class level comment on an Entity
* [HHH-19004](https://hibernate.atlassian.net/browse/HHH-19004) - TenantId regression on @EmbeddedId 6.6.0 -> 6.6.1
* [HHH-18992](https://hibernate.atlassian.net/browse/HHH-18992) - Locking does not work with multiLoad
* [HHH-18988](https://hibernate.atlassian.net/browse/HHH-18988) - Embeddable inheritance + default_schema results in NPE at startup
* [HHH-18974](https://hibernate.atlassian.net/browse/HHH-18974) - UuidVersion6Strategy/UuidVersion7Strategy use random seed in static init + constructor
* [HHH-18968](https://hibernate.atlassian.net/browse/HHH-18968) - MySQLDialect wrongly uses Timestamp as type for localtime function
* [HHH-18961](https://hibernate.atlassian.net/browse/HHH-18961) - JtaIsolationDelegate, obtaining connection : NPE when SQLExceptionConversionDelegate#convert returns null
* [HHH-18949](https://hibernate.atlassian.net/browse/HHH-18949) - Hibernate Processor should not insert underscores within uppercase names
* [HHH-18945](https://hibernate.atlassian.net/browse/HHH-18945) - Hibernate Processor - fails if entity extends mapped superclass with same simple class name
* [HHH-18933](https://hibernate.atlassian.net/browse/HHH-18933) - the ordering of the class declaration in persistence.xml seems to affect the metamodel
* [HHH-18932](https://hibernate.atlassian.net/browse/HHH-18932) - Wrongly using FK column instead of PK when using joined alias
* [HHH-18912](https://hibernate.atlassian.net/browse/HHH-18912) - ORM release process
* [HHH-18904](https://hibernate.atlassian.net/browse/HHH-18904) - Bytecode Enhancement fails with UnsupportedEnhancementStrategy.FAIL for pre-persist method
* [HHH-18903](https://hibernate.atlassian.net/browse/HHH-18903) - Bytecode enhancement fails for entities that contain a method named get
* [HHH-18901](https://hibernate.atlassian.net/browse/HHH-18901) - AnnotationFormatError: Duplicate annotation for class: interface org.hibernate.bytecode.enhance.spi.EnhancementInfo
* [HHH-18894](https://hibernate.atlassian.net/browse/HHH-18894) - Hibernate 6.6 enum literal is considered field literal instead
* [HHH-18893](https://hibernate.atlassian.net/browse/HHH-18893) - DialectOverrides.SQLRestrictions
* [HHH-18883](https://hibernate.atlassian.net/browse/HHH-18883) - When initializing version attribute to a negative value then a TransientObjectException is thrown when loading an entity
* [HHH-18869](https://hibernate.atlassian.net/browse/HHH-18869) - Schema validation fails with MariaDB when entity field is of type BigDecimal[]
* [HHH-18868](https://hibernate.atlassian.net/browse/HHH-18868) - Wrong behaviour of getAttribute method in impl. of ManagedType when scattered id attributes are used in MappedSuperclass
* [HHH-18867](https://hibernate.atlassian.net/browse/HHH-18867) - beginTransaction() when a tx is already active
* [HHH-18858](https://hibernate.atlassian.net/browse/HHH-18858) - array fields and static metamodel
* [HHH-18819](https://hibernate.atlassian.net/browse/HHH-18819) - Error resolving persistent property of @MapperSuperclass if subtype @Embeddable used as @IdClass
* [HHH-18771](https://hibernate.atlassian.net/browse/HHH-18771) - ListInitializer should consistently consider @ListIndexBase
* [HHH-18750](https://hibernate.atlassian.net/browse/HHH-18750) - @OneToMany with @Any mapped in secondary table KO (ClassCastException)
* [HHH-18693](https://hibernate.atlassian.net/browse/HHH-18693) - Hibernate Processor does not handle inner @Embeddable types
* [HHH-18384](https://hibernate.atlassian.net/browse/HHH-18384) - @JoinColumnsOrFormulas broken
* [HHH-17652](https://hibernate.atlassian.net/browse/HHH-17652) - Cannot invoke "org.hibernate.envers.internal.entities.EntityConfiguration.getRelationDescription(String)" because "entCfg" is null
* [HHH-16883](https://hibernate.atlassian.net/browse/HHH-16883) - EntityGraph.addSubclassSubgraph() throws UnsupportedOperationException
* [HHH-16516](https://hibernate.atlassian.net/browse/HHH-16516) - Adding quoteOnNonIdentifierChar flag to org.hibernate.boot.model.naming.Identifier breaks backwards compatibility
* [HHH-16216](https://hibernate.atlassian.net/browse/HHH-16216) - SybaseASEDialect creates additional not null checks
* [HHH-15848](https://hibernate.atlassian.net/browse/HHH-15848) - session.isDirty() shouldn't throw exception for transient many-to-one object in a session
* [HHH-14725](https://hibernate.atlassian.net/browse/HHH-14725) - Using a InputStream with BlobProxy and Envers results in  java.sql.SQLException: could not reset reader
* [HHH-14519](https://hibernate.atlassian.net/browse/HHH-14519) - Misleading error message when unable to resolve table name
* [HHH-13969](https://hibernate.atlassian.net/browse/HHH-13969) - Fix handling of large varbinary for SAP/Sybase ASE
* [HHH-13915](https://hibernate.atlassian.net/browse/HHH-13915) - Shared state in ByteBuddy basic proxies leads to intermittently broken persistence
* [HHH-13815](https://hibernate.atlassian.net/browse/HHH-13815) - TransientObjectException after merging a bidirectional one-to-many with orphan deletion
* [HHH-13790](https://hibernate.atlassian.net/browse/HHH-13790) - Temporary session not being closed
* [HHH-13612](https://hibernate.atlassian.net/browse/HHH-13612) - Quoted table name in FROM clause and Column in @Formula gets wrongly qualified with generated alias
* [HHH-13377](https://hibernate.atlassian.net/browse/HHH-13377) - Lazy loaded properties of bytecode enhanced entity are left stale after refresh of entity
* [HHH-13243](https://hibernate.atlassian.net/browse/HHH-13243) - Setting @ManyToAny.fetch to FetchType.EAGER doesn't work


## 7.0.0.Beta3 (December 05, 2024)

[Full changelog](https://hibernate.atlassian.net/projects/HHH/versions/32364)

### Bug
* [HHH-18912](https://hibernate.atlassian.net/browse/HHH-18912) - Fix ORM release process
* [HHH-18881](https://hibernate.atlassian.net/browse/HHH-18881) - In MySQL, array of dates are not converted correctly
* [HHH-18872](https://hibernate.atlassian.net/browse/HHH-18872) - ConcreteProxy type not restored from 2LC when loading a ManyToOne
* [HHH-18862](https://hibernate.atlassian.net/browse/HHH-18862) - Group by error due to subselect using foreign key reference instead of primary key in HQL query
* [HHH-18859](https://hibernate.atlassian.net/browse/HHH-18859) - slice operator and @ElementCollection
* [HHH-18851](https://hibernate.atlassian.net/browse/HHH-18851) - ArrayContainsArgumentTypeResolver wrongly infers array type for needle argument
* [HHH-18850](https://hibernate.atlassian.net/browse/HHH-18850) - createCountQuery with Hibernate 6.6.2
* [HHH-18848](https://hibernate.atlassian.net/browse/HHH-18848) - JAR for org.hibernate.orm:hibernate-scan-jandex:7.0.0.Beta2 at Maven Central
* [HHH-18842](https://hibernate.atlassian.net/browse/HHH-18842) - Regression: CollectionType.replace() breaks if target is PersistentCollection, but not instance of Collection (e.g. PersistentMap)
* [HHH-18832](https://hibernate.atlassian.net/browse/HHH-18832) - Bytecode enhancement skipped for entities with "compute-only" @Transient properties
* [HHH-18830](https://hibernate.atlassian.net/browse/HHH-18830) - extraneous SQL UPDATE statements for unowned collection with @OrderColumn
* [HHH-18826](https://hibernate.atlassian.net/browse/HHH-18826) - mappedBy validation in Processor
* [HHH-18765](https://hibernate.atlassian.net/browse/HHH-18765) - Error in the booleanarray_to_string auxiliary function 
* [HHH-18709](https://hibernate.atlassian.net/browse/HHH-18709) - CriteriaUpdate involving JSON field containing Map<String, Object> results in SemanticException
* [HHH-18705](https://hibernate.atlassian.net/browse/HHH-18705) - Hibernate processor creates bad TypedReferenceQuery when @Entity have name attribute
* [HHH-18692](https://hibernate.atlassian.net/browse/HHH-18692) - Hibernate attempts to close batched statements multiple times 
* [HHH-18629](https://hibernate.atlassian.net/browse/HHH-18629) - Inconsistent column alias generated while result class is used for placeholder
* [HHH-18610](https://hibernate.atlassian.net/browse/HHH-18610) - "SQLGrammarException: Unable to find column position by name:" when using Single Table Inheritance with a strict JDBC driver such as PostgreSQL
* [HHH-18583](https://hibernate.atlassian.net/browse/HHH-18583) - Joined + discriminator inheritance treat in where clause not restricting to subtype
* [HHH-18274](https://hibernate.atlassian.net/browse/HHH-18274) - Problems with generics in queries; proposed partial solution
* [HHH-18069](https://hibernate.atlassian.net/browse/HHH-18069) - NullPointerException when unioning partition results
* [HHH-17838](https://hibernate.atlassian.net/browse/HHH-17838) - @OneToOne relationship + @Embeddable keys + FetchType.LAZY fail in most recent version
* [HHH-16054](https://hibernate.atlassian.net/browse/HHH-16054) - JPA / Hibernate, duplicate pkey error when updating entity that is a subclass of a base class that uses IdClass for composite primary key
* [HHH-14119](https://hibernate.atlassian.net/browse/HHH-14119) - IN clause parameter padding not working for criteria query in conjunction with LiteralHandlingMode.BIND

### Improvement
* [HHH-18875](https://hibernate.atlassian.net/browse/HHH-18875) - Stop using `Array.newInstance` in `org.hibernate.internal.util.collections.StandardStack`
* [HHH-18861](https://hibernate.atlassian.net/browse/HHH-18861) - Improve GitHub release announcement body for automated releases
* [HHH-18847](https://hibernate.atlassian.net/browse/HHH-18847) - Organize the org.hibernate.query.results package
* [HHH-18844](https://hibernate.atlassian.net/browse/HHH-18844) - Run preVerifyRelease task as part of h2 CI job
* [HHH-18841](https://hibernate.atlassian.net/browse/HHH-18841) - Make `_identifierMapper` property added for a IdClass synthetic
* [HHH-18840](https://hibernate.atlassian.net/browse/HHH-18840) - detect and report incorrect usage of @OrderColumn, @MapKeyColumn, and @MapKey
* [HHH-18683](https://hibernate.atlassian.net/browse/HHH-18683) - The method Metamodel#entity(String) should throw IllegalArgumentException for non-entities
* [HHH-18534](https://hibernate.atlassian.net/browse/HHH-18534) - Remove the org.hibernate.boot.models.categorize package
* [HHH-17246](https://hibernate.atlassian.net/browse/HHH-17246) - Guard against Sybase being configured for truncating trailing zeros.
* [HHH-16160](https://hibernate.atlassian.net/browse/HHH-16160) - XML aggregate support for more databases
* [HHH-14020](https://hibernate.atlassian.net/browse/HHH-14020) - Allow Hibernate Types to have access to ServiceRegistry during initialization
* [HHH-7913](https://hibernate.atlassian.net/browse/HHH-7913) - Catalog and schema replacement in <subselect> / @Subselect

### New Feature
* [HHH-18644](https://hibernate.atlassian.net/browse/HHH-18644) - New and improved hibernate-maven-plugin

### Remove Feature
* [HHH-18843](https://hibernate.atlassian.net/browse/HHH-18843) - remove deprecated @OrderBy annotation

### Sub-task
* [HHH-18804](https://hibernate.atlassian.net/browse/HHH-18804) - Add XML aggregate support for HANA
* [HHH-18803](https://hibernate.atlassian.net/browse/HHH-18803) - Add XML aggregate support for DB2
* [HHH-18802](https://hibernate.atlassian.net/browse/HHH-18802) - Add XML aggregate support for SQL Server
* [HHH-18801](https://hibernate.atlassian.net/browse/HHH-18801) - Add XML aggregate support for Sybase ASE
* [HHH-18800](https://hibernate.atlassian.net/browse/HHH-18800) - Add XML aggregate support for PostgreSQL
* [HHH-18799](https://hibernate.atlassian.net/browse/HHH-18799) - Add XML aggregate support for Oracle

### Task
* [HHH-18906](https://hibernate.atlassian.net/browse/HHH-18906) - Allow specifying UnsupportedEnhancementStrategy for Hibernate testing
* [HHH-18866](https://hibernate.atlassian.net/browse/HHH-18866) - Fix more failing tests on CockroachDB
* [HHH-18854](https://hibernate.atlassian.net/browse/HHH-18854) - Changes for Hibernate Reactive 3.0 integration
* [HHH-18678](https://hibernate.atlassian.net/browse/HHH-18678) - Use specific tasks for CI builds


## 7.0.0.Beta2 (November 13, 2024)

[Full changelog](https://hibernate.atlassian.net/projects/HHH/versions/32358)

### Bug
* [HHH-18816](https://hibernate.atlassian.net/browse/HHH-18816) - Error when rendering the fk-side of an association in an exists subquery
* [HHH-18808](https://hibernate.atlassian.net/browse/HHH-18808) - HqlParser.g4 outputs wrong token for `nakedIdentifier` and `identifier` when keyword is used
* [HHH-18807](https://hibernate.atlassian.net/browse/HHH-18807) - a bug in HqlLexer.g4
* [HHH-18806](https://hibernate.atlassian.net/browse/HHH-18806) - handling of nationalized strings on Sybase / jTDS
* [HHH-18773](https://hibernate.atlassian.net/browse/HHH-18773) - Multiple selections of same alias triggers possible non-threadsafe access to the session
* [HHH-18770](https://hibernate.atlassian.net/browse/HHH-18770) - NPE when using the JFR integration with JFR disabled
* [HHH-18764](https://hibernate.atlassian.net/browse/HHH-18764) - Class cast exception when using non basic type as identifier and in an embedded field using a natural ID
* [HHH-18761](https://hibernate.atlassian.net/browse/HHH-18761) - named query method generation for @NamedQuery on entity
* [HHH-18739](https://hibernate.atlassian.net/browse/HHH-18739) - Do not support join queries when using Mysql
* [HHH-18738](https://hibernate.atlassian.net/browse/HHH-18738) - Schema of database sequence is not configured if xml mapping is used
* [HHH-18730](https://hibernate.atlassian.net/browse/HHH-18730) - Multi-column association in aggregate component doesn't work
* [HHH-18720](https://hibernate.atlassian.net/browse/HHH-18720) - Type check on select columns in union all gives SemanticException when there is a null column
* [HHH-18719](https://hibernate.atlassian.net/browse/HHH-18719) - Previous row state reuse can provide detached entities to the consumer
* [HHH-18712](https://hibernate.atlassian.net/browse/HHH-18712) - Warning about attempts to update an immutable entity for normal (not immutable) entity
* [HHH-18703](https://hibernate.atlassian.net/browse/HHH-18703) - JoinedSubclassEntityPersister#getTableNameForColumn KO
* [HHH-18702](https://hibernate.atlassian.net/browse/HHH-18702) - Exception using @EmbeddedId with @OneToMany that refers to an alternate key column
* [HHH-18699](https://hibernate.atlassian.net/browse/HHH-18699) - Correctly handle @Id and @Version fields in query validation in Hibernate Processor
* [HHH-18697](https://hibernate.atlassian.net/browse/HHH-18697) - JPA 3.2 spec compliance for uppercasing of names in Hibernate Processor
* [HHH-18696](https://hibernate.atlassian.net/browse/HHH-18696) - @Find method for single @NaturalId field
* [HHH-18692](https://hibernate.atlassian.net/browse/HHH-18692) - Hibernate attempts to close batched statements multiple times 
* [HHH-18689](https://hibernate.atlassian.net/browse/HHH-18689) - 'FULL' query cache sometimes incomplete
* [HHH-18681](https://hibernate.atlassian.net/browse/HHH-18681) - InterpretationException executing subquery in case-when : o.h.query.sqm.tree.select.SqmSelection.getExpressible() is null
* [HHH-18675](https://hibernate.atlassian.net/browse/HHH-18675) - Self-referencing many-to-many relation on generic entity gives NullPointerException in mapping
* [HHH-18671](https://hibernate.atlassian.net/browse/HHH-18671) - Fix setting name (spelling)
* [HHH-18669](https://hibernate.atlassian.net/browse/HHH-18669) - NullPointerException in the AgroalConnectionProvider
* [HHH-18667](https://hibernate.atlassian.net/browse/HHH-18667) - Annotation processor leaks - OOME when used in Eclipse IDE
* [HHH-18662](https://hibernate.atlassian.net/browse/HHH-18662) - Attribute not mentioned in orm.xml ends up not being mapped in Hibernate ORM 7
* [HHH-18658](https://hibernate.atlassian.net/browse/HHH-18658) - Inner join prevents finding an entity instance referencing an empty map
* [HHH-18647](https://hibernate.atlassian.net/browse/HHH-18647) - SemanticException when using createCriteriaInsertValues to insert into foreign key column
* [HHH-18645](https://hibernate.atlassian.net/browse/HHH-18645) - AssertionError in AbstractBatchEntitySelectFetchInitializer#registerToBatchFetchQueue
* [HHH-18642](https://hibernate.atlassian.net/browse/HHH-18642) - DB2: select from new table with identity column not working when missing read permission
* [HHH-18635](https://hibernate.atlassian.net/browse/HHH-18635) - Avoid using `bigdatetime` column type on Sybase jconn when not necessary
* [HHH-18632](https://hibernate.atlassian.net/browse/HHH-18632) - Concurrency issue with AbstractEntityPersister#nonLazyPropertyLoadPlansByName
* [HHH-18631](https://hibernate.atlassian.net/browse/HHH-18631) - AssertionError when loading an entity after removing another, associated entity
* [HHH-18628](https://hibernate.atlassian.net/browse/HHH-18628) - Regression: Unable to determine TableReference
* [HHH-18626](https://hibernate.atlassian.net/browse/HHH-18626) - @Id annotation in @Embeddable class results in AssertionFailure
* [HHH-18617](https://hibernate.atlassian.net/browse/HHH-18617) - Fetching unowned side of bidirectional OneToOne mappings including tenant identifier triggers EntityFilteredException
* [HHH-18608](https://hibernate.atlassian.net/browse/HHH-18608) - NPE in EntityInitializerImpl.resolveInstanceSubInitializers
* [HHH-18596](https://hibernate.atlassian.net/browse/HHH-18596) - ValueHandlingMode hack in query pagination
* [HHH-18585](https://hibernate.atlassian.net/browse/HHH-18585) - exposure of internal types via Dialect
* [HHH-18582](https://hibernate.atlassian.net/browse/HHH-18582) - Mapping array of arrays with @JdbcTypeCode(SqlTypes.ARRAY) causes NPE
* [HHH-18581](https://hibernate.atlassian.net/browse/HHH-18581) - Performance degradation from Hibernate 5 to 6 on NativeQuery
* [HHH-18575](https://hibernate.atlassian.net/browse/HHH-18575) - IN predicate with numeric/decimal parameter types leads to Binding is multi-valued; illegal call to #getBindValue
* [HHH-18571](https://hibernate.atlassian.net/browse/HHH-18571) - Entities and collections with batch size 1 are treated as batchable
* [HHH-18570](https://hibernate.atlassian.net/browse/HHH-18570) - Invalid SQL when filter contains identifier named date
* [HHH-18565](https://hibernate.atlassian.net/browse/HHH-18565) - Bytecode enhancement, assertion error on reloading *toOne entities
* [HHH-18564](https://hibernate.atlassian.net/browse/HHH-18564) - Literal expressions using AttributeConverters stopped working in hibernate 6
* [HHH-18561](https://hibernate.atlassian.net/browse/HHH-18561) - Informix primary key constraint syntax error
* [HHH-18560](https://hibernate.atlassian.net/browse/HHH-18560) - DB2iDialect executes incompatible query in combination with @AuditJoinTable mapping
* [HHH-18558](https://hibernate.atlassian.net/browse/HHH-18558) - Informix UUID type support


## 7.0.0.Beta1 (August 01, 2024)

[Full changelog](https://hibernate.atlassian.net/projects/HHH/versions/32319)

### Bug
* [HHH-18314](https://hibernate.atlassian.net/browse/HHH-18314) - dialect for Db2 claims to be NationalizationSupport.EXPLICIT but never generates DDL with NCHAR/NVARCHAR

### Improvement
* [HHH-18453](https://hibernate.atlassian.net/browse/HHH-18453) - Fix Java code block highlighting in User Guide
* [HHH-18448](https://hibernate.atlassian.net/browse/HHH-18448) - Add cast and notEqualTo methods to JpaExpression and SqmExpression
* [HHH-18441](https://hibernate.atlassian.net/browse/HHH-18441) - Create extension to PersistenceConfiguration
* [HHH-18440](https://hibernate.atlassian.net/browse/HHH-18440) - Rewrite the Bootstrapping chapter in the User Guide
* [HHH-18412](https://hibernate.atlassian.net/browse/HHH-18412) - Upgrade JBoss Logging Tools (processor) to 3.0.1.Final
* [HHH-18393](https://hibernate.atlassian.net/browse/HHH-18393) - Upgrade JBoss Logging Tools (processor) to 3.0.0.Final
* [HHH-18316](https://hibernate.atlassian.net/browse/HHH-18316) - use utf8mb4 instead of utf8 a.k.a utf8mb3 on MySQL
* [HHH-18097](https://hibernate.atlassian.net/browse/HHH-18097) - Replace `java.io.Closeable` with `java.lang.AutoCloseable`
* [HHH-18009](https://hibernate.atlassian.net/browse/HHH-18009) - Consolidate JdbcObserver and ConnectionObserver into JdbcEventHandler
* [HHH-17720](https://hibernate.atlassian.net/browse/HHH-17720) - Add common JAXB contracts for named queries

### New Feature
* [HHH-18304](https://hibernate.atlassian.net/browse/HHH-18304) - Transform hbm.xml key-many-to-one references
* [HHH-18281](https://hibernate.atlassian.net/browse/HHH-18281) - Transform <filter-def/> and <filter/>
* [HHH-18266](https://hibernate.atlassian.net/browse/HHH-18266) - HbmXmlTransformer hbm inverse
* [HHH-18265](https://hibernate.atlassian.net/browse/HHH-18265) - HbmXmlTransformer transform hbm <key column=""/> 
* [HHH-18264](https://hibernate.atlassian.net/browse/HHH-18264) - HbmXmlTransformer collection classification
* [HHH-18060](https://hibernate.atlassian.net/browse/HHH-18060) - HbmXmlTransformer work
* [HHH-17979](https://hibernate.atlassian.net/browse/HHH-17979) - Add @PropertyRef

### Remove Feature
* [HHH-18452](https://hibernate.atlassian.net/browse/HHH-18452) - Remove deprecated org.hibernate.Interceptor methods
* [HHH-18449](https://hibernate.atlassian.net/browse/HHH-18449) - Remove deprecated Integrator#integrate form
* [HHH-18444](https://hibernate.atlassian.net/browse/HHH-18444) - Remove deprecate Session#refresh methods
* [HHH-18443](https://hibernate.atlassian.net/browse/HHH-18443) - Drop SessionFactoryBuilder#enableJpaListCompliance
* [HHH-18442](https://hibernate.atlassian.net/browse/HHH-18442) - Drop DynamicInsert#value and DynamicUpdate#value
* [HHH-18437](https://hibernate.atlassian.net/browse/HHH-18437) - Remove deprecations from JdbcSessionContext
* [HHH-18428](https://hibernate.atlassian.net/browse/HHH-18428) - Remove Session#delete
* [HHH-18199](https://hibernate.atlassian.net/browse/HHH-18199) - Remove @Where and @WhereJoinTable
* [HHH-18196](https://hibernate.atlassian.net/browse/HHH-18196) - Remove Session#save / Session#update / Session#saveOrUpdate
* [HHH-18195](https://hibernate.atlassian.net/browse/HHH-18195) - Remove @SelectBeforeUpdate
* [HHH-18194](https://hibernate.atlassian.net/browse/HHH-18194) - Remove @Proxy
* [HHH-18193](https://hibernate.atlassian.net/browse/HHH-18193) - Remove @Polymorphism
* [HHH-18191](https://hibernate.atlassian.net/browse/HHH-18191) - Remove @LazyToOne
* [HHH-18190](https://hibernate.atlassian.net/browse/HHH-18190) - Remove @LazyCollection
* [HHH-18189](https://hibernate.atlassian.net/browse/HHH-18189) - Remove @IndexColumn
* [HHH-18188](https://hibernate.atlassian.net/browse/HHH-18188) - Remove GenerationTime and its uses
* [HHH-18186](https://hibernate.atlassian.net/browse/HHH-18186) - Remove @GeneratorType
* [HHH-18184](https://hibernate.atlassian.net/browse/HHH-18184) - Remove CacheModeType and its uses
* [HHH-17697](https://hibernate.atlassian.net/browse/HHH-17697) - Remove deprecated annotations

### Sub-task
* [HHH-18197](https://hibernate.atlassian.net/browse/HHH-18197) - Remove @Table
* [HHH-18192](https://hibernate.atlassian.net/browse/HHH-18192) - Remove @Loader
* [HHH-18187](https://hibernate.atlassian.net/browse/HHH-18187) - Remove @Index
* [HHH-18185](https://hibernate.atlassian.net/browse/HHH-18185) - Remove @ForeignKey
* [HHH-18075](https://hibernate.atlassian.net/browse/HHH-18075) - Transform property-ref
* [HHH-17888](https://hibernate.atlassian.net/browse/HHH-17888) - Remove support for MariaDB versions older than 10.5

### Task
* [HHH-18397](https://hibernate.atlassian.net/browse/HHH-18397) - Transform "foreign" generators
* [HHH-18396](https://hibernate.atlassian.net/browse/HHH-18396) - Transform property-ref pointing to a to-one attribute
* [HHH-18394](https://hibernate.atlassian.net/browse/HHH-18394) - Fix transformation of nested subclass mappings
* [HHH-18037](https://hibernate.atlassian.net/browse/HHH-18037) - Move DerbyDialect to hibernate-community-dialects
* [HHH-18010](https://hibernate.atlassian.net/browse/HHH-18010) - Investigate ConnectionObserver and friends
* [HHH-17583](https://hibernate.atlassian.net/browse/HHH-17583) - Cleanup for 7.0
* [HHH-17448](https://hibernate.atlassian.net/browse/HHH-17448) - Add newly standard column annotation attributes to Hibernate column annotations


## 7.0.0.Alpha3 (June 14, 2024)

[Full changelog](https://hibernate.atlassian.net/projects/HHH/versions/32304)

### Bug
* [HHH-18135](https://hibernate.atlassian.net/browse/HHH-18135) - GenerationTypeStrategy implementations always throw UnsupportedOperationException
* [HHH-18081](https://hibernate.atlassian.net/browse/HHH-18081) - XML  <secondary-table/> element is not added to JdkClassDetails
* [HHH-11937](https://hibernate.atlassian.net/browse/HHH-11937) - Remove warnings about "empty composites" being experimental when feature is stabilized
* [HHH-11936](https://hibernate.atlassian.net/browse/HHH-11936) - Stabilize "empty composites" feature

### New Feature
* [HHH-18231](https://hibernate.atlassian.net/browse/HHH-18231) - SPI for persistence XML parsing
* [HHH-18057](https://hibernate.atlassian.net/browse/HHH-18057) - Support for JPA 3.2 column options
* [HHH-18056](https://hibernate.atlassian.net/browse/HHH-18056) - Support for JPA 32 table options
* [HHH-18055](https://hibernate.atlassian.net/browse/HHH-18055) - Support for JPA 3.2 table comment
* [HHH-18054](https://hibernate.atlassian.net/browse/HHH-18054) - Support for JPA 3.2 @CheckConstraint
* [HHH-16153](https://hibernate.atlassian.net/browse/HHH-16153) - Support JPA 3.2 `@EnumeratedValue`

### Remove Feature
* [HHH-18222](https://hibernate.atlassian.net/browse/HHH-18222) - remove hibernate.create_empty_composites.enabled in Hibernate 7
* [HHH-18207](https://hibernate.atlassian.net/browse/HHH-18207) - remove deprecated Dialects
* [HHH-18139](https://hibernate.atlassian.net/browse/HHH-18139) - remove IdentifierGeneratorFactory and related code

### Sub-task
* [HHH-18095](https://hibernate.atlassian.net/browse/HHH-18095) - Transform hbm.xml column read/write fragments
* [HHH-18072](https://hibernate.atlassian.net/browse/HHH-18072) - Transform hbm.xml not-found

### Task
* [HHH-18127](https://hibernate.atlassian.net/browse/HHH-18127) - Leverage hibernate-models Annotation-as-Class
* [HHH-18096](https://hibernate.atlassian.net/browse/HHH-18096) - Support for JPA 3.2 database generator options


## 7.0.0.Alpha2 (May 03, 2024)

[Full changelog](https://hibernate.atlassian.net/projects/HHH/versions/32280)

### Bug
* [HHH-18053](https://hibernate.atlassian.net/browse/HHH-18053) - duration arithmetic with fractional seconds
* [HHH-18049](https://hibernate.atlassian.net/browse/HHH-18049) - Handle <exclude-default-listeners/> and <exclude-superclass-listeners/>
* [HHH-18042](https://hibernate.atlassian.net/browse/HHH-18042) - ConstructorResults defined in XML are not applied
* [HHH-18041](https://hibernate.atlassian.net/browse/HHH-18041) - With SharedCacheMode.DISABLE_SELECTIVE entities with cacheable false should not be cached
* [HHH-18039](https://hibernate.atlassian.net/browse/HHH-18039) - EntityListeners defined in XML should replace those from annotations, not add to
* [HHH-18038](https://hibernate.atlassian.net/browse/HHH-18038) - Fall back to persistence-unit name as SessionFactory name
* [HHH-18036](https://hibernate.atlassian.net/browse/HHH-18036) - Retrieving java.sql.Date from Oracle contains unwanted milliseconds
* [HHH-18028](https://hibernate.atlassian.net/browse/HHH-18028) - TCK test failure with attribute converter and Embeddable
* [HHH-18018](https://hibernate.atlassian.net/browse/HHH-18018) - Derby implementation for 'right' function wrongly passes parameter to 'length'

### Improvement
* [HHH-18048](https://hibernate.atlassian.net/browse/HHH-18048) - Split notions of SessionFactory name and SessionFactory JNDI name
* [HHH-18005](https://hibernate.atlassian.net/browse/HHH-18005) - Remove AnnotationDescriptor#createUsage method calls that rely on lambdas for configuration
* [HHH-18003](https://hibernate.atlassian.net/browse/HHH-18003) - Create a PersistenceUnitDescriptor wrapper around JPA 3.2 PersistenceConfiguration
* [HHH-18000](https://hibernate.atlassian.net/browse/HHH-18000) - Remove XmlProcessingHelper methods for creating AnnotationUsage instances

### New Feature
* [HHH-18025](https://hibernate.atlassian.net/browse/HHH-18025) - RefreshOptions & LockOptions for Hibernate 7
* [HHH-18001](https://hibernate.atlassian.net/browse/HHH-18001) - FindOptions for Hibernate 7

### Task
* [HHH-18043](https://hibernate.atlassian.net/browse/HHH-18043) - Change SQL Server default timestamp precision to 7
* [HHH-18035](https://hibernate.atlassian.net/browse/HHH-18035) - Change Oracle default timestamp precision to 9
* [HHH-17982](https://hibernate.atlassian.net/browse/HHH-17982) - Setup JPA 3.2 TCK testing automation for ORM 7


## 7.0.0.Alpha1 (April 16, 2024)

[Full changelog](https://hibernate.atlassian.net/projects/HHH/versions/32214)

### Deprecation
* [HHH-17441](https://hibernate.atlassian.net/browse/HHH-17441) - Deprecate @Comment

### New Feature
* [HHH-17460](https://hibernate.atlassian.net/browse/HHH-17460) - Ongoing JPA 3.2 work
* [HHH-17459](https://hibernate.atlassian.net/browse/HHH-17459) - Allow resolution callbacks on select o.h.mapping objects

### Remove Feature
* [HHH-17961](https://hibernate.atlassian.net/browse/HHH-17961) - Drop support for hibernate.mapping.precedence
* [HHH-17894](https://hibernate.atlassian.net/browse/HHH-17894) - Remove AdditionalJaxbMappingProducer
* [HHH-17893](https://hibernate.atlassian.net/browse/HHH-17893) - Remove MetadataContributor
* [HHH-17892](https://hibernate.atlassian.net/browse/HHH-17892) - Remove @Persister

### Task
* [HHH-17444](https://hibernate.atlassian.net/browse/HHH-17444) - Ongoing JPA 32 work
