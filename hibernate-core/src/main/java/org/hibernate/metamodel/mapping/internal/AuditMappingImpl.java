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
import org.hibernate.sql.ast.spi.creation.SqlAliasBaseManager;
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
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.annotation.Nullable;

import static org.hibernate.internal.util.NullnessUtil.castNonNull;
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
			@Nullable SelectableMapping invalidatingChangesetMapping
	) {}

	private final Map<String, TableAuditInfo> tableAuditInfoMap;

	private final JdbcMapping jdbcMapping;
	private final BasicType<?> changesetIdBasicType;
	@Nullable private final String currentTimestampFunctionName;
	private final FunctionRenderer maxFunctionDescriptor;
	private final AuditStrategy auditStrategy;

	private final EntityMappingType entityMappingType;
	private final SessionFactoryImplementor sessionFactory;
	@Nullable private AuditEntityLoader entityLoader;

	public AuditMappingImpl(
			Map<String, TableAuditInfo> tableAuditInfoMap,
			EntityMappingType entityMappingType,
			MappingModelCreationProcess creationProcess) {
		this.tableAuditInfoMap = Map.copyOf( tableAuditInfoMap );
		this.entityMappingType = entityMappingType;

		final var creationContext = creationProcess.getCreationContext();
		final var typeConfiguration = creationContext.getTypeConfiguration();
		this.sessionFactory = creationContext.getSessionFactory();
		this.auditStrategy = sessionFactory.getSessionFactoryOptions().getAuditStrategy();
		final var changesetIdJavaType = sessionFactory.getChangesetCoordinator().getIdentifierType();

		jdbcMapping = resolveJdbcMapping( typeConfiguration, changesetIdJavaType );
		changesetIdBasicType = resolveBasicType( typeConfiguration, changesetIdJavaType );

		final var dialect = sessionFactory.getJdbcServices().getDialect();
		currentTimestampFunctionName =
				sessionFactory.getChangesetCoordinator().useServerTimestamp( dialect )
						? dialect.getCurrentTemporalSupport().currentTimestamp()
						: null;

		maxFunctionDescriptor = resolveMaxFunction( sessionFactory );
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
		if ( usesValidityStrategy( info ) ) {
			return createValidityRestriction( tableReference, info, upperBound, includeDeletions );
		}
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

		// Main predicate: REV = (subquery) AND optionally REVTYPE <> DEL
		final var auditPredicate = new Junction( Junction.Nature.CONJUNCTION );
		auditPredicate.add( new ComparisonPredicate(
				new ColumnReference( tableReference, info.changesetIdMapping ),
				EQUAL,
				new SelectStatement( subQuerySpec )
		) );
		if ( !includeDeletions && info.modificationTypeMapping != null ) {
			auditPredicate.add( new ComparisonPredicate(
					new ColumnReference( tableReference, info.modificationTypeMapping ),
					NOT_EQUAL,
					new JdbcLiteral<>( ModificationType.DEL, info.modificationTypeMapping.getJdbcMapping() )
			) );
		}
		return auditPredicate;
	}

	private boolean usesValidityStrategy(TableAuditInfo info) {
		return auditStrategy == VALIDITY && info.invalidatingChangesetMapping != null;
	}

	/**
	 * Build the validity strategy restriction:
	 * {@code REV <= upperBound AND (REVEND > upperBound OR REVEND IS NULL) AND REVTYPE <> DEL}
	 */
	private static Predicate createValidityRestriction(
			TableReference tableReference,
			TableAuditInfo info,
			Expression upperBound,
			boolean includeDeletions) {
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

		// REVTYPE <> DEL (when applicable)
		if ( !includeDeletions && info.modificationTypeMapping != null ) {
			predicate.add( new ComparisonPredicate(
					new ColumnReference( tableReference, info.modificationTypeMapping ),
					NOT_EQUAL,
					new JdbcLiteral<>( ModificationType.DEL, info.modificationTypeMapping.getJdbcMapping() )
			) );
		}
		return predicate;
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

	private static JdbcMapping resolveJdbcMapping(
			TypeConfiguration typeConfiguration,
			Class<?> javaType) {
		final var basicType = typeConfiguration.getBasicTypeForJavaType( javaType );
		return basicType != null
				? basicType
				: typeConfiguration.standardBasicTypeForJavaType( javaType );
	}

	private static <J> BasicType<J> resolveBasicType(
			TypeConfiguration typeConfiguration,
			Class<J> javaType) {
		final var basicType = typeConfiguration.getBasicTypeForJavaType( javaType );
		return basicType == null
				? typeConfiguration.standardBasicTypeForJavaType( javaType )
				: basicType;
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

	private List<SelectableMapping> collectCollectionRowKeySelectables(PluralAttributeMapping collectionDescriptor) {
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
		if ( indexDescriptor != null ) {
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
		final var influencers = creationState.getLoadQueryInfluencers();
		final var persister = associatedEntityMappingType.getEntityPersister();
		final var info = resolveInfo( persister.getTableName() );
		if ( hasTemporalPredicate( influencers ) ) {
			predicateConsumer.accept( createRestriction(
					persister,
					lazyTableGroup.resolveTableReference( navigablePath, info.auditTableName ),
					collectEntityKeySelectables( associatedEntityMappingType ),
					creationState.getSqlAliasBaseGenerator(),
					info,
					resolveDefaultUpperBound( info ),
					false
			) );
		}
		else if ( influencers.isAllRevisions() ) {
			final var parentRevColumn = findParentRevColumn( navigablePath, creationState );
			if ( parentRevColumn != null ) {
				predicateConsumer.accept( createRestriction(
						persister,
						lazyTableGroup.resolveTableReference( navigablePath, info.auditTableName ),
						collectEntityKeySelectables( associatedEntityMappingType ),
						creationState.getSqlAliasBaseGenerator(),
						info,
						parentRevColumn,
						false
				) );
			}
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
			predicateConsumer.accept( createRestriction(
					persister,
					tableGroup.resolveTableReference( info.auditTableName ),
					collectEntityKeySelectables( associatedEntityDescriptor ),
					sqlAliasBaseGenerator,
					info,
					resolveDefaultUpperBound( info ),
					false
			) );
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
			final String collectionTable = collectionDescriptor.getCollectionDescriptor().getTableName();
			final var info = resolveInfo( collectionTable );
			predicateConsumer.accept( createRestriction(
					collectionDescriptor,
					tableGroup.resolveTableReference( info.auditTableName ),
					collectCollectionRowKeySelectables( collectionDescriptor ),
					sqlAliasBaseGenerator,
					info,
					resolveDefaultUpperBound( info ),
					false
			) );
		}
	}

	@Override
	public void applyPredicate(@Nonnull TableGroupJoin tableGroupJoin, @Nonnull LoadQueryInfluencers loadQueryInfluencers) {
		if ( hasTemporalPredicate( loadQueryInfluencers )
				&& tableGroupJoin.getJoinedGroup().getModelPart() instanceof EntityValuedModelPart entityPart ) {
			final var entityDescriptor = entityPart.getEntityMappingType();
			final var persister = entityDescriptor.getEntityPersister();
			final var info = resolveInfo( persister.getTableName() );
			tableGroupJoin.applyPredicate( createRestriction(
					persister,
					tableGroupJoin.getJoinedGroup().resolveTableReference( info.auditTableName ),
					collectEntityKeySelectables( entityDescriptor ),
					new SqlAliasBaseManager(),
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

	/**
	 * Walk up the navigable path to find a parent table group with an
	 * audit mapping, and return a column reference to its REV column.
	 */
	@Nullable
	private static ColumnReference findParentRevColumn(
			NavigablePath navigablePath,
			SqlAstCreationState creationState) {
		final var parentPath = navigablePath.getParent();
		if ( parentPath == null ) {
			return null;
		}
		final var parentTableGroup = creationState.getFromClauseAccess()
				.findTableGroup( parentPath );
		if ( parentTableGroup != null
				&& parentTableGroup.getModelPart() instanceof EntityValuedModelPart entityPart ) {
			final var parentAuditMapping = entityPart.getEntityMappingType().getAuditMapping();
			if ( parentAuditMapping != null ) {
				final String parentTable = entityPart.getEntityMappingType().getMappedTableDetails().getTableName();
				final String parentAuditTable = parentAuditMapping.resolveTableName( parentTable );
				return new ColumnReference(
						parentTableGroup.resolveTableReference( parentAuditTable ),
						parentAuditMapping.getChangesetIdMapping( parentTable )
				);
			}
		}
		return null;
	}

	@Override
	public boolean useAuxiliaryTable(@Nonnull LoadQueryInfluencers influencers) {
		return influencers.getTemporalIdentifier() != null;
	}

	@Override
	public boolean isAffectedByInfluencers(@Nonnull LoadQueryInfluencers influencers) {
		return influencers.getTemporalIdentifier() != null;
	}

	private static boolean hasTemporalPredicate(LoadQueryInfluencers influencers) {
		return influencers.getTemporalIdentifier() != null
			&& !influencers.isAllRevisions();
	}
}
