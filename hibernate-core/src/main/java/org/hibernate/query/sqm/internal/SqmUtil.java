/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.function.Function;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.MappingMetamodel;
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
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.IllegalSelectQueryException;
import org.hibernate.query.Order;
import org.hibernate.query.criteria.JpaOrder;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.SqmQuerySource;
import org.hibernate.query.sqm.spi.JdbcParameterBySqmParameterAccess;
import org.hibernate.query.sqm.spi.SqmParameterMappingModelResolutionAccess;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.SqmDmlStatement;
import org.hibernate.query.sqm.tree.SqmJoinType;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.query.sqm.tree.expression.JpaCriteriaParameter;
import org.hibernate.query.sqm.tree.expression.SqmAliasedNodeRef;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.tree.expression.SqmJpaCriteriaParameterWrapper;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.expression.SqmTuple;
import org.hibernate.query.sqm.tree.from.SqmAttributeJoin;
import org.hibernate.query.sqm.tree.from.SqmFrom;
import org.hibernate.query.sqm.tree.from.SqmJoin;
import org.hibernate.query.sqm.tree.from.SqmQualifiedJoin;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.select.SqmOrderByClause;
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
import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.type.internal.ConvertedBasicTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;

import static java.util.stream.Collectors.toList;
import static org.hibernate.internal.util.NullnessUtil.castNonNull;
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

	/**
	 * Utility that returns the entity association target's mapping type if the specified {@code sqmPath} should
	 * be dereferenced using the target table, i.e. when the path's lhs is an explicit join that is used in the
	 * group by clause, or defaults to the provided {@code modelPartContainer} otherwise.
	 */
	public static ModelPartContainer getTargetMappingIfNeeded(
			SqmPath<?> sqmPath,
			ModelPartContainer modelPartContainer,
			SqmToSqlAstConverter sqlAstCreationState) {
		final SqmQueryPart<?> queryPart = sqlAstCreationState.getCurrentSqmQueryPart();
		if ( queryPart != null ) {
			// We only need to do this for queries
			final Clause clause = sqlAstCreationState.getCurrentClauseStack().getCurrent();
			if ( clause != Clause.FROM && modelPartContainer.getPartMappingType() != modelPartContainer && sqmPath.getLhs() instanceof SqmFrom<?, ?> ) {
				final ModelPart modelPart;
				if ( modelPartContainer instanceof PluralAttributeMapping ) {
					modelPart = getCollectionPart(
							(PluralAttributeMapping) modelPartContainer,
							castNonNull( sqmPath.getNavigablePath().getParent() )
					);
				}
				else {
					modelPart = modelPartContainer;
				}
				if ( modelPart instanceof EntityAssociationMapping ) {
					final EntityAssociationMapping association = (EntityAssociationMapping) modelPart;
					// If the path is one of the association's target key properties,
					// we need to render the target side if in group/order by
					if ( association.getTargetKeyPropertyNames().contains( sqmPath.getReferencedPathSource().getPathName() )
							&& ( clause == Clause.GROUP || clause == Clause.ORDER
							|| !isFkOptimizationAllowed( sqmPath.getLhs() )
							|| queryPart.getFirstQuerySpec().groupByClauseContains( sqmPath.getNavigablePath(), sqlAstCreationState )
							|| queryPart.getFirstQuerySpec().orderByClauseContains( sqmPath.getNavigablePath(), sqlAstCreationState ) ) ) {
						return association.getAssociatedEntityMappingType();
					}
				}
			}
		}
		return modelPartContainer;
	}

	private static CollectionPart getCollectionPart(PluralAttributeMapping attribute, NavigablePath path) {
		final CollectionPart.Nature nature = CollectionPart.Nature.fromNameExact( path.getLocalName() );
		if ( nature != null ) {
			switch ( nature ) {
				case ELEMENT:
					return attribute.getElementDescriptor();
				case INDEX:
					return attribute.getIndexDescriptor();
			}
		}
		return null;
	}

	/**
	 * Utility that returns {@code false} when the provided {@link SqmPath sqmPath} is
	 * a join that cannot be dereferenced through the foreign key on the associated table,
	 * i.e. a join that's neither {@linkplain SqmJoinType#INNER} nor {@linkplain SqmJoinType#LEFT}
	 * or one that has an explicit on clause predicate.
	 */
	public static boolean isFkOptimizationAllowed(SqmPath<?> sqmPath) {
		if ( sqmPath instanceof SqmJoin<?, ?> ) {
			final SqmJoin<?, ?> sqmJoin = (SqmJoin<?, ?>) sqmPath;
			switch ( sqmJoin.getSqmJoinType() ) {
				case INNER:
				case LEFT:
					return !( sqmJoin instanceof SqmQualifiedJoin<?, ?>)
							|| ( (SqmQualifiedJoin<?, ?>) sqmJoin ).getJoinPredicate() == null;
				default:
					return false;
			}
		}
		return false;
	}

	public static List<NavigablePath> getGroupByNavigablePaths(SqmQuerySpec<?> querySpec) {
		final List<SqmExpression<?>> expressions = querySpec.getGroupByClauseExpressions();
		if ( expressions.isEmpty() ) {
			return Collections.emptyList();
		}

		return collectNavigablePaths( expressions );
	}

	public static List<NavigablePath> getOrderByNavigablePaths(SqmQuerySpec<?> querySpec) {
		final SqmOrderByClause order = querySpec.getOrderByClause();
		if ( order == null || order.getSortSpecifications().isEmpty() ) {
			return Collections.emptyList();
		}

		final List<SqmExpression<?>> expressions = order.getSortSpecifications()
				.stream()
				.map( SqmSortSpecification::getSortExpression )
				.collect( toList() );
		return collectNavigablePaths( expressions );
	}

	private static List<NavigablePath> collectNavigablePaths(final List<SqmExpression<?>> expressions) {
		final List<NavigablePath> navigablePaths = new ArrayList<>( expressions.size() );
		final SqmPathVisitor pathVisitor = new SqmPathVisitor( path -> navigablePaths.add( path.getNavigablePath() ) );
		for ( final SqmExpression<?> expression : expressions ) {
			if ( expression instanceof SqmAliasedNodeRef ) {
				final NavigablePath navigablePath = ( (SqmAliasedNodeRef) expression ).getNavigablePath();
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
			if ( join.getReferencedPathSource() == pathSource ) {
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
			return Collections.emptyMap();
		}

		final int queryParameterCount = domainParameterXref.getQueryParameterCount();
		final Map<QueryParameterImplementor<?>, Map<SqmParameter<?>, List<JdbcParametersList>>> result = new IdentityHashMap<>( queryParameterCount );

		for ( Map.Entry<QueryParameterImplementor<?>, List<SqmParameter<?>>> entry : domainParameterXref.getQueryParameters().entrySet() ) {
			final QueryParameterImplementor<?> queryParam = entry.getKey();
			final List<SqmParameter<?>> sqmParams = entry.getValue();

			final Map<SqmParameter<?>, List<JdbcParametersList>> sqmParamMap = result.computeIfAbsent(
					queryParam,
					qp -> new IdentityHashMap<>( sqmParams.size() )
			);

			for ( SqmParameter<?> sqmParam : sqmParams ) {
				List<List<JdbcParameter>> lists = jdbcParameterBySqmParameterAccess.getJdbcParamsBySqmParam().get(
						sqmParam );
				sqmParamMap.put( sqmParam, convert( lists ) );

				final List<SqmParameter<?>> expansions = domainParameterXref.getExpansions( sqmParam );
				if ( ! expansions.isEmpty() ) {
					for ( SqmParameter<?> expansion : expansions ) {
						List<List<JdbcParameter>> innerList = jdbcParameterBySqmParameterAccess.getJdbcParamsBySqmParam()
								.get( expansion );
						sqmParamMap.put( expansion, convert( innerList) );
						result.put( queryParam, sqmParamMap );
					}
				}
			}
		}

		return result;
	}

	private static List<JdbcParametersList> convert(final List<List<JdbcParameter>> lists) {
		if ( lists == null ) {
			return null;
		}
		List<JdbcParametersList> output = new ArrayList<>( lists.size() );
		for ( List<JdbcParameter> element : lists ) {
			output.add( JdbcParametersList.fromList( element ) );
		}
		return output;
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
			MappingMetamodel domainModel,
			Function<NavigablePath, TableGroup> tableGroupLocator,
			SqmParameterMappingModelResolutionAccess mappingModelResolutionAccess,
			SharedSessionContractImplementor session) {
		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl(
				domainParameterXref.getSqmParameterCount()
		);

		for ( Map.Entry<QueryParameterImplementor<?>, List<SqmParameter<?>>> entry :
				domainParameterXref.getQueryParameters().entrySet() ) {
			final QueryParameterImplementor<?> queryParam = entry.getKey();
			final List<SqmParameter<?>> sqmParameters = entry.getValue();

			final QueryParameterBinding<?> domainParamBinding = domainParamBindings.getBinding( queryParam );

			final Map<SqmParameter<?>, List<JdbcParametersList>> jdbcParamMap = jdbcParamXref.get( queryParam );
			for ( SqmParameter<?> sqmParameter : sqmParameters ) {
				final MappingModelExpressible resolvedMappingModelType = mappingModelResolutionAccess
						.getResolvedMappingModelType( sqmParameter );
				if ( resolvedMappingModelType != null ) {
					domainParamBinding.setType( resolvedMappingModelType );
				}
				final Bindable parameterType = determineParameterType(
						domainParamBinding,
						queryParam,
						sqmParameters,
						mappingModelResolutionAccess,
						session.getFactory()
				);

				final List<JdbcParametersList> jdbcParamsBinds = jdbcParamMap.get( sqmParameter );
				if ( jdbcParamsBinds == null ) {
					// This can happen when a group or order by item expression, that contains parameters,
					// is replaced with an alias reference expression, which can happen for JPA Criteria queries
					continue;
				}
				if ( !domainParamBinding.isBound() ) {
					for ( int i = 0; i < jdbcParamsBinds.size(); i++ ) {
						final JdbcParametersList jdbcParams = jdbcParamsBinds.get( i );
						parameterType.forEachJdbcType(
								(position, jdbcMapping) -> {
									jdbcParameterBindings.addBinding(
											jdbcParams.get( position ),
											new JdbcParameterBindingImpl( jdbcMapping, null )
									);
								}
						);
					}
				}
				else if ( domainParamBinding.isMultiValued() ) {
					final Collection<?> bindValues = domainParamBinding.getBindValues();
					final Iterator<?> valueItr = bindValues.iterator();

					// the original SqmParameter is the one we are processing.. create a binding for it..
					final Object firstValue = valueItr.next();
					for ( int i = 0; i < jdbcParamsBinds.size(); i++ ) {
						final JdbcParametersList jdbcParams = jdbcParamsBinds.get( i );
						createValueBindings(
								jdbcParameterBindings,
								queryParam,
								domainParamBinding,
								parameterType,
								jdbcParams,
								firstValue,
								tableGroupLocator,
								session
						);
					}

					// an then one for each of the expansions
					final List<SqmParameter<?>> expansions = domainParameterXref.getExpansions( sqmParameter );
					final int expansionCount = bindValues.size() - 1;
					final int parameterUseCount = jdbcParamsBinds.size();
					assert expansions.size() == expansionCount * parameterUseCount;
					int expansionPosition = 0;
					while ( valueItr.hasNext() ) {
						final Object expandedValue = valueItr.next();
						for ( int j = 0; j < parameterUseCount; j++ ) {
							final SqmParameter<?> expansionSqmParam = expansions.get( expansionPosition + j * expansionCount );
							final List<JdbcParametersList> jdbcParamBinds = jdbcParamMap.get( expansionSqmParam );
							for ( int i = 0; i < jdbcParamBinds.size(); i++ ) {
								JdbcParametersList expansionJdbcParams = jdbcParamBinds.get( i );
								createValueBindings(
										jdbcParameterBindings,
										queryParam,
										domainParamBinding,
										parameterType,
										expansionJdbcParams,
										expandedValue,
										tableGroupLocator,
										session
								);
							}
						}
						expansionPosition++;
					}
				}
				else {
					final JdbcMapping jdbcMapping;
					if ( domainParamBinding.getType() instanceof JdbcMapping ) {
						jdbcMapping = (JdbcMapping) domainParamBinding.getType();
					}
					else if ( domainParamBinding.getBindType() instanceof BasicValuedMapping ) {
						jdbcMapping = ( (BasicValuedMapping) domainParamBinding.getType() ).getJdbcMapping();
					}
					else {
						jdbcMapping = null;
					}

					final BasicValueConverter valueConverter = jdbcMapping == null ? null : jdbcMapping.getValueConverter();
					if ( valueConverter != null ) {
						final Object convertedValue = valueConverter.toRelationalValue( domainParamBinding.getBindValue() );
						for ( int i = 0; i < jdbcParamsBinds.size(); i++ ) {
							final JdbcParametersList jdbcParams = jdbcParamsBinds.get( i );
							assert jdbcParams.size() == 1;
							final JdbcParameter jdbcParameter = jdbcParams.get( 0 );
							jdbcParameterBindings.addBinding(
									jdbcParameter,
									new JdbcParameterBindingImpl( jdbcMapping, convertedValue )
							);
						}
					}
					else {
						final Object bindValue = domainParamBinding.getBindValue();
						if ( bindValue == null ) {
							for ( int i = 0; i < jdbcParamsBinds.size(); i++ ) {
								final JdbcParametersList jdbcParams = jdbcParamsBinds.get( i );
								for ( int j = 0; j < jdbcParams.size(); j++ ) {
									final JdbcParameter jdbcParameter = jdbcParams.get( j );
									jdbcParameterBindings.addBinding(
											jdbcParameter,
											new JdbcParameterBindingImpl( jdbcMapping, bindValue )
									);
								}
							}
						}
						else {

							for ( int i = 0; i < jdbcParamsBinds.size(); i++ ) {
								final JdbcParametersList jdbcParams = jdbcParamsBinds.get( i );
								createValueBindings(
										jdbcParameterBindings,
										queryParam,
										domainParamBinding,
										parameterType,
										jdbcParams,
										bindValue,
										tableGroupLocator,
										session
								);
							}
						}
					}
				}
			}
		}

		return jdbcParameterBindings;
	}

	private static void createValueBindings(
			JdbcParameterBindings jdbcParameterBindings,
			QueryParameterImplementor<?> domainParam,
			QueryParameterBinding<?> domainParamBinding,
			Bindable parameterType,
			JdbcParametersList jdbcParams,
			Object bindValue,
			Function<NavigablePath, TableGroup> tableGroupLocator,
			SharedSessionContractImplementor session) {
		if ( parameterType == null ) {
			throw new SqlTreeCreationException( "Unable to interpret mapping-model type for Query parameter : " + domainParam );
		}
		else if ( parameterType instanceof PluralAttributeMapping ) {
			// Default to the collection element
			parameterType = ( (PluralAttributeMapping) parameterType ).getElementDescriptor();
		}

		if ( parameterType instanceof EntityIdentifierMapping ) {
			final EntityIdentifierMapping identifierMapping = (EntityIdentifierMapping) parameterType;
			final EntityMappingType entityMapping = identifierMapping.findContainingEntityMapping();
			if ( entityMapping.getRepresentationStrategy().getInstantiator().isInstance( bindValue, session.getFactory() ) ) {
				bindValue = identifierMapping.getIdentifierIfNotUnsaved( bindValue, session );
			}
		}
		else if ( parameterType instanceof EntityMappingType ) {
			final EntityIdentifierMapping identifierMapping = ( (EntityMappingType) parameterType ).getIdentifierMapping();
			final EntityMappingType entityMapping = identifierMapping.findContainingEntityMapping();
			parameterType = identifierMapping;
			if ( entityMapping.getRepresentationStrategy().getInstantiator().isInstance( bindValue, session.getFactory() ) ) {
				bindValue = identifierMapping.getIdentifierIfNotUnsaved( bindValue, session );
			}
		}
		else if ( parameterType instanceof EntityAssociationMapping ) {
			EntityAssociationMapping association = (EntityAssociationMapping) parameterType;
			if ( association.getSideNature() == ForeignKeyDescriptor.Nature.TARGET ) {
				// If the association is the target, we must use the identifier of the EntityMappingType
				bindValue = association.getAssociatedEntityMappingType().getIdentifierMapping()
						.getIdentifier( bindValue );
				parameterType = association.getAssociatedEntityMappingType().getIdentifierMapping();
			}
			else {
				bindValue = association.getForeignKeyDescriptor().getAssociationKeyFromSide(
						bindValue,
						association.getSideNature().inverse(),
						session
				);
				parameterType = association.getForeignKeyDescriptor();
			}
		}
		else if ( parameterType instanceof JavaObjectType ) {
			parameterType = domainParamBinding.getType();
		}

		int offset = jdbcParameterBindings.registerParametersForEachJdbcValue(
				bindValue,
				parameterType,
				jdbcParams,
				session
		);
		assert offset == jdbcParams.size();
	}

	public static Bindable determineParameterType(
			QueryParameterBinding<?> binding,
			QueryParameterImplementor<?> parameter,
			List<SqmParameter<?>> sqmParameters,
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
			final MappingModelExpressible<?> mappingModelType = mappingModelResolutionAccess
					.getResolvedMappingModelType( sqmParameters.get( i ) );
			if ( mappingModelType != null ) {
				return mappingModelType;
			}
		}

		final TypeConfiguration typeConfiguration = sessionFactory.getTypeConfiguration();

		// assume we have (or can create) a mapping for the parameter's Java type
		return typeConfiguration.standardBasicTypeForJavaType( parameter.getParameterType() );
	}

	/**
	 * Utility to mitigate issues related to type pollution.
	 * Returns the passes object after casting it to Bindable,
	 * if the type is compatible.
	 * If it's not, null will be returned.
	 * @param o any object instance
	 * @return a reference to the same object o, but of type Bindable if possible, or null.
	 */
	private static Bindable asBindable(final Object o) {
		if ( o == null ) {
			return null;
		}
		//There's a high chance that we're dealing with a BasicTypeImpl, or a subclass of it.
		else if ( o instanceof BasicTypeImpl ) {
			return (BasicTypeImpl) o;
		}
		//Alternatively, chances are good that we're dealing with an ConvertedBasicTypeImpl.
		else if ( o instanceof ConvertedBasicTypeImpl ) {
			return (ConvertedBasicTypeImpl) o;
		}
		else {
			//Eventually fallback to the standard check for completeness:
			if ( o instanceof Bindable ) {
				return (Bindable) o;
			}
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
					return Collections.emptyMap();
				}
			};
		}
	}

	static JpaOrder sortSpecification(SqmSelectStatement<?> sqm, Order<?> order) {
		final List<SqmSelectableNode<?>> items = sqm.getQuerySpec().getSelectClause().getSelectionItems();
		int element = order.getElement();
		if ( element < 1) {
			throw new IllegalQueryOperationException("Cannot order by element " + element
					+ " (the first select item is element 1)");
		}
		if ( element > items.size() ) {
			throw new IllegalQueryOperationException("Cannot order by element " + element
					+ " (there are only " + items.size() + " select items)");
		}
		final SqmSelectableNode<?> selected = items.get( element-1 );

		final NodeBuilder builder = sqm.nodeBuilder();
		if ( order.getEntityClass() == null ) {
			// ordering by an element of the select list
			return new SqmSortSpecification(
					new SqmAliasedNodeRef( element, builder.getIntegerType(), builder ),
					order.getDirection(),
					order.getNullPrecedence(),
					order.isCaseInsensitive()
			);
		}
		else {
			// ordering by an attribute of the returned entity
			if ( items.size() == 1) {
				if ( selected instanceof SqmRoot) {
					final SqmFrom<?,?> root = (SqmFrom<?,?>) selected;
					if ( !order.getEntityClass().isAssignableFrom( root.getJavaType() ) ) {
						throw new IllegalQueryOperationException("Select item was of wrong entity type");
					}
					final StringTokenizer tokens = new StringTokenizer( order.getAttributeName(), "." );
					SqmPath<?> path = root;
					while ( tokens.hasMoreTokens() ) {
						path = path.get( tokens.nextToken() );
					}
					return builder.sort( path, order.getDirection(), order.getNullPrecedence(), order.isCaseInsensitive() );
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

	public static boolean isSelectionAssignableToResultType(SqmSelection<?> selection, Class<?> expectedResultType) {
		if ( expectedResultType == null
				|| selection != null && selection.getSelectableNode() instanceof SqmParameter ) {
			return true;
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

	private static class CriteriaParameterCollector {
		private Set<SqmParameter<?>> sqmParameters;
		private Map<JpaCriteriaParameter<?>, List<SqmJpaCriteriaParameterWrapper<?>>> jpaCriteriaParamResolutions;

		public void process(SqmParameter<?> parameter) {
			if ( sqmParameters == null ) {
				sqmParameters = new LinkedHashSet<>();
			}

			if ( parameter instanceof SqmJpaCriteriaParameterWrapper<?> ) {
				if ( jpaCriteriaParamResolutions == null ) {
					jpaCriteriaParamResolutions = new IdentityHashMap<>();
				}

				final SqmJpaCriteriaParameterWrapper<?> wrapper = (SqmJpaCriteriaParameterWrapper<?>) parameter;
				final JpaCriteriaParameter<?> criteriaParameter = wrapper.getJpaCriteriaParameter();

				final List<SqmJpaCriteriaParameterWrapper<?>> sqmParametersForCriteriaParameter = jpaCriteriaParamResolutions.computeIfAbsent(
						criteriaParameter,
						jcp -> new ArrayList<>()
				);

				sqmParametersForCriteriaParameter.add( wrapper );
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
					sqmParameters == null ? Collections.emptySet() : sqmParameters,
					jpaCriteriaParamResolutions == null ? Collections.emptyMap() : jpaCriteriaParamResolutions
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
				this.jpaCriteriaParamResolutions = Collections.emptyMap();
			}
			else {
				this.jpaCriteriaParamResolutions = new IdentityHashMap<>( CollectionHelper.determineProperSizing( jpaCriteriaParamResolutions ) );
				for ( Map.Entry<JpaCriteriaParameter<?>, List<SqmJpaCriteriaParameterWrapper<?>>> entry : jpaCriteriaParamResolutions.entrySet() ) {
					final Iterator<SqmJpaCriteriaParameterWrapper<?>> itr = entry.getValue().iterator();
					if ( !itr.hasNext() ) {
						throw new IllegalStateException(
								"SqmJpaCriteriaParameterWrapper references for JpaCriteriaParameter [" + entry.getKey() + "] already exhausted" );
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
}
