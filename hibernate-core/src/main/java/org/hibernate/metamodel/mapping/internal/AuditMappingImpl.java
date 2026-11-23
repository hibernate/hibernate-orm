package org.hibernate.metamodel.mapping.internal;

import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hibernate.audit.AuditStrategy;
import org.hibernate.audit.ModificationType;
import org.hibernate.audit.internal.AuditEntityLoaderImpl;
import org.hibernate.audit.spi.AuditEntityLoader;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.mapping.AuditMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.FunctionRenderer;
import org.hibernate.query.sqm.function.SelfRenderingAggregateFunctionSqlAstExpression;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.creation.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.creation.SqlAstCreationState;
import org.hibernate.sql.ast.spi.query.expression.AggregateFunctionExpression;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.expression.Expression;
import org.hibernate.sql.ast.spi.query.expression.JdbcLiteral;
import org.hibernate.sql.ast.spi.query.expression.SelfRenderingSqlFragmentExpression;
import org.hibernate.sql.ast.spi.query.from.LazyTableGroup;
import org.hibernate.sql.ast.spi.query.from.NamedTableReference;
import org.hibernate.sql.ast.spi.query.from.StandardTableGroup;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.from.TableGroupJoin;
import org.hibernate.sql.ast.spi.query.from.TableGroupProducer;
import org.hibernate.sql.ast.spi.query.from.TableReference;
import org.hibernate.sql.ast.spi.query.from.TableReferenceJoin;
import org.hibernate.sql.ast.spi.query.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.spi.query.predicate.Junction;
import org.hibernate.sql.ast.spi.query.predicate.NullnessPredicate;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.exec.internal.TemporalJdbcParameter;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.type.BasicType;

import jakarta.annotation.Nullable;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;
import static org.hibernate.metamodel.mapping.internal.CurrentState.ownerRevision;
import static java.util.Collections.singletonList;
import static org.hibernate.audit.AuditStrategy.VALIDITY;
import static org.hibernate.query.sqm.ComparisonOperator.EQUAL;
import static org.hibernate.query.sqm.ComparisonOperator.GREATER_THAN;
import static org.hibernate.query.sqm.ComparisonOperator.LESS_THAN_OR_EQUAL;
import static org.hibernate.query.sqm.ComparisonOperator.NOT_EQUAL;


/**
 * Audit mapping implementation.
 *
 * @author Gavin King
 * @since 7.4
 */
public class AuditMappingImpl implements AuditMapping {
	private static final String SUBQUERY_ALIAS_STEM = "audit";
	public static final String MAX = "max";

	/**
	 * Per-table audit info.
	 */
	public record TableAuditInfo(
			String auditTableName,
			SelectableMapping changesetIdMapping,
			@Nullable SelectableMapping modificationTypeMapping,
			@Nullable SelectableMapping invalidatingChangesetMapping,
			List<SelectableMapping> collectionIndexColumns
	) {
		public TableAuditInfo(String auditTableName, SelectableMapping changesetIdMapping,
				@Nullable SelectableMapping modificationTypeMapping, @Nullable SelectableMapping invalidatingChangesetMapping) {
			this( auditTableName, changesetIdMapping, modificationTypeMapping, invalidatingChangesetMapping, List.of() );
		}
	}

	private final Map<String, TableAuditInfo> tableAuditInfoMap;

	private final JdbcMapping jdbcMapping;
	private final BasicType<?> changesetIdBasicType;
	@Nullable private final String currentTimestampFunctionName;
	private final FunctionRenderer maxFunctionDescriptor;
	private final AuditStrategy auditStrategy;

	@Nullable private final EntityMappingType entityMappingType;
	@Nullable private final PluralAttributeMapping collectionMapping;
	private final SessionFactoryImplementor sessionFactory;
	@Nullable private AuditEntityLoader entityLoader;

	/**
	 * The audit mapping of an entity.
	 */
	public AuditMappingImpl(
			Map<String, TableAuditInfo> tableAuditInfoMap,
			EntityMappingType entityMappingType,
			MappingModelCreationProcess creationProcess) {
		this( tableAuditInfoMap, entityMappingType, null, creationProcess );
	}

	/**
	 * The audit mapping of a collection, which is also the audit mapping of the
	 * inverse side of a many-to-many association.
	 */
	public AuditMappingImpl(
			Map<String, TableAuditInfo> tableAuditInfoMap,
			PluralAttributeMapping collectionMapping,
			MappingModelCreationProcess creationProcess) {
		this( tableAuditInfoMap, null, collectionMapping, creationProcess );
	}

	private AuditMappingImpl(
			Map<String, TableAuditInfo> tableAuditInfoMap,
			@Nullable EntityMappingType entityMappingType,
			@Nullable PluralAttributeMapping collectionMapping,
			MappingModelCreationProcess creationProcess) {
		this.tableAuditInfoMap = Map.copyOf( tableAuditInfoMap );
		this.entityMappingType = entityMappingType;
		this.collectionMapping = collectionMapping;

		final var creationContext = creationProcess.getCreationContext();
		this.sessionFactory = creationContext.getSessionFactory();
		this.auditStrategy = sessionFactory.getSessionFactoryOptions().getAuditStrategy();

		// Historical predicates must use the same mapping as the audit columns,
		// rather than resolving an application-wide default again from the Java type.
		jdbcMapping = tableAuditInfoMap.values().iterator().next().changesetIdMapping().getJdbcMapping();
		changesetIdBasicType = (BasicType<?>) jdbcMapping;

		final var dialect = sessionFactory.getJdbcServices().getDialect();
		currentTimestampFunctionName =
				sessionFactory.getChangesetCoordinator().useServerTimestamp( dialect )
						? dialect.getCurrentTemporalSupport().currentTimestamp()
						: null;

		maxFunctionDescriptor = resolveMaxFunction( sessionFactory );
	}

	@Nullable
	@Override
	public AuditMapping forInverseCollection(@Nonnull Property inverseProperty) {
		return inverseProperty.isAuditedExcluded() ? null : this;
	}

	private TableAuditInfo resolveInfo(String originalTableName) {
		final var info = tableAuditInfoMap.get( originalTableName );
		if ( info == null ) {
			throw new IllegalArgumentException(
					"No audit table info for table '" + originalTableName
							+ "' (known tables: " + tableAuditInfoMap.keySet() + ")" );
		}
		return info;
	}

	@Override
	public List<SelectableMapping> getCollectionIndexColumns(String originalTableName) {
		return resolveInfo( originalTableName ).collectionIndexColumns();
	}

	@Nonnull
	@Override
	public AuditEntityLoader getEntityLoader() {
		if ( entityLoader == null ) {
			if ( entityMappingType == null ) {
				throw new IllegalStateException( "getEntityLoader() is not available for collection audit mappings" );
			}
			entityLoader = new AuditEntityLoaderImpl( entityMappingType, sessionFactory );
		}
		return entityLoader;
	}

	@Nonnull
	@Override
	public String getTableName() {
		throw new UnsupportedOperationException(
				"Invalid call to getTableName() for multi-table aware AuditMapping implementation"
		);
	}

	@Nonnull
	@Override
	public String resolveTableName(@Nonnull String originalTableName) {
		return resolveInfo( originalTableName ).auditTableName;
	}

	@Nonnull
	@Override
	public SelectableMapping getChangesetIdMapping(@Nonnull String originalTableName) {
		return resolveInfo( originalTableName ).changesetIdMapping;
	}

	@Nullable
	@Override
	public SelectableMapping getModificationTypeMapping(@Nonnull String originalTableName) {
		return resolveInfo( originalTableName ).modificationTypeMapping;
	}

	@Nullable
	@Override
	public SelectableMapping getInvalidatingChangesetIdMapping(@Nonnull String originalTableName) {
		return resolveInfo( originalTableName ).invalidatingChangesetMapping;
	}

	@Nonnull
	@Override
	public List<String> getExtraSelectExpressions() {
		final var anyInfo = tableAuditInfoMap.values().iterator().next();
		final var exprs = new ArrayList<>( List.of(
				anyInfo.changesetIdMapping.getSelectionExpression(),
				castNonNull( anyInfo.modificationTypeMapping ).getSelectionExpression()
		) );
		if ( anyInfo.invalidatingChangesetMapping != null ) {
			exprs.add( anyInfo.invalidatingChangesetMapping.getSelectionExpression() );
		}
		return exprs;
	}

	@Nonnull
	@Override
	public JdbcMapping getJdbcMapping() {
		return jdbcMapping;
	}

	private Expression resolveDefaultUpperBound(TableAuditInfo info) {
		return currentTimestampFunctionName != null
				? new SelfRenderingSqlFragmentExpression( currentTimestampFunctionName, jdbcMapping )
				: new TemporalJdbcParameter( info.changesetIdMapping );
	}

	@Nonnull
	@Override
	public Predicate createRestriction(
			@Nonnull TableGroupProducer tableGroupProducer,
			@Nonnull TableReference tableReference,
			@Nonnull List<SelectableMapping> keySelectables,
			@Nonnull SqlAliasBaseGenerator sqlAliasBaseGenerator,
			@Nonnull String originalTableName,
			@Nonnull Expression upperBound,
			boolean includeDeletions) {
		return createRestriction(
				tableGroupProducer,
				tableReference,
				keySelectables,
				sqlAliasBaseGenerator,
				resolveInfo( originalTableName ),
				upperBound,
				includeDeletions
		);
	}

	/**
	 * Build the temporal restriction predicate.
	 * <p>
	 * For the default strategy:
	 * {@code REV = (SELECT MAX(REV) ... WHERE REV <= upperBound) AND REVTYPE <> 2}
	 * <p>
	 * For the validity strategy:
	 * {@code REV <= upperBound AND (REVEND > upperBound OR REVEND IS NULL) AND REVTYPE <> 2}
	 *
	 * @param includeDeletions if {@code true}, omit the {@code REVTYPE <> DEL} filter
	 */
	private Predicate createRestriction(
			TableGroupProducer tableGroupProducer,
			TableReference tableReference,
			List<SelectableMapping> keySelectables,
			SqlAliasBaseGenerator sqlAliasBaseGenerator,
			TableAuditInfo info,
			Expression upperBound,
			boolean includeDeletions) {
		final var revision = usesValidityStrategy( info )
				? createValidityRestriction( tableReference, info, upperBound )
				: createMaxRevisionRestriction( tableGroupProducer, tableReference, keySelectables, sqlAliasBaseGenerator, info, upperBound );
		return includeDeletions || info.modificationTypeMapping == null ? revision
				: Predicate.combinePredicates( revision, excludeDeletions( tableReference, info.modificationTypeMapping ) );
	}

	private Predicate createMaxRevisionRestriction(
			TableGroupProducer tableGroupProducer, TableReference tableReference, List<SelectableMapping> keySelectables,
			SqlAliasBaseGenerator sqlAliasBaseGenerator, TableAuditInfo info, Expression upperBound) {
		final var subQuerySpec = new QuerySpec( false, 1 );
		final String stem = tableGroupProducer.getSqlAliasStem();
		final String aliasStem = stem == null ? SUBQUERY_ALIAS_STEM : stem;
		final var subTableReference = new NamedTableReference(
				info.auditTableName,
				sqlAliasBaseGenerator.createSqlAliasBase( aliasStem ).generateNewAlias()
		);
		final var subTableGroup = new StandardTableGroup(
				true,
				new NavigablePath( stem == null ? "audit-subquery" : stem + "#audit" ),
				tableGroupProducer,
				subTableReference.getIdentificationVariable(),
				subTableReference,
				null,
				null
		);
		subQuerySpec.getFromClause().addRoot( subTableGroup );

		final var changesetId = new ColumnReference( subTableReference, info.changesetIdMapping );
		subQuerySpec.getSelectClause()
				.addSqlSelection( new SqlSelectionImpl( buildMaxExpression( changesetId ) ) );

		// Subquery WHERE: id columns match + REV <= upperBound
		final var subPredicate = new Junction( Junction.Nature.CONJUNCTION );
		for ( var selectableMapping : keySelectables ) {
			subPredicate.add( new ComparisonPredicate(
					new ColumnReference( subTableReference, selectableMapping ),
					EQUAL,
					new ColumnReference( tableReference, selectableMapping )
			) );
		}
		subPredicate.add( new ComparisonPredicate( changesetId, LESS_THAN_OR_EQUAL, upperBound ) );
		subQuerySpec.applyPredicate( subPredicate );

		return new ComparisonPredicate(
				new ColumnReference( tableReference, info.changesetIdMapping ),
				EQUAL,
				new SelectStatement( subQuerySpec )
		);
	}

	private boolean usesValidityStrategy(TableAuditInfo info) {
		return auditStrategy == VALIDITY && info.invalidatingChangesetMapping != null;
	}

	/**
	 * Build the validity strategy restriction:
	 * {@code REV <= upperBound AND (REVEND > upperBound OR REVEND IS NULL)}
	 */
	private static Predicate createValidityRestriction(TableReference tableReference, TableAuditInfo info, Expression upperBound) {
		final var predicate = new Junction( Junction.Nature.CONJUNCTION );

		// REV <= upperBound
		predicate.add( new ComparisonPredicate(
				new ColumnReference( tableReference, info.changesetIdMapping ),
				LESS_THAN_OR_EQUAL,
				upperBound
		) );

		// (REVEND > upperBound OR REVEND IS NULL)
		final var revEndRef = new ColumnReference( tableReference, info.invalidatingChangesetMapping );
		final var revEndDisjunction = new Junction( Junction.Nature.DISJUNCTION );
		revEndDisjunction.add( new ComparisonPredicate( revEndRef, GREATER_THAN, upperBound ) );
		revEndDisjunction.add( new NullnessPredicate( revEndRef ) );
		predicate.add( revEndDisjunction );

		return predicate;
	}

	private static Predicate excludeDeletions(TableReference tableReference, SelectableMapping modificationType) {
		return new ComparisonPredicate(
				new ColumnReference( tableReference, modificationType ),
				NOT_EQUAL,
				new JdbcLiteral<>( ModificationType.DEL, modificationType.getJdbcMapping() )
		);
	}

	private AggregateFunctionExpression buildMaxExpression(ColumnReference expression) {
		return new SelfRenderingAggregateFunctionSqlAstExpression<>(
				MAX,
				maxFunctionDescriptor,
				singletonList( expression ),
				null,
				changesetIdBasicType,
				changesetIdBasicType
		);
	}

	private static FunctionRenderer resolveMaxFunction(SessionFactoryImplementor sessionFactory) {
		final var functionDescriptor =
				sessionFactory.getQueryEngine().getSqmFunctionRegistry()
						.findFunctionDescriptor( MAX );
		if ( functionDescriptor instanceof AbstractSqmSelfRenderingFunctionDescriptor selfRendering ) {
			return selfRendering;
		}
		throw new IllegalStateException( "Function 'max' is not a self rendering function" );
	}

	private static List<SelectableMapping> collectEntityKeySelectables(EntityMappingType entityDescriptor) {
		final var keySelectables = new ArrayList<SelectableMapping>();
		entityDescriptor.getIdentifierMapping().forEachSelectable(
				(selectionIndex, selectableMapping) -> {
					if ( !selectableMapping.isFormula() ) {
						keySelectables.add( selectableMapping );
					}
				}
		);
		return keySelectables;
	}

	private List<SelectableMapping> collectCollectionRowKeySelectables() {
		// The owning role defines the physical row key in both directions. Resolve its initialized mapping
		// through the persister since the mapping may be wrapped during model creation.
		final var collectionDescriptor = castNonNull( collectionMapping ).getCollectionDescriptor().getAttributeMapping();
		final var keySelectables = new ArrayList<SelectableMapping>();
		final var identifierDescriptor = collectionDescriptor.getIdentifierDescriptor();
		if ( identifierDescriptor != null ) {
			identifierDescriptor.forEachSelectable(
					(selectionIndex, selectableMapping) -> {
						if ( !selectableMapping.isFormula() ) {
							keySelectables.add( selectableMapping );
						}
					}
			);
			return keySelectables;
		}

		collectionDescriptor.getKeyDescriptor().getKeyPart().forEachSelectable(
				(selectionIndex, selectableMapping) -> {
					if ( !selectableMapping.isFormula() ) {
						keySelectables.add( selectableMapping );
					}
				}
		);

		final var indexDescriptor = collectionDescriptor.getIndexDescriptor();
		// the key of a map mapped by @MapKey is an attribute of the element,
		// which is not in the collection table, so the element identifies the row
		if ( indexDescriptor != null && collectionDescriptor.getIndexMetadata().getIndexPropertyName() == null ) {
			indexDescriptor.forEachSelectable(
					(selectionIndex, selectableMapping) -> {
						if ( !selectableMapping.isFormula() ) {
							keySelectables.add( selectableMapping );
						}
					}
			);
		}
		else if ( collectionDescriptor.getElementDescriptor() instanceof OneToManyCollectionPart oneToMany ) {
			oneToMany.getAssociatedEntityMappingType().getIdentifierMapping().forEachSelectable(
					(selectionIndex, selectableMapping) -> {
						if ( !selectableMapping.isFormula() ) {
							keySelectables.add( selectableMapping );
						}
					}
			);
		}
		else {
			collectionDescriptor.getElementDescriptor().forEachSelectable(
					(selectionIndex, selectableMapping) -> {
						if ( !selectableMapping.isFormula() ) {
							keySelectables.add( selectableMapping );
						}
					}
			);
		}
		return keySelectables;
	}

	@Override
	public void applyPredicate(
			@Nonnull EntityMappingType associatedEntityMappingType,
			@Nonnull Consumer<Predicate> predicateConsumer,
			@Nonnull LazyTableGroup lazyTableGroup,
			@Nonnull NavigablePath navigablePath,
			@Nonnull SqlAstCreationState creationState) {
		applyAssociationRestrictions( associatedEntityMappingType, lazyTableGroup,
				ownerRevision( navigablePath, creationState ), predicateConsumer, predicateConsumer,
				creationState.getSqlAliasBaseGenerator(), creationState.getLoadQueryInfluencers() );
	}

	@Override
	public void applyAssociationRestrictions(
			@Nonnull EntityMappingType associatedEntityDescriptor, @Nonnull TableGroup tableGroup,
			@Nullable Expression ownerRevision,
			@Nonnull Consumer<Predicate> rowRestrictions, @Nonnull Consumer<Predicate> ownerRestrictions,
			@Nonnull SqlAliasBaseGenerator aliases, @Nonnull LoadQueryInfluencers influencers) {
		if ( ownerRevision == null ) {
			applyPredicate( associatedEntityDescriptor, rowRestrictions, tableGroup, aliases, influencers );
		}
		else {
			applyAtRevision( associatedEntityDescriptor, associatedEntityDescriptor.getEntityPersister().getTableName(),
					collectEntityKeySelectables( associatedEntityDescriptor ), tableGroup, ownerRevision,
					rowRestrictions, ownerRestrictions, aliases );
		}
	}

	@Override
	public void applyAssociationRestrictions(
			@Nonnull PluralAttributeMapping collectionDescriptor, @Nonnull TableGroup tableGroup,
			@Nullable Expression ownerRevision,
			@Nonnull Consumer<Predicate> rowRestrictions, @Nonnull Consumer<Predicate> ownerRestrictions,
			@Nonnull SqlAliasBaseGenerator aliases, @Nonnull LoadQueryInfluencers influencers) {
		if ( ownerRevision == null ) {
			applyPredicate( collectionDescriptor, rowRestrictions, tableGroup, aliases, influencers );
		}
		else {
			applyAtRevision( collectionDescriptor, collectionDescriptor.getCollectionDescriptor().getTableName(),
					collectCollectionRowKeySelectables(), tableGroup, ownerRevision,
					rowRestrictions, ownerRestrictions, aliases );
		}
	}

	/**
	 * Restricts audited rows to their state as of the given revision of their owner. The
	 * bound on the revision refers to the owner. The check for a deleted row refers only
	 * to the audited row.
	 */
	private void applyAtRevision(
			TableGroupProducer tableGroupProducer, String table, List<SelectableMapping> keys,
			TableGroup tableGroup, Expression revision,
			Consumer<Predicate> rowRestrictions, Consumer<Predicate> ownerRestrictions,
			SqlAliasBaseGenerator aliases) {
		final var reference = tableGroup.resolveTableReference( resolveTableName( table ) );
		ownerRestrictions.accept( createRestriction( tableGroupProducer, reference, keys, aliases, table, revision, true ) );
		final var modificationType = getModificationTypeMapping( table );
		if ( modificationType != null ) {
			rowRestrictions.accept( excludeDeletions( reference, modificationType ) );
		}
	}

	@Override
	public void applyPredicate(
			@Nonnull EntityMappingType associatedEntityDescriptor,
			@Nonnull Consumer<Predicate> predicateConsumer,
			@Nonnull TableGroup tableGroup,
			@Nonnull SqlAliasBaseGenerator sqlAliasBaseGenerator,
			@Nonnull LoadQueryInfluencers influencers) {
		if ( hasTemporalPredicate( influencers ) ) {
			final var persister = associatedEntityDescriptor.getEntityPersister();
			final var info = resolveInfo( persister.getTableName() );
			predicateConsumer.accept( createRestriction( persister, tableGroup.resolveTableReference( info.auditTableName ),
					collectEntityKeySelectables( associatedEntityDescriptor ), sqlAliasBaseGenerator, info,
					resolveDefaultUpperBound( info ), false ) );
		}
	}

	@Override
	public void applyPredicate(
			@Nonnull PluralAttributeMapping collectionDescriptor,
			@Nonnull Consumer<Predicate> predicateConsumer,
			@Nonnull TableGroup tableGroup,
			@Nonnull SqlAliasBaseGenerator sqlAliasBaseGenerator,
			@Nonnull LoadQueryInfluencers influencers) {
		if ( hasTemporalPredicate( influencers ) ) {
			final var info = resolveInfo( collectionDescriptor.getCollectionDescriptor().getTableName() );
			predicateConsumer.accept( createRestriction( collectionDescriptor, tableGroup.resolveTableReference( info.auditTableName ),
					collectCollectionRowKeySelectables(), sqlAliasBaseGenerator, info,
					resolveDefaultUpperBound( info ), false ) );
		}
	}

	@Override
	public void applyPredicate(
			@Nonnull TableGroupJoin tableGroupJoin,
			@Nonnull SqlAliasBaseGenerator sqlAliasBaseGenerator,
			@Nonnull LoadQueryInfluencers loadQueryInfluencers) {
		if ( hasTemporalPredicate( loadQueryInfluencers )
				&& tableGroupJoin.getJoinedGroup().getModelPart() instanceof EntityValuedModelPart entityPart ) {
			final var entityDescriptor = entityPart.getEntityMappingType();
			final var persister = entityDescriptor.getEntityPersister();
			final var info = resolveInfo( persister.getTableName() );
			tableGroupJoin.applyPredicate( createRestriction(
					persister,
					tableGroupJoin.getJoinedGroup().resolveTableReference( info.auditTableName ),
					collectEntityKeySelectables( entityDescriptor ),
					sqlAliasBaseGenerator,
					info,
					resolveDefaultUpperBound( info ),
					false
			) );
		}
	}

	@Override
	public void applyPredicate(
			@Nonnull Supplier<Consumer<Predicate>> predicateCollector,
			@Nonnull SqlAstCreationState creationState,
			@Nonnull TableGroup tableGroup,
			@Nonnull NamedTableReference rootTableReference,
			@Nonnull EntityMappingType entityMappingType) {
		if ( hasTemporalPredicate( creationState.getLoadQueryInfluencers() ) ) {
			final String originalTable = entityMappingType.getEntityPersister().getTableName();
			final var info = resolveInfo( originalTable );
			predicateCollector.get().accept( createRestriction(
					entityMappingType.getEntityPersister(),
					rootTableReference,
					collectEntityKeySelectables( entityMappingType ),
					creationState.getSqlAliasBaseGenerator(),
					info,
					resolveDefaultUpperBound( info ),
					false
			) );
		}
	}

	@Override
	public void applyPredicate(
			@Nonnull TableReferenceJoin tableReferenceJoin,
			@Nonnull NamedTableReference primaryTableReference,
			@Nonnull String originalTableName,
			@Nonnull EntityMappingType entityMappingType,
			@Nonnull SqlAliasBaseGenerator sqlAliasBaseGenerator,
			@Nonnull LoadQueryInfluencers influencers) {
		if ( influencers.getTemporalIdentifier() != null ) {
			// Correlate REV between primary and joined tables
			final String primaryTable = entityMappingType.getMappedTableDetails().getTableName();
			final var primaryInfo = resolveInfo( primaryTable );
			final var joinedInfo = resolveInfo( originalTableName );
			tableReferenceJoin.applyPredicate( new ComparisonPredicate(
					new ColumnReference( primaryTableReference, primaryInfo.changesetIdMapping() ),
					EQUAL,
					new ColumnReference( tableReferenceJoin.getJoinedTableReference(), joinedInfo.changesetIdMapping() )
			) );
			// If the joined table carries REVTYPE (i.e. the root table), apply the DEL filter
			if ( joinedInfo.modificationTypeMapping() != null && hasTemporalPredicate( influencers ) ) {
				tableReferenceJoin.applyPredicate( new ComparisonPredicate(
						new ColumnReference( tableReferenceJoin.getJoinedTableReference(), joinedInfo.modificationTypeMapping() ),
						NOT_EQUAL,
						new JdbcLiteral<>( ModificationType.DEL, joinedInfo.modificationTypeMapping().getJdbcMapping() )
				) );
			}
		}
	}

	@Override
	public boolean hasRowRestriction(@Nonnull LoadQueryInfluencers influencers) {
		return useAuxiliaryTable( influencers );
	}
	@Override
	public boolean useAuxiliaryTable(@Nonnull LoadQueryInfluencers influencers) {
		return influencers.getTemporalIdentifier() != null;
	}

	@Override
	public boolean isAffectedByInfluencers(@Nonnull LoadQueryInfluencers influencers) {
		return isAffectedByRowRestrictions( influencers );
	}

	@Override
	public boolean isAffectedByRowRestrictions(@Nonnull LoadQueryInfluencers influencers) {
		return useAuxiliaryTable( influencers );
	}

	private boolean hasTemporalPredicate(LoadQueryInfluencers influencers) {
		return hasRowRestriction( influencers )
			&& !influencers.isAllRevisions();
	}
}
