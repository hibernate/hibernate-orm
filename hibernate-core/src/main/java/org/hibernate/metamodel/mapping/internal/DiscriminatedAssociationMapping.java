/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.Column;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.DiscriminatedAssociationModelPart;
import org.hibernate.metamodel.mapping.DiscriminatorMapping;
import org.hibernate.metamodel.mapping.DiscriminatorValueDetails;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SelectablePath;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.from.TableGroupJoinProducer;
import org.hibernate.sql.ast.tree.from.StandardVirtualTableGroup;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.QueryLiteral;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.Junction;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.ast.tree.predicate.PredicateCollector;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.entity.internal.DiscriminatedEntityFetchJoinedImpl;
import org.hibernate.sql.results.graph.entity.internal.DiscriminatedEntityFetch;
import org.hibernate.sql.results.graph.entity.internal.DiscriminatedEntityResult;
import org.hibernate.type.AnyType;
import org.hibernate.type.BasicType;
import org.hibernate.type.MetaType;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.metamodel.mapping.internal.MappingModelCreationHelper.getSelectablePath;
import static org.hibernate.metamodel.mapping.internal.MappingModelCreationHelper.getTableIdentifierExpression;
import static org.hibernate.query.sqm.ComparisonOperator.EQUAL;

/**
 * Represents the "type" of an any-valued mapping
 *
 * @author Steve Ebersole
 */
public class DiscriminatedAssociationMapping implements MappingType, FetchOptions {

	public static DiscriminatedAssociationMapping from(
			NavigableRole containerRole,
			JavaType<?> baseAssociationJtd,
			DiscriminatedAssociationModelPart declaringModelPart,
			AnyType anyType,
			Any bootValueMapping,
			MappingModelCreationProcess creationProcess) {

		final var creationContext = creationProcess.getCreationContext();
		final var sessionFactory = creationContext.getSessionFactory();
		final var dialect = creationContext.getDialect();
		final String tableName =
				getTableIdentifierExpression( bootValueMapping.getTable(), creationProcess );

		assert bootValueMapping.getColumnSpan() == 2;
		final var columnIterator = bootValueMapping.getSelectables().iterator();

		assert columnIterator.hasNext();
		final var metaSelectable = columnIterator.next();
		assert columnIterator.hasNext();
		final var keySelectable = columnIterator.next();
		assert !columnIterator.hasNext();
		assert !metaSelectable.isFormula();
		assert !keySelectable.isFormula();
		final var metaColumn = (Column) metaSelectable;
		final var keyColumn = (Column) keySelectable;
		final var parentSelectablePath =
				declaringModelPart.asAttributeMapping() != null
						? getSelectablePath( declaringModelPart.asAttributeMapping().getDeclaringType() )
						: null;

		final var metaType = (MetaType) anyType.getDiscriminatorType();
		final var discriminatorPart = new AnyDiscriminatorPart(
				containerRole.append( AnyDiscriminatorPart.ROLE_NAME ),
				declaringModelPart,
				tableName,
				metaColumn.getText( dialect ),
				parentSelectablePath != null
						? parentSelectablePath.append( metaColumn.getQuotedName( dialect ) )
						: new SelectablePath( metaColumn.getQuotedName( dialect ) ),
				metaColumn.getCustomReadExpression(),
				metaColumn.getCustomWriteExpression(),
				metaColumn.getSqlType(),
				metaColumn.getLength(),
				metaColumn.getArrayLength(),
				metaColumn.getPrecision(),
				metaColumn.getScale(),
				bootValueMapping.isColumnInsertable( 0 ),
				bootValueMapping.isColumnUpdateable( 0 ),
				bootValueMapping.isPartitionKey(),
				(BasicType<?>) metaType.getBaseType(),
				metaType.getDiscriminatorValuesToEntityNameMap(),
				metaType.getImplicitValueStrategy(),
				sessionFactory.getMappingMetamodel()
		);


		final var keyType = (BasicType<?>) anyType.getIdentifierType();
		final var keyPart = new AnyKeyPart(
				containerRole.append( AnyKeyPart.KEY_NAME ),
				declaringModelPart,
				tableName,
				keyColumn.getText( dialect ),
				parentSelectablePath != null
						? parentSelectablePath.append( keyColumn.getQuotedName( dialect ) )
						: new SelectablePath( keyColumn.getQuotedName( dialect ) ),
				keyColumn.getCustomReadExpression(),
				keyColumn.getCustomWriteExpression(),
				keyColumn.getSqlType(),
				keyColumn.getLength(),
				keyColumn.getArrayLength(),
				keyColumn.getPrecision(),
				keyColumn.getScale(),
				bootValueMapping.isNullable(),
				bootValueMapping.isColumnInsertable( 1 ),
				bootValueMapping.isColumnUpdateable( 1 ),
				bootValueMapping.isPartitionKey(),
				keyType
		);

		return new DiscriminatedAssociationMapping(
				declaringModelPart,
				discriminatorPart,
				keyPart,
				baseAssociationJtd,
				bootValueMapping.isLazy()
						? FetchTiming.DELAYED
						: FetchTiming.IMMEDIATE,
				sessionFactory
		);
	}

	private final DiscriminatedAssociationModelPart modelPart;
	private final AnyDiscriminatorPart discriminatorPart;
	private final BasicValuedModelPart keyPart;
	private final JavaType<?> baseAssociationJtd;
	private final FetchTiming fetchTiming;
	private final SessionFactoryImplementor sessionFactory;

	public DiscriminatedAssociationMapping(
			DiscriminatedAssociationModelPart modelPart,
			AnyDiscriminatorPart discriminatorPart,
			BasicValuedModelPart keyPart,
			JavaType<?> baseAssociationJtd,
			FetchTiming fetchTiming,
			SessionFactoryImplementor sessionFactory) {
		this.modelPart = modelPart;
		this.discriminatorPart = discriminatorPart;
		this.keyPart = keyPart;
		this.baseAssociationJtd = baseAssociationJtd;
		this.fetchTiming = fetchTiming;
		this.sessionFactory = sessionFactory;
	}

	public DiscriminatedAssociationModelPart getModelPart() {
		return modelPart;
	}

	public DiscriminatorMapping getDiscriminatorPart() {
		return discriminatorPart;
	}

	public BasicValuedModelPart getKeyPart() {
		return keyPart;
	}

	public Object resolveDiscriminatorValueToEntityMapping(EntityMappingType entityMappingType) {
		final var details =
				discriminatorPart.getValueConverter()
						.getDetailsForEntityName( entityMappingType.getEntityName() );
		return details == null ? null : details.getValue();
	}

	public EntityMappingType resolveDiscriminatorValueToEntityMapping(Object discriminatorValue) {
		final var details =
				discriminatorPart.getValueConverter().
						getDetailsForDiscriminatorValue( discriminatorValue );
		return details == null ? null : details.getIndicatedEntity();
	}

	public <X, Y> int breakDownJdbcValues(
			int offset,
			X x,
			Y y,
			Object domainValue,
			ModelPart.JdbcValueBiConsumer<X, Y> valueConsumer,
			SharedSessionContractImplementor session) {
		if ( domainValue == null ) {
			valueConsumer.consume( offset, x, y, null, getDiscriminatorPart() );
			valueConsumer.consume( offset + 1, x, y, null, getKeyPart() );
			return getDiscriminatorPart().getJdbcTypeCount() + getKeyPart().getJdbcTypeCount();
		}
		else {
			final var concreteMappingType = determineConcreteType( domainValue, session );

			final Object discriminator = getModelPart().resolveDiscriminatorForEntityType( concreteMappingType );
			final Object disassembledDiscriminator = getDiscriminatorPart().disassemble( discriminator, session );
			valueConsumer.consume( offset, x, y, disassembledDiscriminator, getDiscriminatorPart() );

			final var identifierMapping = concreteMappingType.getIdentifierMapping();
			final Object identifier = identifierMapping.getIdentifier( domainValue );
			final Object disassembledKey = getKeyPart().disassemble( identifier, session );
			valueConsumer.consume( offset + 1, x, y, disassembledKey, getKeyPart() );
		}
		return getDiscriminatorPart().getJdbcTypeCount() + getKeyPart().getJdbcTypeCount();
	}

	public <X, Y> int decompose(
			int offset,
			X x,
			Y y,
			Object domainValue,
			ModelPart.JdbcValueBiConsumer<X, Y> valueConsumer,
			SharedSessionContractImplementor session) {
		if ( domainValue == null ) {
			valueConsumer.consume( offset, x, y, null, getDiscriminatorPart() );
			valueConsumer.consume( offset + 1, x, y, null, getKeyPart() );
		}
		else {
			final var concreteMappingType = determineConcreteType( domainValue, session );

			final Object discriminator = getModelPart().resolveDiscriminatorForEntityType( concreteMappingType );
			getDiscriminatorPart().decompose( discriminator, offset, x, y, valueConsumer, session );

			final var identifierMapping = concreteMappingType.getIdentifierMapping();
			final Object identifier = identifierMapping.getIdentifier( domainValue );
			getKeyPart().decompose( identifier, offset + 1, x, y, valueConsumer, session );
		}
		return getDiscriminatorPart().getJdbcTypeCount() + getKeyPart().getJdbcTypeCount();
	}

	private EntityMappingType determineConcreteType(Object entity, SharedSessionContractImplementor session) {
		final String entityName =
				session == null
						? sessionFactory.bestGuessEntityName( entity )
						: session.bestGuessEntityName( entity );
		return sessionFactory.getMappingMetamodel()
				.getEntityDescriptor( entityName );
	}

	public ModelPart findSubPart(String name, EntityMappingType treatTarget) {
		if ( AnyDiscriminatorPart.ROLE_NAME.equals( name ) ) {
			return getDiscriminatorPart();
		}

		if ( AnyKeyPart.KEY_NAME.equals( name ) ) {
			return getKeyPart();
		}

		if ( treatTarget != null ) {
			// make sure the treat-target is one of the mapped entities
			ensureMapped( treatTarget );

			return resolveAssociatedSubPart( name, treatTarget );
		}

		return discriminatorPart.getValueConverter().fromValueDetails( (detail) -> {
			try {
				final var subPart = resolveAssociatedSubPart( name, detail.getIndicatedEntity() );
				if ( subPart != null ) {
					return subPart;
				}
			}
			catch (Exception ignore) {
			}

			return null;
		} );
	}

	private ModelPart resolveAssociatedSubPart(String name, EntityMappingType entityMapping) {
		final var identifierMapping = entityMapping.getIdentifierMapping();

		if ( identifierMapping.getPartName().equals( name ) ) {
			return getKeyPart();
		}

		if ( identifierMapping instanceof SingleAttributeIdentifierMapping ) {
			final String idAttrName = identifierMapping.getAttributeName();
			if ( idAttrName.equals( name ) ) {
				return getKeyPart();
			}
		}

		return entityMapping.findSubPart( name );
	}

	private void ensureMapped(EntityMappingType treatTarget) {
		assert treatTarget != null;

		final var details =
				discriminatorPart.getValueConverter()
						.getDetailsForEntityName( treatTarget.getEntityName() );
		if ( details == null ) {
			throw new IllegalArgumentException(
					String.format(
							Locale.ROOT,
							"Treat-target [`%s`] is not not an entity mapped by ANY value : %s",
							treatTarget.getEntityName(),
							modelPart.getNavigableRole()
					)
			);
		}
	}

	public MappingType getPartMappingType() {
		return this;
	}

	public JavaType<?> getJavaType() {
		return baseAssociationJtd;
	}

	@Override
	public JavaType<?> getMappedJavaType() {
		return baseAssociationJtd;
	}

	@Override
	public FetchStyle getStyle() {
		return FetchStyle.SELECT;
	}

	@Override
	public FetchTiming getTiming() {
		return fetchTiming;
	}

	List<DiscriminatorValueDetails> getMappedEntityValueDetails() {
		final var valueDetails = new ArrayList<DiscriminatorValueDetails>();
		discriminatorPart.getValueConverter().forEachValueDetail( valueDetails::add );
		return valueDetails;
	}

	public static NavigablePath concreteEntityPath(NavigablePath associationPath, EntityMappingType entityMappingType) {
		return associationPath.treatAs( entityMappingType.getEntityName() );
	}

	TableGroup createRootTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			boolean fetched,
			SqlAstJoinType requestedJoinType,
			Consumer<Predicate> predicateConsumer,
			org.hibernate.sql.ast.spi.SqlAstCreationState creationState) {
		final var virtualTableGroup = new StandardVirtualTableGroup( navigablePath, modelPart, lhs, fetched );
		final var valueDetails = getMappedEntityValueDetails();
		final SqlAstJoinType effectiveJoinType =
				valueDetails.size() == 1 && requestedJoinType == SqlAstJoinType.INNER
						? SqlAstJoinType.INNER
						: SqlAstJoinType.LEFT;

		for ( DiscriminatorValueDetails valueDetail : valueDetails ) {
			addConcreteEntityTableGroupJoin(
					virtualTableGroup,
					lhs,
					navigablePath,
					valueDetail,
					effectiveJoinType,
					predicateConsumer,
					creationState
			);
		}

		return virtualTableGroup;
	}

	private void addConcreteEntityTableGroupJoin(
			StandardVirtualTableGroup virtualTableGroup,
			TableGroup lhs,
			NavigablePath associationPath,
			DiscriminatorValueDetails valueDetail,
			SqlAstJoinType joinType,
			Consumer<Predicate> predicateConsumer,
			org.hibernate.sql.ast.spi.SqlAstCreationState creationState) {
		final var entityMapping = valueDetail.getIndicatedEntity();
		final var concretePath = concreteEntityPath( associationPath, entityMapping );
		final var joinPredicateCollector = new PredicateCollector();

		final var entityTableGroup = entityMapping.createRootTableGroup(
				joinType == SqlAstJoinType.INNER && lhs.canUseInnerJoins(),
				concretePath,
				null,
				null,
				() -> joinPredicateCollector::applyPredicate,
				creationState
		);
		joinPredicateCollector.applyPredicate(
				createAssociationPredicate( lhs, entityTableGroup, entityMapping, valueDetail )
		);

		applyEntityRestrictions( joinPredicateCollector, entityMapping, entityTableGroup, creationState );

		final var tableGroupJoin = new TableGroupJoin(
				concretePath,
				joinType,
				entityTableGroup,
				joinPredicateCollector.getPredicate()
		);
		virtualTableGroup.addNestedTableGroupJoin( tableGroupJoin );
		creationState.getFromClauseAccess().registerTableGroup( concretePath, entityTableGroup );

		if ( predicateConsumer != null && joinPredicateCollector.getPredicate() != null ) {
			predicateConsumer.accept( joinPredicateCollector.getPredicate() );
		}
	}

	private void applyEntityRestrictions(
			PredicateCollector predicateCollector,
			EntityMappingType entityMappingType,
			TableGroup entityTableGroup,
			org.hibernate.sql.ast.spi.SqlAstCreationState creationState) {
		final Map<String, org.hibernate.Filter> enabledFilters = creationState.getLoadQueryInfluencers().getEnabledFilters();
		if ( entityMappingType.getEntityPersister().hasFilterForLoadByKey() ) {
			entityMappingType.applyBaseRestrictions(
					predicateCollector::applyPredicate,
					entityTableGroup,
					true,
					enabledFilters,
					creationState.applyOnlyLoadByKeyFilters(),
					null,
					creationState
			);
		}
		entityMappingType.applyWhereRestrictions(
				predicateCollector::applyPredicate,
				entityTableGroup,
				true,
				creationState
		);
		if ( entityMappingType.getSuperMappingType() != null && !creationState.supportsEntityNameUsage() ) {
			entityMappingType.applyDiscriminator( null, null, entityTableGroup, creationState );
		}
		final var auxiliaryMapping = entityMappingType.getAuxiliaryMapping();
		if ( auxiliaryMapping != null ) {
			auxiliaryMapping.applyPredicate(
					entityMappingType,
					predicateCollector::applyPredicate,
					entityTableGroup,
					creationState.getSqlAliasBaseGenerator(),
					creationState.getLoadQueryInfluencers()
			);
		}
	}

	private Predicate createAssociationPredicate(
			TableGroup lhs,
			TableGroup entityTableGroup,
			EntityMappingType entityMappingType,
			DiscriminatorValueDetails valueDetail) {
		final var identifierMapping = entityMappingType.getIdentifierMapping();
		final BasicValuedModelPart identifierPart = identifierMapping.asBasicValuedModelPart();
		if ( identifierPart == null ) {
			throw new UnsupportedOperationException(
					"Join fetching an @Any association is not supported for entity '" + entityMappingType.getEntityName()
							+ "' because it does not use a basic identifier"
			);
		}

		final TableReference discriminatorTableReference =
				lhs.resolveTableReference( null, discriminatorPart.getContainingTableExpression() );
		final TableReference keyTableReference =
				lhs.resolveTableReference( null, keyPart.getContainingTableExpression() );
		final TableReference identifierTableReference =
				entityTableGroup.resolveTableReference( entityTableGroup.getNavigablePath(), identifierPart.getContainingTableExpression() );

		final Junction predicate = new Junction( Junction.Nature.CONJUNCTION );
		predicate.add(
				new ComparisonPredicate(
						new ColumnReference( discriminatorTableReference, discriminatorPart ),
						EQUAL,
						new QueryLiteral<>( valueDetail.getValue(), discriminatorPart )
				)
		);
		predicate.add(
				new ComparisonPredicate(
						new ColumnReference( identifierTableReference, identifierPart ),
						EQUAL,
						new ColumnReference( keyTableReference, keyPart )
				)
		);
		return predicate;
	}

	private TableGroup resolveJoinedFetchTableGroup(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			String resultVariable,
			DomainResultCreationState creationState) {
		final FromClauseAccess fromClauseAccess = creationState.getSqlAstCreationState().getFromClauseAccess();
		return fromClauseAccess.resolveTableGroup(
				fetchablePath,
				navigablePath -> {
					final TableGroup parentTableGroup = fromClauseAccess.getTableGroup( fetchParent.getNavigablePath() );
					final TableGroupJoin tableGroupJoin = ( (TableGroupJoinProducer) modelPart ).createTableGroupJoin(
							navigablePath,
							parentTableGroup,
							resultVariable,
							null,
							SqlAstJoinType.LEFT,
							true,
							false,
							creationState.getSqlAstCreationState()
					);
					parentTableGroup.addTableGroupJoin( tableGroupJoin );
					return tableGroupJoin.getJoinedGroup();
				}
		);
	}

	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState) {
		if ( selected ) {
			resolveJoinedFetchTableGroup( fetchParent, fetchablePath, resultVariable, creationState );
			return new DiscriminatedEntityFetchJoinedImpl(
					fetchablePath,
					baseAssociationJtd,
					modelPart,
					fetchTiming,
					fetchParent,
					getMappedEntityValueDetails(),
					creationState
			);
		}
		return new DiscriminatedEntityFetch(
				fetchablePath,
				baseAssociationJtd,
				modelPart,
				fetchTiming,
				fetchParent,
				creationState
		);
	}

	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		return new DiscriminatedEntityResult<>(
				navigablePath,
				baseAssociationJtd,
				modelPart,
				resultVariable,
				creationState
		);
	}

}
