/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.internal;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.hibernate.AssertionFailure;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.Stack;
import org.hibernate.jpa.spi.JpaCompliance;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.Bindable;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EntityAssociationMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressible;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.metamodel.model.domain.BasicDomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.IdentifiableDomainType;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.metamodel.model.domain.SingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.internal.EntitySqmPathSource;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.query.IllegalMutationQueryException;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.IllegalSelectQueryException;
import org.hibernate.query.Order;
import org.hibernate.query.QueryTypeMismatchException;
import org.hibernate.query.criteria.JpaRoot;
import org.hibernate.query.criteria.JpaSelection;
import org.hibernate.query.restriction.Restriction;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.SqmQuerySource;
import org.hibernate.query.sqm.spi.JdbcParameterBySqmParameterAccess;
import org.hibernate.query.sqm.spi.SqmParameterMappingModelResolutionAccess;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmDeleteOrUpdateStatement;
import org.hibernate.query.sqm.tree.SqmDmlStatement;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.domain.SqmSingularJoin;
import org.hibernate.query.sqm.tree.expression.JpaCriteriaParameter;
import org.hibernate.query.sqm.tree.expression.SqmAliasedNodeRef;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmJpaCriteriaParameterWrapper;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.expression.SqmTuple;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.query.sqm.tree.predicate.SqmWhereClause;
import org.hibernate.query.sqm.tree.select.SqmOrderByClause;
import org.hibernate.query.sqm.tree.select.SqmQueryGroup;
import org.hibernate.query.sqm.tree.select.SqmQueryPart;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.query.sqm.tree.select.SqmSelectableNode;
import org.hibernate.query.sqm.tree.select.SqmSelection;
import org.hibernate.query.sqm.tree.select.SqmSortSpecification;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlTreeCreationException;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.type.JavaObjectType;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.PrimitiveJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.type.internal.ConvertedBasicTypeImpl;

import jakarta.persistence.Tuple;
import jakarta.persistence.metamodel.Type;
import org.checkerframework.checker.nullness.qual.Nullable;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static org.hibernate.internal.util.NullnessUtil.castNonNull;
import static org.hibernate.internal.util.collections.CollectionHelper.arrayList;
import static org.hibernate.internal.util.collections.CollectionHelper.determineProperSizing;
import static org.hibernate.query.sqm.tree.jpa.ParameterCollector.collectParameters;

/**
 * Helper utilities for dealing with SQM
 *
 * @author Steve Ebersole
 */
public class SqmUtil {
	private SqmUtil() {
	}

	public static boolean isSelect(SqmStatement<?> sqm) {
		return sqm instanceof SqmSelectStatement;
	}

	public static boolean isMutation(SqmStatement<?> sqm) {
		return sqm instanceof SqmDmlStatement;
	}

	public static <T> boolean isRestrictedMutation(SqmStatement<T> sqmStatement) {
		return sqmStatement instanceof SqmDeleteOrUpdateStatement;
	}

	public static void verifyIsSelectStatement(SqmStatement<?> sqm, String hqlString) {
		if ( ! isSelect( sqm ) ) {
			throw new IllegalSelectQueryException(
					String.format(
							Locale.ROOT,
							"Expecting a SELECT Query [%s], but found %s",
							SqmSelectStatement.class.getName(),
							sqm.getClass().getName()
					),
					hqlString
			);
		}
	}

	public static void verifyIsNonSelectStatement(SqmStatement<?> sqm, String hqlString) {
		if ( ! isMutation( sqm ) ) {
			throw expectingNonSelect( sqm, hqlString );
		}
	}

	public static IllegalQueryOperationException expectingNonSelect(SqmStatement<?> sqm, String hqlString) {
		return new IllegalQueryOperationException(
				String.format(
						Locale.ROOT,
						"Expecting a non-SELECT Query [%s], but found %s",
						SqmDmlStatement.class.getName(),
						sqm.getClass().getName()
				),
				hqlString,
				null
		);
	}

	public static void verifyIsRestrictedMutation(SqmStatement<?> sqm, String hqlString) {
		if ( ! isRestrictedMutation( sqm ) ) {
			throw new IllegalMutationQueryException(
					String.format(
							Locale.ROOT,
							"Expecting a restricted mutation query [%s], but found %s",
							SqmDeleteOrUpdateStatement.class.getName(),
							sqm.getClass().getName()
					),
					hqlString
			);
		}
	}

	public static @Nullable String determineAffectedTableName(TableGroup tableGroup, ValuedModelPart mapping) {
		return tableGroup.getModelPart() instanceof EntityAssociationMapping associationMapping
			&& !associationMapping.containsTableReference( mapping.getContainingTableExpression() )
				? associationMapping.getAssociatedEntityMappingType().getMappedTableDetails().getTableName()
				: null;
	}

	/**
	 * Utility that returns the entity association target's mapping type if the specified {@code sqmPath} should
	 * be dereferenced using the target table, i.e. when the path's lhs is an explicit join that is used in the
	 * group by clause, or defaults to the provided {@code modelPartContainer} otherwise.
	 */
	public static ModelPartContainer getTargetMappingIfNeeded(
			SqmPath<?> sqmPath,
			ModelPartContainer modelPartContainer,
			SqmToSqlAstConverter sqlAstCreationState) {
		// We only need to do this for queries
		if ( sqlAstCreationState.getCurrentClauseStack().getCurrent() != Clause.FROM
				&& modelPartContainer.getPartMappingType() != modelPartContainer
				&& sqmPath.getLhs() instanceof SqmFrom<?, ?> ) {
			final ModelPart modelPart =
					modelPartContainer instanceof PluralAttributeMapping plural
							? getCollectionPart( plural, castNonNull( sqmPath.getNavigablePath().getParent() ) )
							: modelPartContainer;
			if ( modelPart instanceof EntityAssociationMapping association
					&& shouldRenderTargetSide( sqmPath, association, sqlAstCreationState ) ) {
				return association.getAssociatedEntityMappingType();
			}
		}
		return modelPartContainer;
	}

	private static boolean shouldRenderTargetSide(
			SqmPath<?> sqmPath,
			EntityAssociationMapping association,
			SqmToSqlAstConverter sqlAstCreationState) {
		if ( !association.getTargetKeyPropertyNames().contains( sqmPath.getReferencedPathSource().getPathName() ) ) {
			return false;
		}
		else {
			// If the path is one of the association's target key properties,
			// we need to render the target side if in group/order by
			final Clause clause = sqlAstCreationState.getCurrentClauseStack().getCurrent();
			return clause == Clause.GROUP || clause == Clause.ORDER
				|| !isFkOptimizationAllowed( sqmPath.getLhs(), association )
				|| inGroupByOrOrderBy( sqmPath, sqlAstCreationState );
		}
	}

	private static boolean inGroupByOrOrderBy(SqmPath<?> sqmPath, SqmToSqlAstConverter converter) {
		final Stack<SqmQueryPart> queryPartStack = converter.getSqmQueryPartStack();
		final NavigablePath np = sqmPath.getNavigablePath();
		final Boolean found = queryPartStack.findCurrentFirst( queryPart -> {
			final SqmQuerySpec<?> spec = queryPart.getFirstQuerySpec();
			return spec.groupByClauseContains( np, converter )
				|| spec.orderByClauseContains( np, converter )
					? true : null;
		} );
		return Boolean.TRUE.equals( found );
	}

	private static CollectionPart getCollectionPart(PluralAttributeMapping attribute, NavigablePath path) {
		final CollectionPart.Nature nature = CollectionPart.Nature.fromNameExact( path.getLocalName() );
		return nature == null ? null : switch ( nature ) {
			case ELEMENT -> attribute.getElementDescriptor();
			case INDEX -> attribute.getIndexDescriptor();
			case ID -> attribute.getIdentifierDescriptor();
		};
	}

	/**
	 * Utility that returns {@code false} when the provided {@link SqmPath sqmPath} is
	 * a join that cannot be dereferenced through the foreign key on the associated table,
	 * i.e. a join that's neither {@linkplain SqmJoinType#INNER} nor {@linkplain SqmJoinType#LEFT}
	 * or one that has an explicit on clause predicate.
	 *
	 * @deprecated Use {@link #isFkOptimizationAllowed(SqmPath, EntityAssociationMapping)} instead
	 */
	@Deprecated(forRemoval = true, since = "6.6.1")
	public static boolean isFkOptimizationAllowed(SqmPath<?> sqmPath) {
		if ( sqmPath instanceof SqmJoin<?, ?> sqmJoin ) {
			switch ( sqmJoin.getSqmJoinType() ) {
				case LEFT:
					final EntityAssociationMapping associationMapping = resolveAssociationMapping( sqmJoin );
					if ( associationMapping != null && isFiltered( associationMapping ) ) {
						return false;
					}
					// FallThrough intended
				case INNER:
					return sqmJoin.getJoinPredicate() == null;
				default:
					return false;
			}
		}
		return false;
	}

	/**
	 * Utility that returns {@code false} when the provided {@link SqmPath sqmPath} is
	 * a join that cannot be dereferenced through the foreign key on the associated table,
	 * i.e. a join that's neither {@linkplain SqmJoinType#INNER} nor {@linkplain SqmJoinType#LEFT}
	 * or one that has an explicit on clause predicate.
	 */
	public static boolean isFkOptimizationAllowed(SqmPath<?> sqmPath, EntityAssociationMapping associationMapping) {
		// By default, never allow the FK optimization if the path is a join, unless the association has a join table
		// Hibernate ORM has no way for users to refer to collection/join table rows,
		// so referring the columns of these rows by default when requesting FK column attributes is sensible.
		// Users that need to refer to the actual target table columns will have to add an explicit entity join.
		if ( associationMapping.isFkOptimizationAllowed()
			&& sqmPath instanceof SqmJoin<?, ?> sqmJoin
			&& hasJoinTable( associationMapping ) ) {
			switch ( sqmJoin.getSqmJoinType() ) {
				case LEFT:
					if ( isFiltered( associationMapping ) ) {
						return false;
					}
					// FallThrough intended
				case INNER:
					return sqmJoin.getJoinPredicate() == null;
				default:
					return false;
			}
		}
		return false;
	}

	private static boolean hasJoinTable(EntityAssociationMapping associationMapping) {
		if ( associationMapping instanceof CollectionPart collectionPart ) {
			return !collectionPart.getCollectionAttribute().getCollectionDescriptor().isOneToMany();
		}
		else if ( associationMapping instanceof ToOneAttributeMapping toOneAttributeMapping ) {
			return toOneAttributeMapping.hasJoinTable();
		}
		return false;
	}

	private static boolean isFiltered(EntityAssociationMapping associationMapping) {
			final EntityMappingType entityMappingType = associationMapping.getAssociatedEntityMappingType();
			return !associationMapping.isFkOptimizationAllowed()
				// When the identifier mappings are different we have a joined subclass entity
				// which will filter rows based on a discriminator predicate
				|| entityMappingType.getIdentifierMappingForJoin() != entityMappingType.getIdentifierMapping();
	}

	private static @Nullable EntityAssociationMapping resolveAssociationMapping(SqmJoin<?, ?> sqmJoin) {
		return sqmJoin instanceof SqmSingularJoin<?, ?> singularJoin
			&& singularJoin.getAttribute().getPathType() instanceof EntityDomainType<?>
				? resolveAssociationMapping( singularJoin )
				: null;
	}

	private static @Nullable EntityAssociationMapping resolveAssociationMapping(SqmSingularJoin<?, ?> sqmJoin) {
		final MappingMetamodelImplementor metamodel = sqmJoin.nodeBuilder().getMappingMetamodel();
		SingularPersistentAttribute<?, ?> attribute = sqmJoin.getAttribute();
		ManagedDomainType<?> declaringType = attribute.getDeclaringType();
		if ( declaringType.getPersistenceType() != Type.PersistenceType.ENTITY ) {
			final StringBuilder pathBuilder = new StringBuilder();
			do {
				if ( !pathBuilder.isEmpty() ) {
					pathBuilder.insert(0, '.');
				}
				pathBuilder.insert( 0, attribute.getName() );
				if ( sqmJoin.getLhs() instanceof SqmSingularJoin<?, ?> sqmSingularJoin ) {
					sqmJoin = sqmSingularJoin;
					attribute = sqmJoin.getAttribute();
					declaringType = attribute.getDeclaringType();
				}
				else {
					return null;
				}
			} while ( declaringType.getPersistenceType() != Type.PersistenceType.ENTITY );
			pathBuilder.insert(0, '.');
			pathBuilder.insert( 0, attribute.getName() );
			return (EntityAssociationMapping)
					metamodel.getEntityDescriptor( ( (EntityDomainType<?>) declaringType ).getHibernateEntityName() )
							.findByPath( pathBuilder.toString() );
		}
		else {
			return (EntityAssociationMapping)
					metamodel.getEntityDescriptor( ( (EntityDomainType<?>) declaringType ).getHibernateEntityName() )
							.findAttributeMapping( attribute.getName() );
		}
	}

	public static List<NavigablePath> getWhereClauseNavigablePaths(SqmQuerySpec<?> querySpec) {
		final SqmWhereClause where = querySpec.getWhereClause();
		return where == null || where.getPredicate() == null
				? emptyList()
				: collectNavigablePaths( List.of( where.getPredicate() ) );

	}

	public static List<NavigablePath> getGroupByNavigablePaths(SqmQuerySpec<?> querySpec) {
		final List<SqmExpression<?>> expressions = querySpec.getGroupByClauseExpressions();
		return expressions.isEmpty() ? emptyList() : collectNavigablePaths( expressions );

	}

	public static List<NavigablePath> getOrderByNavigablePaths(SqmQuerySpec<?> querySpec) {
		final SqmOrderByClause order = querySpec.getOrderByClause();
		if ( order == null || order.getSortSpecifications().isEmpty() ) {
			return emptyList();
		}
		else {
			final List<SqmExpression<?>> expressions =
					order.getSortSpecifications().stream()
							.map( SqmSortSpecification::getSortExpression )
							.collect( toList() );
			return collectNavigablePaths( expressions );
		}
	}

	private static List<NavigablePath> collectNavigablePaths(final List<SqmExpression<?>> expressions) {
		final List<NavigablePath> navigablePaths = arrayList( expressions.size() );
		final SqmPathVisitor pathVisitor = new SqmPathVisitor( path -> navigablePaths.add( path.getNavigablePath() ) );
		for ( final SqmExpression<?> expression : expressions ) {
			if ( expression instanceof SqmAliasedNodeRef sqmAliasedNodeRef ) {
				final NavigablePath navigablePath = sqmAliasedNodeRef.getNavigablePath();
				if ( navigablePath != null ) {
					navigablePaths.add( navigablePath );
				}
			}
			else {
				expression.accept( pathVisitor );
			}
		}
		return navigablePaths;
	}

	public static <T, A> SqmAttributeJoin<T, A> findCompatibleFetchJoin(
			SqmFrom<?, T> sqmFrom,
			SqmPathSource<A> pathSource,
			SqmJoinType requestedJoinType) {
		for ( final SqmJoin<T, ?> join : sqmFrom.getSqmJoins() ) {
			if ( join.getModel() == pathSource ) {
				final SqmAttributeJoin<T, ?> attributeJoin = (SqmAttributeJoin<T, ?>) join;
				if ( attributeJoin.isFetched() ) {
					final SqmJoinType joinType = join.getSqmJoinType();
					if ( joinType != requestedJoinType ) {
						throw new IllegalStateException( String.format(
								"Requested join fetch with association [%s] with '%s' join type, " +
										"but found existing join fetch with '%s' join type.",
								pathSource.getPathName(),
								requestedJoinType,
								joinType
						) );
					}
					//noinspection unchecked
					return (SqmAttributeJoin<T, A>) attributeJoin;
				}
			}
		}
		return null;
	}

	public static Map<QueryParameterImplementor<?>, Map<SqmParameter<?>, List<JdbcParametersList>>> generateJdbcParamsXref(
			DomainParameterXref domainParameterXref,
			JdbcParameterBySqmParameterAccess jdbcParameterBySqmParameterAccess) {
		if ( domainParameterXref == null || !domainParameterXref.hasParameters() ) {
			return emptyMap();
		}
		else {
			final Map<QueryParameterImplementor<?>, Map<SqmParameter<?>, List<JdbcParametersList>>> result =
					new IdentityHashMap<>( domainParameterXref.getQueryParameterCount() );
			domainParameterXref.getQueryParameters().forEach( (queryParam, sqmParams) -> {
				final Map<SqmParameter<?>, List<JdbcParametersList>> sqmParamMap =
						result.computeIfAbsent( queryParam, qp -> new IdentityHashMap<>( sqmParams.size() ) );
				for ( SqmParameter<?> sqmParam : sqmParams ) {
					final var jdbcParamsBySqmParam = jdbcParameterBySqmParameterAccess.getJdbcParamsBySqmParam();
					sqmParamMap.put( sqmParam, convert( jdbcParamsBySqmParam.get( sqmParam ) ) );
					for ( SqmParameter<?> expansion : domainParameterXref.getExpansions( sqmParam ) ) {
						sqmParamMap.put( expansion, convert( jdbcParamsBySqmParam.get( expansion ) ) );
						result.put( queryParam, sqmParamMap );
					}
				}
			} );
			return result;
		}
	}

	private static List<JdbcParametersList> convert(final List<List<JdbcParameter>> lists) {
		if ( lists == null ) {
			return null;
		}
		else {
			final List<JdbcParametersList> output = new ArrayList<>( lists.size() );
			for ( List<JdbcParameter> element : lists ) {
				output.add( JdbcParametersList.fromList( element ) );
			}
			return output;
		}
	}

//	public static JdbcParameterBindings buildJdbcParameterBindings(
//			SqmStatement sqmStatement,
//			JdbcParameterBySqmParameterAccess sqmInterpretation,
//			ExecutionContext executionContext) {
//		final DomainParameterXref domainParameterXref = DomainParameterXref.from( sqmStatement );
//		final Map<QueryParameterImplementor<?>, Map<SqmParameter, List<JdbcParameter>>> jdbcParamsXref =
//				generateJdbcParamsXref( domainParameterXref, sqmInterpretation );
//		return createJdbcParameterBindings(
//				executionContext.getDomainParameterBindingContext().getQueryParameterBindings(),
//				domainParameterXref,
//				jdbcParamsXref,
//				executionContext.getSession()
//		);
//	}

//	public static JdbcParameterBindings buildJdbcParameterBindings(
//			SqmStatement sqmStatement,
//			Map<QueryParameterImplementor<?>, Map<SqmParameter, List<JdbcParameter>>> jdbcParamsXref,
//			ExecutionContext executionContext) {
//		final DomainParameterXref domainParameterXref = DomainParameterXref.from( sqmStatement );
//		return createJdbcParameterBindings(
//				executionContext.getDomainParameterBindingContext().getQueryParameterBindings(),
//				domainParameterXref,
//				jdbcParamsXref,
//				executionContext.getSession()
//		);
//	}

//	public static JdbcParameterBindings buildJdbcParameterBindings(
//			DomainParameterXref domainParameterXref,
//			Map<QueryParameterImplementor<?>, Map<SqmParameter, List<JdbcParameter>>> jdbcParamsXref,
//			ExecutionContext executionContext) {
//		return createJdbcParameterBindings(
//				executionContext.getDomainParameterBindingContext().getQueryParameterBindings(),
//				domainParameterXref,
//				jdbcParamsXref,
//				executionContext.getSession()
//		);
//	}

	public static JdbcParameterBindings createJdbcParameterBindings(
			QueryParameterBindings domainParamBindings,
			DomainParameterXref domainParameterXref,
			Map<QueryParameterImplementor<?>, Map<SqmParameter<?>, List<JdbcParametersList>>> jdbcParamXref,
			SqmParameterMappingModelResolutionAccess mappingModelResolutionAccess,
			SharedSessionContractImplementor session) {
		final JdbcParameterBindings jdbcParameterBindings =
				new JdbcParameterBindingsImpl( domainParameterXref.getSqmParameterCount() );
		domainParameterXref.getQueryParameters()
				.forEach( (queryParameter, sqmParameters) ->
						createJdbcParameterBinding(
								domainParamBindings,
								domainParameterXref,
								jdbcParamXref,
								mappingModelResolutionAccess,
								session,
								queryParameter,
								sqmParameters,
								jdbcParameterBindings
						) );
		return jdbcParameterBindings;
	}

	private static <T> void createJdbcParameterBinding(
			QueryParameterBindings domainParamBindings,
			DomainParameterXref domainParameterXref,
			Map<QueryParameterImplementor<?>, Map<SqmParameter<?>, List<JdbcParametersList>>> jdbcParamXref,
			SqmParameterMappingModelResolutionAccess modelResolutionAccess,
			SharedSessionContractImplementor session,
			QueryParameterImplementor<T> queryParameter,
			List<SqmParameter<?>> sqmParameters,
			JdbcParameterBindings jdbcParameterBindings) {
		final QueryParameterBinding<T> domainParamBinding = domainParamBindings.getBinding( queryParameter );
		final var jdbcParamMap = jdbcParamXref.get( queryParameter );
		for ( SqmParameter<?> sqmParameter : sqmParameters ) {
			final MappingModelExpressible<T> resolvedMappingModelType =
					modelResolutionAccess.getResolvedMappingModelType( checkParameter( queryParameter, sqmParameter ) );
			if ( resolvedMappingModelType != null ) {
				domainParamBinding.setType( resolvedMappingModelType );
			}
			final Bindable parameterType =
					determineParameterType( domainParamBinding, queryParameter, sqmParameters, modelResolutionAccess,
							session.getFactory() );

			final List<JdbcParametersList> jdbcParamsBinds = jdbcParamMap.get( sqmParameter );
			if ( jdbcParamsBinds == null ) {
				// This can happen when a group or order by item expression, that contains parameters,
				// is replaced with an alias reference expression, which can happen for JPA Criteria queries
			}
			else if ( !domainParamBinding.isBound() ) {
				for ( int i = 0; i < jdbcParamsBinds.size(); i++ ) {
					final JdbcParametersList jdbcParams = jdbcParamsBinds.get( i );
					parameterType.forEachJdbcType( (position, jdbcMapping) ->
							jdbcParameterBindings.addBinding( jdbcParams.get( position ),
									new JdbcParameterBindingImpl( jdbcMapping, null ) ) );
				}
			}
			else if ( domainParamBinding.isMultiValued() ) {
				final Collection<?> bindValues = domainParamBinding.getBindValues();
				final Iterator<?> valueIterator = bindValues.iterator();
				// the original SqmParameter is the one we are processing - create a binding for it
				final Object firstValue = valueIterator.next();
				for ( int i = 0; i < jdbcParamsBinds.size(); i++ ) {
					final JdbcParametersList jdbcParams = jdbcParamsBinds.get( i );
					createValueBindings(
							jdbcParameterBindings,
							queryParameter,
							domainParamBinding,
							parameterType,
							jdbcParams,
							firstValue,
							session
					);
				}

				// and then one for each of the expansions
				final List<SqmParameter<?>> expansions = domainParameterXref.getExpansions( sqmParameter );
				final int expansionCount = bindValues.size() - 1;
				final int parameterUseCount = jdbcParamsBinds.size();
				assert expansions.size() == expansionCount * parameterUseCount;
				int expansionPosition = 0;
				while ( valueIterator.hasNext() ) {
					final Object expandedValue = valueIterator.next();
					for ( int j = 0; j < parameterUseCount; j++ ) {
						final SqmParameter<?> expansionSqmParam = expansions.get( expansionPosition + j * expansionCount );
						final List<JdbcParametersList> jdbcParamBinds = jdbcParamMap.get( expansionSqmParam );
						for ( int i = 0; i < jdbcParamBinds.size(); i++ ) {
							final JdbcParametersList expansionJdbcParams = jdbcParamBinds.get( i );
							createValueBindings(
									jdbcParameterBindings,
									queryParameter,
									domainParamBinding,
									parameterType,
									expansionJdbcParams,
									expandedValue,
									session
							);
						}
					}
					expansionPosition++;
				}
			}
			else {
				final JdbcMapping jdbcMapping = jdbcMapping( domainParamBinding );
				final BasicValueConverter valueConverter =
						jdbcMapping == null ? null : jdbcMapping.getValueConverter();
				if ( valueConverter != null ) {
					final Object convertedValue =
							valueConverter.toRelationalValue( domainParamBinding.getBindValue() );
					for ( int i = 0; i < jdbcParamsBinds.size(); i++ ) {
						final JdbcParametersList jdbcParams = jdbcParamsBinds.get( i );
						assert jdbcParams.size() == 1;
						final JdbcParameter jdbcParameter = jdbcParams.get( 0 );
						jdbcParameterBindings.addBinding( jdbcParameter,
								new JdbcParameterBindingImpl( jdbcMapping, convertedValue ) );
					}
				}
				else {
					final Object bindValue = domainParamBinding.getBindValue();
					if ( bindValue == null ) {
						for ( int i = 0; i < jdbcParamsBinds.size(); i++ ) {
							final JdbcParametersList jdbcParams = jdbcParamsBinds.get( i );
							for ( int j = 0; j < jdbcParams.size(); j++ ) {
								final JdbcParameter jdbcParameter = jdbcParams.get( j );
								jdbcParameterBindings.addBinding( jdbcParameter,
										new JdbcParameterBindingImpl( jdbcMapping, bindValue ) );
							}
						}
					}
					else {
						for ( int i = 0; i < jdbcParamsBinds.size(); i++ ) {
							final JdbcParametersList jdbcParams = jdbcParamsBinds.get( i );
							createValueBindings(
									jdbcParameterBindings,
									queryParameter,
									domainParamBinding,
									parameterType,
									jdbcParams,
									bindValue,
									session
							);
						}
					}
				}
			}
		}
	}

	private static <T> SqmParameter<T> checkParameter
			(QueryParameterImplementor<T> queryParameter, SqmParameter<?> sqmParameter) {
		// TODO: currently no robust way to check the types
//		assert type == sqmParameter.getParameterType()
//			|| sqmParameter.getParameterType().isAssignableFrom( queryParameter.getParameterType() );
		assert sqmParameter.getName() == null
			|| sqmParameter.getName().equals( queryParameter.getName() );
		assert sqmParameter.getPosition() == null
			|| sqmParameter.getPosition().equals( queryParameter.getPosition() );
		@SuppressWarnings("unchecked") // not great!
		final SqmParameter<T> checkedSqmParameter = (SqmParameter<T>) sqmParameter;
		return checkedSqmParameter;
	}

	private static <T> JdbcMapping jdbcMapping(QueryParameterBinding<T> domainParamBinding) {
		if ( domainParamBinding.getType() instanceof JdbcMapping mapping ) {
			return mapping;
		}
		// TODO: why do the test and the cast disagree here? getBindType() vs getType()
		else if ( domainParamBinding.getBindType() instanceof BasicValuedMapping ) {
			return ( (BasicValuedMapping) domainParamBinding.getType() ).getJdbcMapping();
		}
		else {
			return null;
		}
	}

	private static void createValueBindings(
			JdbcParameterBindings jdbcParameterBindings,
			QueryParameterImplementor<?> domainParam,
			QueryParameterBinding<?> domainParamBinding,
			Bindable parameterType,
			JdbcParametersList jdbcParams,
			Object bindValue,
			SharedSessionContractImplementor session) {
		if ( parameterType == null ) {
			throw new SqlTreeCreationException( "Unable to interpret mapping-model type for Query parameter: " + domainParam );
		}
		final int offset =
				jdbcParameterBindings.registerParametersForEachJdbcValue(
						bindValue( parameterType, bindValue, session ),
						parameterType( domainParamBinding, parameterType ),
						jdbcParams,
						session
				);
		assert offset == jdbcParams.size();
	}

	private static Object bindValue(
			Bindable parameterType, Object bindValue, SharedSessionContractImplementor session) {
		if ( parameterType instanceof EntityIdentifierMapping identifierMapping ) {
			return getIdentifier( bindValue, session, identifierMapping );
		}
		else if ( parameterType instanceof EntityMappingType entityMappingType ) {
			return getIdentifier( bindValue, session, entityMappingType.getIdentifierMapping() );
		}
		else if ( parameterType instanceof EntityAssociationMapping association ) {
			// If the association is the target, we must use the identifier of the EntityMappingType
			return getIdentifier( bindValue, session, association );
		}
		else {
			return bindValue;
		}
	}

	private static Object getIdentifier
			(Object bindValue, SharedSessionContractImplementor session, EntityAssociationMapping association) {
		return association.getSideNature() == ForeignKeyDescriptor.Nature.TARGET
				? association.getAssociatedEntityMappingType()
				.getIdentifierMapping().getIdentifier( bindValue )
				: association.getForeignKeyDescriptor()
						.getAssociationKeyFromSide( bindValue, association.getSideNature().inverse(), session );
	}

	private static Object getIdentifier
			(Object bindValue, SharedSessionContractImplementor session, EntityIdentifierMapping identifierMapping) {
		return identifierMapping.findContainingEntityMapping().getRepresentationStrategy()
						.getInstantiator().isInstance( bindValue )
				? identifierMapping.getIdentifierIfNotUnsaved( bindValue, session )
				: bindValue;
	}

	private static Bindable parameterType(
			QueryParameterBinding<?> domainParamBinding, Bindable parameterType) {
		if ( parameterType instanceof PluralAttributeMapping pluralAttributeMapping ) {
			// Default to the collection element
			parameterType = pluralAttributeMapping.getElementDescriptor();
		}

		if ( parameterType instanceof EntityIdentifierMapping ) {
			return parameterType;
		}
		else if ( parameterType instanceof EntityMappingType entityMappingType ) {
			return entityMappingType.getIdentifierMapping();
		}
		else if ( parameterType instanceof EntityAssociationMapping association ) {
			// If the association is the target, we must use the identifier of the EntityMappingType
			return association.getSideNature() == ForeignKeyDescriptor.Nature.TARGET
					? association.getAssociatedEntityMappingType().getIdentifierMapping()
					: association.getForeignKeyDescriptor();
		}
		else if ( parameterType instanceof JavaObjectType ) {
			return domainParamBinding.getType();
		}
		else {
			return parameterType;
		}
	}

	public static Bindable determineParameterType(
			QueryParameterBinding<?> binding,
			QueryParameterImplementor<?> parameter,
			List<? extends SqmParameter<?>> sqmParameters,
			SqmParameterMappingModelResolutionAccess mappingModelResolutionAccess,
			SessionFactoryImplementor sessionFactory) {

		{
			final Bindable tryOne = asBindable( binding.getBindType() );
			if ( tryOne != null ) {
				return tryOne;
			}
		}

		{
			final Bindable tryTwo = asBindable( parameter.getHibernateType() );
			if ( tryTwo != null ) {
				return tryTwo;
			}
		}

		if ( binding.getType() != null ) {
			return binding.getType();
		}

		for ( int i = 0; i < sqmParameters.size(); i++ ) {
			final MappingModelExpressible<?> mappingModelType =
					mappingModelResolutionAccess.getResolvedMappingModelType( sqmParameters.get( i ) );
			if ( mappingModelType != null ) {
				return mappingModelType;
			}
		}

		// assume we have (or can create) a mapping for the parameter's Java type
		return sessionFactory.getTypeConfiguration()
				.standardBasicTypeForJavaType( parameter.getParameterType() );
	}

	/**
	 * Utility to mitigate issues related to type pollution.
	 * Returns the passes object after casting it to Bindable,
	 * if the type is compatible.
	 * If it's not, null will be returned.
	 * @param object any object instance
	 * @return a reference to the same object o, but of type Bindable if possible, or null.
	 */
	private static Bindable asBindable(final Object object) {
		if ( object == null ) {
			return null;
		}
		//There's a high chance that we're dealing with a BasicTypeImpl, or a subclass of it.
		else if ( object instanceof BasicTypeImpl<?> basicType ) {
			return basicType;
		}
		//Alternatively, chances are good that we're dealing with an ConvertedBasicTypeImpl.
		else if ( object instanceof ConvertedBasicTypeImpl<?> convertedBasicType ) {
			return convertedBasicType;
		}
		//Eventually fallback to the standard check for completeness:
		else if ( object instanceof Bindable bindable ) {
			return bindable;
		}
		else {
			return null;
		}
	}

	public static SqmStatement.ParameterResolutions resolveParameters(SqmStatement<?> statement) {
		if ( statement.getQuerySource() == SqmQuerySource.CRITERIA ) {
			final CriteriaParameterCollector parameterCollector = new CriteriaParameterCollector();
			collectParameters( statement, parameterCollector::process );
			return parameterCollector.makeResolution();
		}
		else {
			return new SqmStatement.ParameterResolutions() {
				@Override
				public Set<SqmParameter<?>> getSqmParameters() {
					return statement.getSqmParameters();
				}

				@Override
				public Map<JpaCriteriaParameter<?>, SqmJpaCriteriaParameterWrapper<?>> getJpaCriteriaParamResolutions() {
					return emptyMap();
				}
			};
		}
	}

	public static SqmSortSpecification sortSpecification(SqmSelectStatement<?> sqm, Order<?> order) {
		final List<SqmSelectableNode<?>> items = sqm.getQuerySpec().getSelectClause().getSelectionItems();
		final SqmSelectableNode<?> selected = selectedNode( sqm, order ); // does validation by side effect!
		return createSortSpecification( sqm, order, items, selected );
	}

	private static SqmSortSpecification createSortSpecification(
			SqmSelectStatement<?> sqm, Order<?> order, List<SqmSelectableNode<?>> items, SqmSelectableNode<?> selected) {
		final NodeBuilder builder = sqm.nodeBuilder();
		if ( order.entityClass() == null ) {
			// ordering by an element of the select list
			return new SqmSortSpecification(
					new SqmAliasedNodeRef( order.element(), builder.getIntegerType(), builder ),
					order.direction(), order.nullPrecedence(), !order.caseSensitive()
			);
		}
		else {
			// ordering by an attribute of the returned entity
			if ( items.size() <= 1) {
				if ( selected instanceof SqmFrom<?, ?> root ) {
					if ( !order.entityClass().isAssignableFrom( root.getJavaType() ) ) {
						throw new IllegalQueryOperationException("Select item was of wrong entity type");
					}
					final StringTokenizer tokens = new StringTokenizer( order.attributeName(), "." );
					SqmPath<?> path = root;
					while ( tokens.hasMoreTokens() ) {
						path = path.get( tokens.nextToken() );
					}
					return builder.sort( path, order.direction(), order.nullPrecedence(), !order.caseSensitive() );
				}
				else {
					throw new IllegalQueryOperationException("Select item was not an entity type");
				}
			}
			else {
				throw new IllegalQueryOperationException("Query has multiple items in the select list");
			}
		}
	}

	private static SqmSelectableNode<?> selectedNode(SqmSelectStatement<?> sqm, Order<?> order) {
		final int element = order.element();
		if ( element < 1) {
			throw new IllegalQueryOperationException("Cannot order by element " + element
					+ " (the first select item is element 1)");
		}
		final var selectionItems = sqm.getQuerySpec().getSelectClause().getSelectionItems();
		final int items = selectionItems.size();
		if ( items == 0 && element == 1 ) {
			if ( order.entityClass() == null || sqm.getQuerySpec().getRootList().size() > 1 ) {
				throw new IllegalQueryOperationException("Cannot order by element " + element
						+ " (there is no select list)");
			}
			else {
				return sqm.getQuerySpec().getRootList().get(0);
			}
		}
		else if ( element > items ) {
			throw new IllegalQueryOperationException( "Cannot order by element " + element
					+ " (there are only " + items + " select items)");
		}
		else {
			return selectionItems.get( element - 1 );
		}
	}

	public static boolean isSelectionAssignableToResultType(SqmSelection<?> selection, Class<?> expectedResultType) {
		if ( expectedResultType == null ) {
			return true;
		}
		else if ( selection != null && selection.getSelectableNode() instanceof SqmParameter<?> sqmParameter ) {
			final Class<?> anticipatedClass =
					sqmParameter.getAnticipatedType() != null
							? sqmParameter.getAnticipatedType().getJavaType()
							: null;
			return anticipatedClass != null
				&& expectedResultType.isAssignableFrom( anticipatedClass );
		}
		else if ( selection == null
				|| !isHqlTuple( selection ) && selection.getSelectableNode().isCompoundSelection() ) {
			return false;
		}
		else {
			final JavaType<?> nodeJavaType = selection.getNodeJavaType();
			return nodeJavaType != null
				&& expectedResultType.isAssignableFrom( nodeJavaType.getJavaTypeClass() );
		}
	}

	public static boolean isHqlTuple(SqmSelection<?> selection) {
		return selection != null && selection.getSelectableNode() instanceof SqmTuple;
	}

	public static Class<?> resolveExpressibleJavaTypeClass(final SqmExpression<?> expression) {
		final SqmExpressible<?> expressible = expression.getExpressible();
		return expressible == null || expressible.getExpressibleJavaType() == null
				? null
				: expressible.getExpressibleJavaType().getJavaTypeClass();
	}

	public static <X> SqmPredicate restriction(
			SqmSelectStatement<X> sqmStatement,
			Class<X> resultType,
			Restriction<? super X> restriction) {
		//noinspection unchecked
		final JpaRoot<X> root = (JpaRoot<X>) sqmStatement.getRoot( 0, resultType );
		return  (SqmPredicate) restriction.toPredicate( root, sqmStatement.nodeBuilder() );
	}

	public static void validateCriteriaQuery(SqmQueryPart<?> queryPart) {
		if ( queryPart instanceof SqmQuerySpec<?> sqmQuerySpec ) {
			if ( sqmQuerySpec.getSelectClause().getSelections().isEmpty() ) {
				// make sure there is at least one root
				final List<SqmRoot<?>> sqmRoots = sqmQuerySpec.getFromClause().getRoots();
				if ( sqmRoots == null || sqmRoots.isEmpty() ) {
					throw new IllegalArgumentException( "Criteria did not define any query roots" );
				}
				// if there is a single root, use that as the selection
				if ( sqmRoots.size() == 1 ) {
					sqmQuerySpec.getSelectClause().add( sqmRoots.get( 0 ), null );
				}
				else {
					throw new IllegalArgumentException( "Criteria has multiple query roots" );
				}
			}
		}
		else if ( queryPart instanceof SqmQueryGroup<?> queryGroup ) {
			for ( SqmQueryPart<?> part : queryGroup.getQueryParts() ) {
				validateCriteriaQuery( part );
			}
		}
		else {
			assert false;
		}
	}

	private static class CriteriaParameterCollector {
		private Set<SqmParameter<?>> sqmParameters;
		private Map<JpaCriteriaParameter<?>, List<SqmJpaCriteriaParameterWrapper<?>>> jpaCriteriaParamResolutions;

		public void process(SqmParameter<?> parameter) {
			if ( sqmParameters == null ) {
				sqmParameters = new LinkedHashSet<>();
			}

			if ( parameter instanceof SqmJpaCriteriaParameterWrapper<?> wrapper ) {
				if ( jpaCriteriaParamResolutions == null ) {
					jpaCriteriaParamResolutions = new IdentityHashMap<>();
				}

				jpaCriteriaParamResolutions.computeIfAbsent( wrapper.getJpaCriteriaParameter(), r -> new ArrayList<>() )
						.add( wrapper );
				sqmParameters.add( wrapper );
			}
			else if ( parameter instanceof JpaCriteriaParameter ) {
				throw new UnsupportedOperationException();
//				final JpaCriteriaParameter<?> criteriaParameter = (JpaCriteriaParameter<?>) parameter;
//
//				if ( jpaCriteriaParamResolutions == null ) {
//					jpaCriteriaParamResolutions = new IdentityHashMap<>();
//				}
//
//				final List<SqmJpaCriteriaParameterWrapper<?>> sqmParametersForCriteriaParameter = jpaCriteriaParamResolutions.computeIfAbsent(
//						criteriaParameter,
//						jcp -> new ArrayList<>()
//				);
//
//				final SqmJpaCriteriaParameterWrapper<?> wrapper = new SqmJpaCriteriaParameterWrapper(
//						criteriaParameter.getHibernateType(),
//						criteriaParameter,
//						criteriaParameter.nodeBuilder()
//				);
//
//				sqmParametersForCriteriaParameter.add( wrapper );
//				sqmParameters.add( wrapper );
			}
			else {
				sqmParameters.add( parameter );
			}
		}

		private SqmStatement.ParameterResolutions makeResolution() {
			return new ParameterResolutionsImpl(
					sqmParameters == null ? emptySet() : sqmParameters,
					jpaCriteriaParamResolutions == null ? emptyMap() : jpaCriteriaParamResolutions
			);
		}
	}

	private static class ParameterResolutionsImpl implements SqmStatement.ParameterResolutions {
		private final Set<SqmParameter<?>> sqmParameters;
		private final Map<JpaCriteriaParameter<?>, SqmJpaCriteriaParameterWrapper<?>> jpaCriteriaParamResolutions;

		public ParameterResolutionsImpl(
				Set<SqmParameter<?>> sqmParameters,
				Map<JpaCriteriaParameter<?>, List<SqmJpaCriteriaParameterWrapper<?>>> jpaCriteriaParamResolutions) {
			this.sqmParameters = sqmParameters;

			if ( jpaCriteriaParamResolutions == null || jpaCriteriaParamResolutions.isEmpty() ) {
				this.jpaCriteriaParamResolutions = emptyMap();
			}
			else {
				this.jpaCriteriaParamResolutions =
						new IdentityHashMap<>( determineProperSizing( jpaCriteriaParamResolutions ) );
				for ( Map.Entry<JpaCriteriaParameter<?>, List<SqmJpaCriteriaParameterWrapper<?>>> entry
						: jpaCriteriaParamResolutions.entrySet() ) {
					final Iterator<SqmJpaCriteriaParameterWrapper<?>> itr = entry.getValue().iterator();
					if ( !itr.hasNext() ) {
						throw new IllegalStateException(
								"SqmJpaCriteriaParameterWrapper references for JpaCriteriaParameter ["
										+ entry.getKey() + "] already exhausted" );
					}
					this.jpaCriteriaParamResolutions.put( entry.getKey(), itr.next() );
				}
			}
		}

		@Override
		public Set<SqmParameter<?>> getSqmParameters() {
			return sqmParameters;
		}

		@Override
		public Map<JpaCriteriaParameter<?>, SqmJpaCriteriaParameterWrapper<?>> getJpaCriteriaParamResolutions() {
			return jpaCriteriaParamResolutions;
		}
	}

	/**
	 * Used to validate that the specified query return type is valid (i.e. the user
	 * did not pass {@code Integer.class} when the selection is an entity)
	 */
	public static void validateQueryReturnType(SqmQueryPart<?> queryPart, @Nullable Class<?> expectedResultType) {
		if ( expectedResultType != null && !isResultTypeAlwaysAllowed( expectedResultType ) ) {
			// the result-class is always safe to use (Object, ...)
			checkQueryReturnType( queryPart, expectedResultType );
		}
	}

	/**
	 * Similar to {@link #validateQueryReturnType(SqmQueryPart, Class)} but does not check if {@link #isResultTypeAlwaysAllowed(Class)}.
	 */
	public static void checkQueryReturnType(SqmQueryPart<?> queryPart, Class<?> expectedResultType) {
		if ( queryPart instanceof SqmQuerySpec<?> querySpec ) {
			checkQueryReturnType( querySpec, expectedResultType );
		}
		else if ( queryPart instanceof SqmQueryGroup<?> queryGroup ) {
			for ( SqmQueryPart<?> sqmQueryPart : queryGroup.getQueryParts() ) {
				checkQueryReturnType( sqmQueryPart, expectedResultType );
			}
		}
		else {
			throw new AssertionFailure( "Unexpected query part" );
		}
	}

	private static void checkQueryReturnType(SqmQuerySpec<?> querySpec, Class<?> expectedResultClass) {
		final JpaCompliance jpaCompliance = querySpec.nodeBuilder().getJpaCompliance();
		final List<SqmSelection<?>> selections = querySpec.getSelectClause().getSelections();
		if ( selections == null || selections.isEmpty() ) {
			// make sure there is at least one root
			final List<SqmRoot<?>> sqmRoots = querySpec.getFromClause().getRoots();
			if ( sqmRoots == null || sqmRoots.isEmpty() ) {
				throw new IllegalArgumentException( "Criteria did not define any query roots" );
			}
			// if there is a single root, use that as the selection
			if ( sqmRoots.size() == 1 ) {
				verifySingularSelectionType( expectedResultClass, jpaCompliance, sqmRoots.get( 0 ) );
			}
			else {
				throw new IllegalArgumentException( "Criteria has multiple query roots" );
			}
		}
		else if ( selections.size() == 1 ) {
			final SqmSelection<?> sqmSelection = selections.get( 0 );
			final SqmSelectableNode<?> selectableNode = sqmSelection.getSelectableNode();
			if ( selectableNode.isCompoundSelection() ) {
				final Class<?> expectedSelectItemType = expectedResultClass.isArray()
						? expectedResultClass.getComponentType()
						: expectedResultClass;
				for ( JpaSelection<?> selection : selectableNode.getSelectionItems() ) {
					verifySelectionType( expectedSelectItemType, jpaCompliance, (SqmSelectableNode<?>) selection );
				}
			}
			else {
				verifySingularSelectionType( expectedResultClass, jpaCompliance, sqmSelection.getSelectableNode() );
			}
		}
		else if ( expectedResultClass.isArray() ) {
			final Class<?> componentType = expectedResultClass.getComponentType();
			for ( SqmSelection<?> selection : selections ) {
				verifySelectionType( componentType, jpaCompliance, selection.getSelectableNode() );
			}
		}
		//TODO: else check that the expectedResultClass has an appropriate constructor
	}

	/**
	 * Special case for a single, non-compound selection-item.  It is essentially
	 * a special case of {@linkplain #verifySelectionType} which additionally
	 * handles the case where the type of the selection-item can be used to
	 * instantiate the result-class (result-class has a matching constructor).
	 *
	 * @apiNote We don't want to hoist this into {@linkplain #verifySelectionType}
	 * itself because this can only happen for the root non-compound case, and we
	 * want to avoid the try/catch otherwise
	 */
	private static void verifySingularSelectionType(
			Class<?> expectedResultClass,
			JpaCompliance jpaCompliance,
			SqmSelectableNode<?> selectableNode) {
		try {
			verifySelectionType( expectedResultClass, jpaCompliance, selectableNode );
		}
		catch (QueryTypeMismatchException mismatchException) {
			// Check for special case of a single selection item and implicit instantiation.
			// See if the selected type can be used to instantiate the expected-type
			final JavaType<?> javaTypeDescriptor = selectableNode.getJavaTypeDescriptor();
			if ( javaTypeDescriptor != null ) {
				// ignore the exception if the expected type has a constructor accepting the selected item type
				if ( hasMatchingConstructor( expectedResultClass, javaTypeDescriptor.getJavaTypeClass() ) ) {
					// ignore it
				}
				else {
					throw mismatchException;
				}
			}
		}
	}

	private static <T> boolean hasMatchingConstructor(Class<T> expectedResultClass, Class<?> selectedJavaType) {
		try {
			expectedResultClass.getDeclaredConstructor( selectedJavaType );
			return true;
		}
		catch (NoSuchMethodException e) {
			return false;
		}
	}

	private static <T> void verifySelectionType(
			Class<T> expectedResultClass,
			JpaCompliance jpaCompliance,
			SqmSelectableNode<?> selection) {
		// special case for parameters in the select list
		if ( selection instanceof SqmParameter<?> sqmParameter ) {
			final SqmExpressible<?> nodeType = sqmParameter.getExpressible();
			// we may not yet know a selection type
			if ( nodeType == null || nodeType.getExpressibleJavaType() == null ) {
				// we can't verify the result type up front
				return;
			}
		}

		if ( !jpaCompliance.isJpaQueryComplianceEnabled() ) {
			verifyResultType( expectedResultClass, selection );
		}
	}

	/**
	 * Any query result can be represented as a {@link Tuple}, {@link List}, or {@link Map},
	 * simply by repackaging the result tuple. Also, any query result is assignable to
	 * {@code Object}, or can be returned as an instance of {@code Object[]}.
	 *
	 * @see ConcreteSqmSelectQueryPlan#determineRowTransformer
	 * @see org.hibernate.query.sql.internal.NativeQueryImpl#determineTupleTransformerForResultType
	 */
	public static boolean isResultTypeAlwaysAllowed(Class<?> expectedResultClass) {
		return expectedResultClass == null
			|| expectedResultClass == Object.class
			|| expectedResultClass == Object[].class
			|| expectedResultClass == List.class
			|| expectedResultClass == Map.class
			|| expectedResultClass == Tuple.class;
	}

	protected static void verifyResultType(Class<?> resultClass, SqmSelectableNode<?> selectableNode) {
		final SqmBindableType<?> selectionExpressible = selectableNode.getExpressible();
		final JavaType<?> javaType =
				selectionExpressible == null
						? selectableNode.getNodeJavaType() // for SqmDynamicInstantiation
						: selectionExpressible.getExpressibleJavaType();
		if ( javaType != null ) {
			final Class<?> javaTypeClass = javaType.getJavaTypeClass();
			if ( javaTypeClass != Object.class ) {
				if ( !isValid( resultClass, selectionExpressible, javaTypeClass, javaType ) ) {
					throwQueryTypeMismatchException( resultClass, selectionExpressible, javaTypeClass );
				}
			}
		}
	}

	private static boolean isValid(
			Class<?> resultClass,
			SqmExpressible<?> selectionExpressible,
			Class<?> selectionExpressibleJavaTypeClass,
			JavaType<?> selectionExpressibleJavaType) {
		return resultClass.isAssignableFrom( selectionExpressibleJavaTypeClass )
			|| selectionExpressibleJavaType instanceof final PrimitiveJavaType<?> primitiveJavaType
					&& primitiveJavaType.getPrimitiveClass() == resultClass
			|| isMatchingDateType( selectionExpressibleJavaTypeClass, resultClass, selectionExpressible )
			|| isEntityIdType( selectionExpressible, resultClass );
	}

	private static boolean isEntityIdType(SqmExpressible<?> selectionExpressible, Class<?> resultClass) {
		if ( selectionExpressible instanceof IdentifiableDomainType<?> identifiableDomainType ) {
			return resultClass.isAssignableFrom( identifiableDomainType.getIdType().getJavaType() );
		}
		else if ( selectionExpressible instanceof EntitySqmPathSource<?> entityPath ) {
			return resultClass.isAssignableFrom( entityPath.getPathType().getIdType().getJavaType() );
		}
		else {
			return false;
		}
	}

	// Special case for date because we always report java.util.Date as expression type
	// But the expected resultClass could be a subtype of that, so we need to check the JdbcType
	private static boolean isMatchingDateType(
			Class<?> javaTypeClass,
			Class<?> resultClass,
			SqmExpressible<?> sqmExpressible) {
		return javaTypeClass == Date.class
			&& isMatchingDateJdbcType( resultClass, getJdbcType( sqmExpressible ) );
	}

	private static JdbcType getJdbcType(SqmExpressible<?> sqmExpressible) {
		if ( sqmExpressible instanceof BasicDomainType<?> basicDomainType ) {
			return basicDomainType.getJdbcType();
		}
		else if ( sqmExpressible instanceof SqmPathSource<?> pathSource ) {
			if ( pathSource.getPathType() instanceof BasicDomainType<?> basicDomainType ) {
				return basicDomainType.getJdbcType();
			}
		}
		return null;
	}

	private static boolean isMatchingDateJdbcType(Class<?> resultClass, JdbcType jdbcType) {
		if ( jdbcType != null ) {
			return switch ( jdbcType.getDefaultSqlTypeCode() ) {
				case Types.DATE -> resultClass.isAssignableFrom(java.sql.Date.class);
				case Types.TIME -> resultClass.isAssignableFrom(java.sql.Time.class);
				case Types.TIMESTAMP -> resultClass.isAssignableFrom(java.sql.Timestamp.class);
				default -> false;
			};
		}
		else {
			return false;
		}
	}

	private static void throwQueryTypeMismatchException(
			Class<?> resultClass,
			@Nullable SqmExpressible<?> sqmExpressible, @Nullable Class<?> javaTypeClass) {
		throw new QueryTypeMismatchException( String.format(
				Locale.ROOT,
				"Incorrect query result type: query produces '%s' but type '%s' was given",
				sqmExpressible == null ? javaTypeClass.getName() : sqmExpressible.getTypeName(),
				resultClass.getName()
		) );
	}
}
