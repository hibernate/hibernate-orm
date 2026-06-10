# HBM XML Test Audit

Generated from current branch by correlating Java/config references to `*.hbm.xml` files and scanning mapping features. Dynamic models, `<import/>`, `<any/>`, `<many-to-any/>`, and `<idbag/>` are treated as Hibernate `mapping.xml` / annotation candidates rather than HBM-only drop candidates.

Updated after dropping `hibernate-envers`, removing the former `3.a` HBM-specific test bucket from `hibernate-core`,
converting the `hibernate-community-dialects` Derby custom SQL mapping to annotations, and adding
branch-local Gradle path exclusions for remaining HBM-dependent `hibernate-core` tests.

## 0. Branch-local test exclusions

The 9.0 branch now excludes remaining HBM-dependent `hibernate-core` test classes by compiled class-file path
in `hibernate-core/hibernate-core.gradle`. The exclusion list is generated from test sources that reference
`.hbm.xml`, inline `<hibernate-mapping>`, or live under an HBM test package, and includes subclasses of those
base classes. This keeps the 9.0 test suite usable while preserving test source paths so annotation / ORM XML
conversions from the 8.0 branch can merge forward with fewer conflicts.

## 1. Convert to annotations (184 test/config references)

- `hibernate-core/src/test/java/org/hibernate/orm/test/lob` (9)
  Tests/configs: BlobLocatorTest.java, ClobLocatorTest.java, ImageTest.java, LobAsLastValueTest.java, LobMergeTest.java, MaterializedBlobTest.java, MaterializedClobTest.java, SerializableTypeTest.java, TextTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/immutable/entitywithmutablecollection/inverse` (8) - `natural_version_generated`
  Tests/configs: EntityWithInverseManyToManyTest.java, EntityWithInverseOneToManyJoinTest.java, EntityWithInverseOneToManyTest.java, VersionedEntityWithInverseManyToManyTest.java, VersionedEntityWithInverseOneToManyFailureExpectedTest.java, VersionedEntityWithInverseOneToManyJoinFailureExpectedTest.java, VersionedEntityWithInverseOneToManyJoinTest.java, VersionedEntityWithInverseOneToManyTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/immutable/entitywithmutablecollection/noninverse` (8) - `natural_version_generated`
  Tests/configs: EntityWithNonInverseManyToManyTest.java, EntityWithNonInverseManyToManyUnidirTest.java, EntityWithNonInverseOneToManyJoinTest.java, EntityWithNonInverseOneToManyTest.java, EntityWithNonInverseOneToManyUnidirTest.java, VersionedEntityWithNonInverseManyToManyTest.java, VersionedEntityWithNonInverseOneToManyJoinTest.java, VersionedEntityWithNonInverseOneToManyTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/cascade/circle` (7) - `natural_version_generated`
  Tests/configs: CascadeMergeToChildBeforeParentTest.java, MultiPathCircleCascadeCheckNullFalseDelayedInsertTest.java, MultiPathCircleCascadeCheckNullTrueDelayedInsertTest.java, MultiPathCircleCascadeCheckNullibilityFalseTest.java, MultiPathCircleCascadeCheckNullibilityTrueTest.java, MultiPathCircleCascadeDelayedInsertTest.java, MultiPathCircleCascadeTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/bytecode/enhancement/cascade/circle` (6)
  Tests/configs: MultiPathCircleCascadeCheckNullFalseDelayedInsertTest.java, MultiPathCircleCascadeCheckNullTrueDelayedInsertTest.java, MultiPathCircleCascadeCheckNullibilityFalseTest.java, MultiPathCircleCascadeCheckNullibilityTrueTest.java, MultiPathCircleCascadeDelayedInsertTest.java, MultiPathCircleCascadeTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/ops` (6) - `natural_version_generated`
  Tests/configs: GetLoadTest.java, MergeMultipleEntityCopiesAllowedOrphanDeleteTest.java, MergeMultipleEntityCopiesAllowedTest.java, MergeMultipleEntityCopiesCustomTest.java, MergeMultipleEntityCopiesDisallowedByDefaultTest.java, SimpleOpsTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/readonly` (5) - `natural_version_generated`
  Tests/configs: ReadOnlyProxyTest.java, ReadOnlySessionLazyNonLazyTest.java, ReadOnlySessionTest.java, ReadOnlyTest.java, ReadOnlyVersionedNodesTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/jpa/ops` (4)
  Tests/configs: GetLoadJpaComplianceTest.java, GetLoadTest.java, MergeTest.java, PersistTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/cascade` (3)
  Tests/configs: BidirectionalOneToManyCascadeTest.java, CascadeTestWithAssignedParentIdTest.java, MultiPathCascadeTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/dialect/function` (3)
  Tests/configs: HANAFunctionsTest.java, MySQLRoundFunctionTest.java, SybaseASEFunctionTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/id` (3)
  Tests/configs: SQLServer2012SequenceGeneratorTest.java, SequenceGeneratorTest.java, UseIdentifierRollbackTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/idgen/enhanced/forcedtable` (3)
  Tests/configs: BasicForcedTableSequenceTest.java, HiLoForcedTableSequenceTest.java, PooledForcedTableSequenceTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/idgen/enhanced/table` (3)
  Tests/configs: BasicTableTest.java, HiLoTableTest.java, PooledTableTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/keymanytoone/bidir/component` (3)
  Tests/configs: EagerCollectionLazyKeyManyToOneTest.java, EagerKeyManyToOneTest.java, LazyKeyManyToOneTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/mapping/converted/enums` (3)
  Tests/configs: EnumExplicitTypeTest.java, EnumTypeTest.java, UnspecifiedEnumTypeTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/schemaupdate` (3)
  Tests/configs: CommentGenerationTest.java, MigrationTest.java, SchemaExportTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/tool/schema/scripts` (3)
  Tests/configs: CommandExtractorServiceTest.java, MultiLineImportFileTest.java, SingleLineImportFileTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/event/collection/association/bidirectional/manytomany` (2)
  Tests/configs: BidirectionalManyToManyBagToSetCollectionEventTest.java, BidirectionalManyToManySetToSetCollectionEventTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/event/collection/association/bidirectional/onetomany` (2)
  Tests/configs: BidirectionalOneToManyBagCollectionEventTest.java, BidirectionalOneToManySetCollectionEventTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/event/collection/association/unidirectional/onetomany` (2)
  Tests/configs: UnidirectionalOneToManyBagCollectionEventTest.java, UnidirectionalOneToManySetCollectionEventTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/generatedkeys/identity` (2)
  Tests/configs: IdentityGeneratedKeysTest.java, IdentityNoGeneratedKeysTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/idgen/biginteger/sequence` (2)
  Tests/configs: BigIntegerSequenceGeneratorTest.java, BigIntegerSequenceGeneratorZeroScaleTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/idgen/enhanced/sequence` (2)
  Tests/configs: HiLoSequenceTest.java, PooledSequenceTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/mapping/naturalid/immutable` (2) - `natural_version_generated`
  Tests/configs: ImmutableManyToOneNaturalIdHbmTest.java, ImmutableNaturalIdTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/onetoone/cache` (2)
  Tests/configs: OneToOneCacheTest.java, OneToOneConstrainedCacheTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/orphan` (2)
  Tests/configs: OrphanTest.java, PropertyRefTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/pagination` (2)
  Tests/configs: DistinctSelectTest.java, PaginationTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/proxy` (2)
  Tests/configs: MultipleSessionFactoriesProxyTest.java, ProxyTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/reattachment` (2)
  Tests/configs: CollectionReattachmentTest.java, ProxyReattachmentTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/stateless` (2) - `natural_version_generated`
  Tests/configs: StatelessSessionQueryTest.java, StatelessSessionTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/bootstrap` (1)
  Tests/configs: BootstrapTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/bootstrap/scanning` (1)
  Tests/configs: PackagingTestCase.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/bytecode/enhancement/lazy` (1)
  Tests/configs: MultiPathCascadeTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/bytecode/enhancement/orphan` (1)
  Tests/configs: OrphanTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/cid` (1)
  Tests/configs: CompositeIdWithGeneratorTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/collection/propertyref` (1)
  Tests/configs: PropertyRefTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/collection/propertyref/lazy` (1)
  Tests/configs: PropertyRefTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/component/basic` (1) - `natural_version_generated`
  Tests/configs: ComponentTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/component/cascading/collection` (1)
  Tests/configs: CascadeToComponentCollectionTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/component/cascading/toone` (1)
  Tests/configs: CascadeToComponentAssociationTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/cut` (1)
  Tests/configs: CompositeUserTypeTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/entitymode/dom4j` (1)
  Tests/configs: DeprecationLoggingTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/event/collection` (1)
  Tests/configs: BrokenCollectionEventTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/event/collection/association/unidirectional/manytomany` (1)
  Tests/configs: UnidirectionalManyToManyBagCollectionEventTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/event/collection/detached` (1)
  Tests/configs: DetachedMultipleCollectionChangeTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/exception` (1)
  Tests/configs: SQLExceptionConversionTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/flush` (1)
  Tests/configs: NativeCriteriaSyncTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/hql` (1)
  Tests/configs: FunctionNameAsColumnTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/hqlfetchscroll` (1)
  Tests/configs: HQLScrollFetchTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/idgen/biginteger/increment` (1)
  Tests/configs: BigIntegerIncrementGeneratorTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/idprops` (1)
  Tests/configs: IdentifierPropertyReferencesTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/immutable` (1)
  Tests/configs: ImmutableTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/insertordering` (1)
  Tests/configs: InsertOrderingTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/interceptor` (1)
  Tests/configs: InterceptorTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/jdbc` (1)
  Tests/configs: GeneralWorkTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/join` (1)
  Tests/configs: OptionalJoinTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/jpa/cascade2` (1)
  Tests/configs: CascadeTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/jpa/criteria/fetchscroll` (1)
  Tests/configs: CriteriaScrollFetchTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/jpa/fetch` (1)
  Tests/configs: FetchingTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/jpa/naturalid` (1) - `natural_version_generated`
  Tests/configs: ImmutableNaturalIdTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/jpa/xml` (1)
  Tests/configs: JpaEntityNameTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/keymanytoone/bidir/embedded` (1)
  Tests/configs: KeyManyToOneTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/keymanytoone/bidir/ondelete` (1)
  Tests/configs: KeyManyToOneCascadeDeleteTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/keymanytoone/unidir/ondelete` (1)
  Tests/configs: KeyManyToOneCascadeDeleteTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/manytomany/batchload` (1)
  Tests/configs: BatchedManyToManyTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/manytomany/ordered` (1)
  Tests/configs: OrderedManyToManyTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/manytomanyassociationclass/compositeid` (1)
  Tests/configs: ManyToManyAssociationClassCompositeIdTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/manytomanyassociationclass/nestedreference` (1)
  Tests/configs: ItemSelfReferenceTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/manytomanyassociationclass/surrogateid/assigned` (1)
  Tests/configs: ManyToManyAssociationClassAssignedIdTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/manytomanyassociationclass/surrogateid/generated` (1)
  Tests/configs: ManyToManyAssociationClassGeneratedIdTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/mapelemformula` (1)
  Tests/configs: MapElementFormulaTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/mapping/formula` (1)
  Tests/configs: FormulaFromHbmTests.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/mapping/generated` (1) - `natural_version_generated`
  Tests/configs: TimestampGeneratedValuesWithCachingTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/mapping/naturalid/composite` (1) - `natural_version_generated`
  Tests/configs: HbmCompositeIdAndNaturalIdTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/mapping/naturalid/mutable` (1) - `natural_version_generated`
  Tests/configs: MutableNaturalIdTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/mapping/naturalid/nullable` (1) - `natural_version_generated`
  Tests/configs: NullableNaturalIdTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/mapping/readwrite` (1)
  Tests/configs: HbmReadWriteTests.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/mappingexception` (1)
  Tests/configs: MappingExceptionTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/namingstrategy` (1)
  Tests/configs: NamingStrategyTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/onetomany` (1)
  Tests/configs: OneToManyTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/onetoone/link` (1)
  Tests/configs: OneToOneLinkTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/ordered` (1)
  Tests/configs: OrderByTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/orphan/manytomany` (1)
  Tests/configs: ManyToManyOrphanTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/orphan/one2one/fk/bidirectional` (1)
  Tests/configs: DeleteOneToOneOrphansTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/orphan/one2one/fk/composite` (1)
  Tests/configs: DeleteOneToOneOrphansTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/orphan/one2one/fk/reversed/bidirectional` (1)
  Tests/configs: DeleteOneToOneOrphansTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/orphan/one2one/fk/reversed/unidirectional` (1)
  Tests/configs: DeleteOneToOneOrphansTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/orphan/one2one/pk/bidirectional` (1)
  Tests/configs: DeleteOneToOneOrphansTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/orphan/one2one/pk/unidirectional` (1)
  Tests/configs: DeleteOneToOneOrphansTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/property` (1)
  Tests/configs: GetAndIsVariantGetterTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/propertyref/basic` (1)
  Tests/configs: PropertyRefTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/propertyref/cachedcollections` (1) - `natural_version_generated`
  Tests/configs: CachedPropertyRefCollectionTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/propertyref/component/complete` (1)
  Tests/configs: CompleteComponentPropertyRefTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/propertyref/partial` (1)
  Tests/configs: PartialComponentPropertyRefTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/querycache` (1)
  Tests/configs: QueryCacheTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/schemaupdate/idgenerator` (1)
  Tests/configs: SequenceGeneratorIncrementTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/schemaupdate/manytomany` (1)
  Tests/configs: ForeignKeyNameTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/stateless/fetching` (1)
  Tests/configs: StatelessSessionFetchingTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/stats` (1)
  Tests/configs: SessionStatsTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/timestamp` (1) - `natural_version_generated`
  Tests/configs: TimestampTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/tm` (1)
  Tests/configs: TransactionTimeoutTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/typeoverride` (1)
  Tests/configs: TypeOverrideTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/unconstrained` (1)
  Tests/configs: UnconstrainedTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/version` (1) - `natural_version_generated`
  Tests/configs: VersionTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/version/db` (1) - `natural_version_generated`
  Tests/configs: DbVersionTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/version/mappedsuperclass` (1) - `natural_version_generated`
  Tests/configs: HbmMappingMappedSuperclassWithVersionTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/version/sybase` (1) - `natural_version_generated`
  Tests/configs: SybaseTimestampVersioningTest.java
- `hibernate-core/src/test/resources/hibernate.cfg.xml` (1)
  Tests/configs: hibernate.cfg.xml
## 2. Convert to mapping.xml / orm.xml (80 test/config references)

- `hibernate-core/src/test/java/org/hibernate/orm/test/jpa/metamodel` (3) - `dynamic_model`
  Tests/configs: JpaMetamodelDisabledPopulationTest.java, JpaMetamodelEnabledPopulationTest.java, JpaMetamodelIgnoreUnsupportedPopulationTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/mapping/generated` (3) - `custom_sql`, `natural_version_generated`
  Tests/configs: PartiallyGeneratedComponentTest.java, TriggerGeneratedValuesWithCachingTest.java, TriggerGeneratedValuesWithoutCachingTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/subclassfilter` (3) - `hbm_extends`, `filters_where`
  Tests/configs: DiscrimSubclassFilterTest.java, JoinedSubclassFilterTest.java, UnionSubclassFilterTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/connections` (2) - `filters_where`
  Tests/configs: ConnectionManagementTestCase.java, HibernateCreateBlobFailedCase.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/entityname` (2) - `dynamic_model`, `hbm_extends`
  Tests/configs: EntityNameFromSubClassTest.java, MultipleMappingsTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/hql` (2) - `composite_element`, `hbm_extends`, `index_map`, `natural_version_generated`, `meta`, `any`, `dynamic_model`
  Tests/configs: ASTParserLoadingTest.java, BulkManipulationTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/schemaupdate/uniqueconstraint` (2) - `dynamic_model`
  Tests/configs: UniqueConstraintDropTest.java, UniqueConstraintGenerationTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/subselect` (2) - `subselect`
  Tests/configs: SetSubselectTest.java, SubselectTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/boot/database/qualfiedTableNaming` (1) - `dynamic_model`
  Tests/configs: DefaultCatalogAndSchemaTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/compositeelement` (1) - `import`, `composite_element`
  Tests/configs: CompositeElementTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/cuk` (1) - `formulas_properties`
  Tests/configs: CompositePropertyRefTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/entitymode/map/basic` (1) - `dynamic_model`
  Tests/configs: DynamicClassTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/entitymode/map/compositeId` (1) - `dynamic_model`
  Tests/configs: CompositeIdTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/entitymode/map/subclass` (1) - `dynamic_model`, `hbm_extends`
  Tests/configs: SubclassDynamicMapTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/filter` (1) - `filters_where`
  Tests/configs: DynamicFilterTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/formulajoin` (1) - `formulas_properties`
  Tests/configs: FormulaJoinTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/generatedkeys/generated` (1) - `custom_sql`
  Tests/configs: GeneratedTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/generatedkeys/select` (1) - `custom_sql`, `natural_version_generated`
  Tests/configs: SelectGeneratorTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/generatedkeys/selectannotated` (1) - `custom_sql`
  Tests/configs: SelectGeneratorTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/idbag` (1) - `idbag`
  Tests/configs: IdBagTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/idgen/enhanced/sequence` (1) - `dynamic_model`
  Tests/configs: BasicSequenceTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/iterate` (1) - `dynamic_model`
  Tests/configs: ScrollTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/jpa/criteria/paths` (1) - `dynamic_model`
  Tests/configs: DynamicModelSingularAttributeJoinTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/lazyonetoone` (1) - `filters_where`
  Tests/configs: LazyOneToOneTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/legacy` (1) - `hbm_extends`, `hbm_query_resultset`, `formulas_properties`
  Tests/configs: ABCTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/manytomany` (1) - `formulas_properties`
  Tests/configs: ManyToManyTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/map` (1) - `hbm_query_resultset`
  Tests/configs: MapIndexFormulaTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/mapping/attrorder` (1) - `dynamic_model`, `composite_element`, `natural_version_generated`
  Tests/configs: AttributeOrderingTests.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/mapping/collections/custom/basic` (1) - `import`, `index_map`
  Tests/configs: UserCollectionTypeHbmVariantTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/mapping/collections/custom/declaredtype` (1) - `import`, `index_map`
  Tests/configs: UserCollectionTypeHbmVariantTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/mapping/collections/custom/parameterized` (1) - `typedef`, `index_map`
  Tests/configs: ParameterizedUserCollectionTypeHbmVariantTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/mapping/collections/mapcompelem` (1) - `import`, `composite_element`
  Tests/configs: MapCompositeElementTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/mapping/contributed` (1) - `dynamic_model`, `natural_version_generated`
  Tests/configs: ContributorImpl.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/mapping/dynamic` (1) - `dynamic_model`
  Tests/configs: DynamicEntityTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/mapping/inheritance/dynamic` (1) - `dynamic_model`, `hbm_extends`
  Tests/configs: DynamicJoinedInheritanceTests.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/mapping/typedmanytoone` (1) - `dynamic_model`, `filters_where`, `formulas_properties`
  Tests/configs: TypedManyToOneTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/mapping/usertypes` (1) - `typedef`
  Tests/configs: UserTypeMappingTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/namingstrategy/synchronizedTables` (1) - `subselect`
  Tests/configs: SynchronizeTableNamingTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/onetoone/formula` (1) - `formulas_properties`
  Tests/configs: OneToOneFormulaTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/onetoone/joined` (1) - `dynamic_model`, `hbm_extends`
  Tests/configs: JoinedSubclassOneToOneTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/onetoone/nopojo` (1) - `dynamic_model`
  Tests/configs: DynamicMapOneToOneTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/onetoone/singletable` (1) - `dynamic_model`, `hbm_extends`
  Tests/configs: DiscrimSubclassOneToOneTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/optlock` (1) - `dynamic_model`
  Tests/configs: OptimisticLockTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/propertyref/basic` (1) - `formulas_properties`
  Tests/configs: BasicPropertiesTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/propertyref/inheritence/joined` (1) - `hbm_extends`, `formulas_properties`
  Tests/configs: JoinedSubclassPropertyRefTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/query/joinfetch` (1) - `hbm_query_resultset`
  Tests/configs: JoinFetchTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/query/resultmapping` (1) - `dynamic_model`, `hbm_query_resultset`
  Tests/configs: EntityResultTests.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/quote` (1) - `typedef`
  Tests/configs: QuoteGlobalTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/resulttransformer` (1) - `hbm_query_resultset`
  Tests/configs: ResultTransformerTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/rowid` (1) - `typedef`
  Tests/configs: RowIdTest2.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/schema` (1) - `custom_sql`
  Tests/configs: PostgreSQLSchemaGenerationTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/schemaupdate` (1) - `formulas_properties`
  Tests/configs: QuotedTableNameWithForeignKeysSchemaUpdateTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/schemaupdate/idbag` (1) - `idbag`
  Tests/configs: IdBagSequenceTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/serialization` (1) - `dynamic_model`
  Tests/configs: MapProxySerializationTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/sql/check` (1) - `custom_sql`
  Tests/configs: OracleCheckStyleTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/sql/hand/custom/db2` (1) - `custom_sql`, `hbm_query_resultset`, `subselect`
  Tests/configs: DB2CustomSQLTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/sql/hand/custom/mysql` (1) - `custom_sql`, `hbm_query_resultset`, `subselect`
  Tests/configs: MySQLCustomSQLTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/sql/hand/custom/oracle` (1) - `custom_sql`, `hbm_query_resultset`, `subselect`
  Tests/configs: OracleCustomSQLTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/sql/hand/custom/sqlserver` (1) - `custom_sql`, `hbm_query_resultset`, `subselect`
  Tests/configs: SQLServerCustomSQLTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/sql/hand/custom/sybase` (1) - `custom_sql`, `hbm_query_resultset`, `subselect`
  Tests/configs: SybaseCustomSQLTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/sql/hand/identity` (1) - `custom_sql`
  Tests/configs: CustomInsertSQLWithIdentityColumnTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/sql/hand/query` (1) - `hbm_query_resultset`, `index_map`
  Tests/configs: NativeSQLQueriesTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/sql/hand/quotedidentifiers` (1) - `hbm_query_resultset`
  Tests/configs: NativeSqlAndQuotedIdentifiersTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/stats` (1) - `import`
  Tests/configs: StatsTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/subselect/join` (1) - `subselect`
  Tests/configs: SubselectInJoinedTableTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/tm` (1) - `dynamic_model`
  Tests/configs: CMTTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/typedonetoone` (1) - `dynamic_model`, `filters_where`
  Tests/configs: TypedOneToOneTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/typeparameters` (1) - `typedef`
  Tests/configs: TypeParameterTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/unionsubclass/alias` (1) - `dynamic_model`, `hbm_extends`
  Tests/configs: SellCarTest.java
## 3.a Drop or quarantine as HBM-specific (0 remaining test/config references)

Completed. Removed 34 `hibernate-core` references from this bucket and the remaining 11 `hibernate-envers` references when the Envers module was dropped.

## 3.b Convert to annotations or drop if redundant (10 test/config references)

Most `hbm_extends` entries in this bucket were dropped. The one remaining `hbm_extends` entry is `AbstractJPATest`, which is a shared base class for many JPA tests rather than an isolated HBM-specific test.

- `hibernate-core/src/test/java/org/hibernate/orm/test/ops` (3) - `index_map`, `natural_version_generated`
  Tests/configs: AbstractOperationTestCase.java, HANANoColumnInsertTest.java, OracleNoColumnInsertTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/onetomany` (2) - `index_map`, `natural_version_generated`
  Tests/configs: AbstractRecursiveBidirectionalOneToManyTest.java, AbstractVersionedRecursiveBidirectionalOneToManyTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/unidir` (2) - `index_map`
  Tests/configs: BackrefPropertyRefTest.java, BackrefTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/jpa/model` (1) - `natural_version_generated`, `hbm_extends`
  Tests/configs: AbstractJPATest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/propertyref` (1) - `index_map`
  Tests/configs: DoesNotWorkWithHbmTest.java
- `hibernate-core/src/test/java/org/hibernate/orm/test/ternary` (1) - `index_map`
  Tests/configs: TernaryTest.java
