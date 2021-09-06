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
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.Bindable;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.ConvertibleModelPart;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.query.IllegalQueryOperationException;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.spi.QueryParameterBinding;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.spi.QueryParameterImplementor;
import org.hibernate.query.sqm.SqmQuerySource;
import org.hibernate.query.sqm.spi.JdbcParameterBySqmParameterAccess;
import org.hibernate.query.sqm.spi.SqmParameterMappingModelResolutionAccess;
import org.hibernate.query.sqm.tree.SqmDmlStatement;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.expression.JpaCriteriaParameter;
import org.hibernate.query.sqm.tree.expression.SqmJpaCriteriaParameterWrapper;
import org.hibernate.query.sqm.tree.expression.SqmParameter;
import org.hibernate.query.sqm.tree.jpa.ParameterCollector;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlTreeCreationException;
import org.hibernate.sql.ast.tree.expression.JdbcParameter;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.converter.AttributeConverterTypeAdapter;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Helper utilities for dealing with SQM
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("WeakerAccess")
public class SqmUtil {
	private SqmUtil() {
	}

	public static void verifyIsSelectStatement(SqmStatement sqm) {
		if ( !(sqm instanceof SqmSelectStatement) ) {
			throw new IllegalQueryOperationException(
					String.format(
							Locale.ROOT,
							"Expecting a SELECT Query [%s], but found %s",
							SqmSelectStatement.class.getName(),
							sqm.getClass().getName()
					)
			);
		}
	}

	public static void verifyIsNonSelectStatement(SqmStatement sqm) {
		if ( !(sqm instanceof SqmDmlStatement) ) {
			throw new IllegalQueryOperationException(
					String.format(
							Locale.ROOT,
							"Expecting a non-SELECT Query [%s], but found %s",
							SqmDmlStatement.class.getName(),
							sqm.getClass().getName()
					)
			);
		}
	}

	public static Map<QueryParameterImplementor<?>, Map<SqmParameter, List<List<JdbcParameter>>>> generateJdbcParamsXref(
			DomainParameterXref domainParameterXref,
			JdbcParameterBySqmParameterAccess jdbcParameterBySqmParameterAccess) {
		if ( domainParameterXref == null || !domainParameterXref.hasParameters() ) {
			return Collections.emptyMap();
		}

		final int queryParameterCount = domainParameterXref.getQueryParameterCount();
		final Map<QueryParameterImplementor<?>, Map<SqmParameter, List<List<JdbcParameter>>>> result = new IdentityHashMap<>( queryParameterCount );

		for ( Map.Entry<QueryParameterImplementor<?>, List<SqmParameter>> entry : domainParameterXref.getSqmParamByQueryParam().entrySet() ) {
			final QueryParameterImplementor<?> queryParam = entry.getKey();
			final List<SqmParameter> sqmParams = entry.getValue();

			final Map<SqmParameter, List<List<JdbcParameter>>> sqmParamMap = result.computeIfAbsent(
					queryParam,
					qp -> new IdentityHashMap<>( sqmParams.size() )
			);

			for ( SqmParameter sqmParam : sqmParams ) {
				sqmParamMap.put( sqmParam, jdbcParameterBySqmParameterAccess.getJdbcParamsBySqmParam().get( sqmParam ) );

				final List<SqmParameter> expansions = domainParameterXref.getExpansions( sqmParam );
				if ( ! expansions.isEmpty() ) {
					for ( SqmParameter expansion : expansions ) {
						sqmParamMap.put( expansion, jdbcParameterBySqmParameterAccess.getJdbcParamsBySqmParam().get( expansion ) );
						result.put( queryParam, sqmParamMap );
					}
				}
			}
		}

		return result;
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
			Map<QueryParameterImplementor<?>, Map<SqmParameter, List<List<JdbcParameter>>>> jdbcParamXref,
			MappingMetamodel domainModel,
			Function<NavigablePath, TableGroup> tableGroupLocator,
			SqmParameterMappingModelResolutionAccess mappingModelResolutionAccess,
			SharedSessionContractImplementor session) {
		final JdbcParameterBindings jdbcParameterBindings = new JdbcParameterBindingsImpl(
				domainParameterXref.getSqmParameterCount()
		);

		for ( Map.Entry<QueryParameterImplementor<?>, List<SqmParameter>> entry :
				domainParameterXref.getSqmParamByQueryParam().entrySet() ) {
			final QueryParameterImplementor<?> queryParam = entry.getKey();
			final List<SqmParameter> sqmParameters = entry.getValue();

			final QueryParameterBinding<?> domainParamBinding = domainParamBindings.getBinding( queryParam );

			final Map<SqmParameter, List<List<JdbcParameter>>> jdbcParamMap = jdbcParamXref.get( queryParam );
			sqm_params: for ( SqmParameter sqmParameter : sqmParameters ) {
				final Bindable parameterType = determineParameterType(
						domainParamBinding,
						queryParam,
						sqmParameters,
						mappingModelResolutionAccess,
						session.getFactory()
				);

				final List<List<JdbcParameter>> jdbcParamsBinds = jdbcParamMap.get( sqmParameter );
				if ( jdbcParamsBinds == null ) {
					// This can happen when a group or order by item expression, that contains parameters,
					// is replaced with an alias reference expression, which can happen for JPA Criteria queries
					continue;
				}
				if ( !domainParamBinding.isBound() ) {
					final MappingModelExpressable mappingExpressable = SqmMappingModelHelper.resolveMappingModelExpressable(
							sqmParameter,
							domainModel,
							tableGroupLocator
					);
					jdbc_params: for ( int i = 0; i < jdbcParamsBinds.size(); i++ ) {
						final List<JdbcParameter> jdbcParams = jdbcParamsBinds.get( i );
						mappingExpressable.forEachJdbcType(
								(position, jdbcType) -> {
									jdbcParameterBindings.addBinding(
											jdbcParams.get( position ),
											new JdbcParameterBindingImpl( jdbcType, null )
									);
								}
						);
					}
				}
				else if ( domainParamBinding.isMultiValued() ) {
					final Collection<?> bindValues = domainParamBinding.getBindValues();
					final Iterator<?> valueItr = bindValues.iterator();

					// the original SqmParameter is the one we are processing.. create a binding for it..
					for ( int i = 0; i < jdbcParamsBinds.size(); i++ ) {
						final List<JdbcParameter> jdbcParams = jdbcParamsBinds.get( i );
						createValueBindings(
								jdbcParameterBindings,
								queryParam,
								domainParamBinding,
								parameterType,
								jdbcParams,
								valueItr.next(),
								tableGroupLocator,
								session
						);
					}

					// an then one for each of the expansions
					final List<SqmParameter> expansions = domainParameterXref.getExpansions( sqmParameter );
					assert expansions.size() == bindValues.size() - 1;
					int expansionPosition = 0;
					while ( valueItr.hasNext() ) {
						final SqmParameter expansionSqmParam = expansions.get( expansionPosition++ );
						final List<List<JdbcParameter>> jdbcParamBinds = jdbcParamMap.get( expansionSqmParam );
						for ( int i = 0; i < jdbcParamBinds.size(); i++ ) {
							List<JdbcParameter> expansionJdbcParams = jdbcParamBinds.get( i );
							createValueBindings(
									jdbcParameterBindings,
									queryParam, domainParamBinding,
									parameterType,
									expansionJdbcParams,
									valueItr.next(),
									tableGroupLocator,
									session
							);
						}
					}
				}
				else if ( domainParamBinding.getBindValue() == null ) {
					for ( int i = 0; i < jdbcParamsBinds.size(); i++ ) {
						final List<JdbcParameter> jdbcParams = jdbcParamsBinds.get( i );
						for ( int j = 0; j < jdbcParams.size(); j++ ) {
							final JdbcParameter jdbcParameter = jdbcParams.get( j );
							jdbcParameterBindings.addBinding(
									jdbcParameter,
									new JdbcParameterBindingImpl( null, null )
							);
						}
					}
				}
				else {
					if ( domainParamBinding.getType() instanceof AttributeConverterTypeAdapter
							|| domainParamBinding.getType() instanceof ConvertibleModelPart ) {
						final BasicValueConverter valueConverter;
						final JdbcMapping jdbcMapping;

						if ( domainParamBinding.getType() instanceof AttributeConverterTypeAdapter ) {
							final AttributeConverterTypeAdapter adapter = (AttributeConverterTypeAdapter) domainParamBinding.getType();
							valueConverter = adapter.getAttributeConverter();
							jdbcMapping = adapter.getJdbcMapping();
						}
						else {
							final ConvertibleModelPart convertibleModelPart = (ConvertibleModelPart) domainParamBinding.getType();
							valueConverter = convertibleModelPart.getValueConverter();
							jdbcMapping = convertibleModelPart.getJdbcMapping();
						}

						if ( valueConverter != null ) {
							final Object convertedValue = valueConverter.toRelationalValue( domainParamBinding.getBindValue() );

							for ( int i = 0; i < jdbcParamsBinds.size(); i++ ) {
								final List<JdbcParameter> jdbcParams = jdbcParamsBinds.get( i );
								assert jdbcParams.size() == 1;
								final JdbcParameter jdbcParameter = jdbcParams.get( 0 );
								jdbcParameterBindings.addBinding(
										jdbcParameter,
										new JdbcParameterBindingImpl( jdbcMapping, convertedValue )
								);
							}

							continue sqm_params;
						}
					}

					final Object bindValue = domainParamBinding.getBindValue();
					for ( int i = 0; i < jdbcParamsBinds.size(); i++ ) {
						final List<JdbcParameter> jdbcParams = jdbcParamsBinds.get( i );
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

		return jdbcParameterBindings;
	}

	private static void createValueBindings(
			JdbcParameterBindings jdbcParameterBindings,
			QueryParameterImplementor<?> domainParam,
			QueryParameterBinding<?> domainParamBinding,
			Bindable parameterType,
			List<JdbcParameter> jdbcParams,
			Object bindValue,
			Function<NavigablePath, TableGroup> tableGroupLocator,
			SharedSessionContractImplementor session) {
		if ( parameterType == null ) {
			throw new SqlTreeCreationException( "Unable to interpret mapping-model type for Query parameter : " + domainParam );
		}
		if ( parameterType instanceof CollectionPart && ( (CollectionPart) parameterType ).getPartMappingType() instanceof Bindable ) {
			parameterType = (Bindable) ( (CollectionPart) parameterType ).getPartMappingType();
		}

		if ( parameterType instanceof EntityIdentifierMapping ) {
			final EntityIdentifierMapping identifierMapping = (EntityIdentifierMapping) parameterType;
			final EntityMappingType entityMapping = identifierMapping.findContainingEntityMapping();
			if ( entityMapping.getRepresentationStrategy().getInstantiator().isInstance( bindValue, session.getFactory() ) ) {
				bindValue = identifierMapping.getIdentifier( bindValue, session );
			}
		}
		else if ( parameterType instanceof EntityMappingType ) {
			final EntityIdentifierMapping identifierMapping = ( (EntityMappingType) parameterType ).getIdentifierMapping();
			final EntityMappingType entityMapping = identifierMapping.findContainingEntityMapping();
			parameterType = identifierMapping;
			if ( entityMapping.getRepresentationStrategy().getInstantiator().isInstance( bindValue, session.getFactory() ) ) {
				bindValue = identifierMapping.getIdentifier( bindValue, session );
			}
		}
		else if ( parameterType instanceof ToOneAttributeMapping ) {
			ToOneAttributeMapping association = (ToOneAttributeMapping) parameterType;
			bindValue = association.getForeignKeyDescriptor().getAssociationKeyFromSide(
					bindValue,
					association.getSideNature().inverse(),
					session
			);
			parameterType = association.getForeignKeyDescriptor();
		}
		else if ( parameterType instanceof PluralAttributeMapping ) {
			// we'd expect the values to refer to the collection element
			// for now, let's blow up and see where this happens and fix the specifics...
			throw new NotYetImplementedFor6Exception( "Binding parameters whose inferred type comes from plural attribute not yet implemented" );
		}

		int offset = jdbcParameterBindings.registerParametersForEachJdbcValue(
				bindValue,
				Clause.IRRELEVANT,
				parameterType,
				jdbcParams,
				session
		);
		assert offset == jdbcParams.size();
	}

	public static Bindable determineParameterType(
			QueryParameterBinding<?> binding,
			QueryParameterImplementor<?> parameter,
			List<SqmParameter> sqmParameters,
			SqmParameterMappingModelResolutionAccess mappingModelResolutionAccess,
			SessionFactoryImplementor sessionFactory) {
		if ( binding.getType() != null ) {
			return binding.getType();
		}

		if ( binding.getBindType() != null && binding.getBindType() instanceof Bindable ) {
			return (Bindable) binding.getBindType();
		}

		if ( parameter.getHibernateType() != null && parameter.getHibernateType() instanceof Bindable ) {
			return (Bindable) parameter.getHibernateType();
		}

		for ( int i = 0; i < sqmParameters.size(); i++ ) {
			final MappingModelExpressable<?> mappingModelType = mappingModelResolutionAccess
					.getResolvedMappingModelType( sqmParameters.get( i ) );
			if ( mappingModelType != null ) {
				return mappingModelType;
			}
		}

		final TypeConfiguration typeConfiguration = sessionFactory.getTypeConfiguration();

		// assume we have (or can create) a mapping for the parameter's Java type
		BasicType basicType = typeConfiguration.standardBasicTypeForJavaType( parameter.getParameterType() );
		return basicType;
	}

	public static SqmStatement.ParameterResolutions resolveParameters(SqmStatement<?> statement) {
		if ( statement.getQuerySource() == SqmQuerySource.CRITERIA ) {
			final CriteriaParameterCollector parameterCollector = new CriteriaParameterCollector();

			ParameterCollector.collectParameters(
					statement,
					parameterCollector::process,
					statement.nodeBuilder().getServiceRegistry()
			);

			return parameterCollector.makeResolution();
		}
		else {
			return new SqmStatement.ParameterResolutions() {
				@Override
				public Set<SqmParameter<?>> getSqmParameters() {
					return statement.getSqmParameters();
				}

				@Override
				public Map<JpaCriteriaParameter<?>, Supplier<SqmJpaCriteriaParameterWrapper<?>>> getJpaCriteriaParamResolutions() {
					return Collections.emptyMap();
				}
			};
		}
	}

	private static class CriteriaParameterCollector {
		private Set<SqmParameter<?>> sqmParameters;
		private Map<JpaCriteriaParameter<?>, List<SqmJpaCriteriaParameterWrapper<?>>> jpaCriteriaParamResolutions;

		public void process(SqmParameter<?> parameter) {
			if ( sqmParameters == null ) {
				sqmParameters = new HashSet<>();
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
		private final Map<JpaCriteriaParameter<?>, Supplier<SqmJpaCriteriaParameterWrapper<?>>> jpaCriteriaParamResolutions;

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
					this.jpaCriteriaParamResolutions.put(
							entry.getKey(),
							() -> {
								if ( itr.hasNext() ) {
									return itr.next();
								}
								throw new IllegalStateException( "SqmJpaCriteriaParameterWrapper references for JpaCriteriaParameter [" + entry.getKey() + "] already exhausted" );
							}
					);

				}
			}
		}

		@Override
		public Set<SqmParameter<?>> getSqmParameters() {
			return sqmParameters;
		}

		@Override
		public Map<JpaCriteriaParameter<?>, Supplier<SqmJpaCriteriaParameterWrapper<?>>> getJpaCriteriaParamResolutions() {
			return jpaCriteriaParamResolutions;
		}
	}
}
