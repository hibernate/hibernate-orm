/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.loader.ast.internal;

import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;

import org.hibernate.LockOptions;
import org.hibernate.Filter;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.query.sqm.sql.spi.FromClauseIndex;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.creation.SqlAliasBaseManager;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.query.expression.JdbcParameter;
import org.hibernate.sql.ast.spi.query.expression.QueryLiteral;
import org.hibernate.sql.ast.spi.query.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.spi.query.predicate.FilterPredicate;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.internal.JdbcParameterBindingImpl;
import org.hibernate.sql.exec.internal.JdbcParameterImpl;
import org.hibernate.sql.exec.internal.SqlTypedMappingJdbcParameter;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.internal.ImmutableFetchList;
import org.hibernate.sql.results.internal.RowTransformerArrayImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;
import org.hibernate.type.StandardBasicTypes;

import java.util.ArrayList;
import java.util.List;

import static org.hibernate.internal.util.collections.ArrayHelper.EMPTY_OBJECT_ARRAY;
import static org.hibernate.binder.internal.TenantIdBinder.PARAMETER_NAME;
import static org.hibernate.loader.LoaderLogging.LOADER_LOGGER;
import static org.hibernate.pretty.MessageHelper.infoString;
import static java.util.Collections.singletonMap;

/**
 * @author Steve Ebersole
 */
class DatabaseSnapshotExecutor {

	private final EntityMappingType entityDescriptor;

	private final JdbcSelect jdbcSelect;
	private final JdbcParametersList jdbcParameters;
	private final JdbcParameter tenantIdParameter;

	DatabaseSnapshotExecutor(
			EntityMappingType entityDescriptor,
			SessionFactoryImplementor sessionFactory) {
		this( entityDescriptor, sessionFactory, null );
	}

	DatabaseSnapshotExecutor(
			EntityMappingType entityDescriptor,
			SessionFactoryImplementor sessionFactory,
			Filter tenantFilter) {
		this( entityDescriptor, sessionFactory, tenantFilter, null );
	}

	DatabaseSnapshotExecutor(
			EntityMappingType entityDescriptor,
			SessionFactoryImplementor sessionFactory,
			Filter tenantFilter,
			List<? extends ModelPart> partsToSelect) {
		this.entityDescriptor = entityDescriptor;
		var jdbcParametersBuilder =
				JdbcParametersList.newBuilder( entityDescriptor.getIdentifierMapping().getJdbcTypeCount() );
		final var rootQuerySpec = new QuerySpec( true );

		final var state =
				new LoaderSqlAstCreationState(
						rootQuerySpec,
						new SqlAliasBaseManager(),
						new FromClauseIndex( null ),
						LockOptions.NONE,
						(fetchParent, creationState) -> ImmutableFetchList.EMPTY,
						true,
						new LoadQueryInfluencers( sessionFactory ),
						sessionFactory.getSqlTranslationEngine()
				);

		final var rootPath = new NavigablePath( entityDescriptor.getEntityName() );

		final var rootTableGroup = entityDescriptor.createRootTableGroup(
				true,
				rootPath,
				null,
				null,
				() -> rootQuerySpec::applyPredicate,
				state
		);

		rootQuerySpec.getFromClause().addRoot( rootTableGroup );
		state.getFromClauseAccess().registerTableGroup( rootPath, rootTableGroup );
		tenantIdParameter = tenantFilter == null ? null : new JdbcParameterImpl(
				tenantFilter.getFilterDefinition().getParameterJdbcMapping( PARAMETER_NAME ) );
		if ( tenantFilter != null ) {
			// Snapshots ignore application filters, but must respect tenant isolation.
			entityDescriptor.applyFilterRestrictions(
					predicate -> {
						// Keep the filter's SQL, including formulas and table aliases, but bind
						// the tenant at execution time so the plan can be shared across sessions.
						for ( var fragment : ((FilterPredicate) predicate).getFragments() ) {
							if ( fragment.getParameters() != null ) {
								for ( var parameter : fragment.getParameters() ) {
									parameter.setJdbcParameter( tenantIdParameter );
								}
							}
						}
						rootQuerySpec.applyPredicate( predicate );
					},
					rootTableGroup,
					true,
					singletonMap( tenantFilter.getName(), tenantFilter ),
					true,
					state
			);
		}

		// Full snapshots follow entity state order. Targeted snapshots follow the requested projection.
		final List<DomainResult<?>> domainResults = new ArrayList<>();

		final var sqlExpressionResolver = state.getSqlExpressionResolver();

		// We just need a literal to have a result set
		final var resolved =
				sessionFactory.getTypeConfiguration()
						.getBasicTypeRegistry()
						.resolve( StandardBasicTypes.INTEGER );
		final QueryLiteral<Integer> queryLiteral = new QueryLiteral<>( null, resolved );
		domainResults.add( queryLiteral.createDomainResult( null, state ) );
		final var idNavigablePath =
				rootPath.append( entityDescriptor.getIdentifierMapping().getNavigableRole().getNavigableName() );
		entityDescriptor.getIdentifierMapping().forEachSelectable(
				(columnIndex, selection) -> {
					final var tableReference =
							rootTableGroup.resolveTableReference( idNavigablePath,
									selection.getContainingTableExpression() );
					final var jdbcParameter = new SqlTypedMappingJdbcParameter( selection );
					jdbcParametersBuilder.add( jdbcParameter );
					final var columnReference =
							(ColumnReference)
									sqlExpressionResolver.resolveSqlExpression( tableReference, selection );
					rootQuerySpec.applyPredicate(
							new ComparisonPredicate( columnReference, ComparisonOperator.EQUAL, jdbcParameter )
					);
				}
		);
		jdbcParameters = jdbcParametersBuilder.build();

		if ( partsToSelect == null ) {
			entityDescriptor.forEachAttributeMapping(
					attributeMapping -> {
						final var snapshotDomainResult =
								attributeMapping.createSnapshotDomainResult(
										rootPath.append( attributeMapping.getAttributeName() ),
										rootTableGroup,
										null,
										state
						);
						if ( snapshotDomainResult != null ) {
							domainResults.add( snapshotDomainResult );
						}
					}
			);
		}
		else {
			// A targeted snapshot ignores static restrictions just like a full snapshot.
			for ( var part : partsToSelect ) {
				domainResults.add( part.createDomainResult(
						rootPath.append( part.getPartName() ), rootTableGroup, null, state ) );
			}
		}

		final var selectStatement = new SelectStatement( rootQuerySpec, domainResults );
		jdbcSelect =
				sessionFactory.getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory()
						.buildTranslator( new SqlAstTranslationRequest.Select( sessionFactory, selectStatement ) )
						.translate( null, QueryOptions.NONE );
	}

	/**
	 * @param tenantId the current tenant filter parameter value, or {@code null} for an unrestricted executor
	 */
	Object[] loadDatabaseSnapshot(Object id, Object tenantId, SharedSessionContractImplementor session) {
		if ( LOADER_LOGGER.isTraceEnabled() ) {
			LOADER_LOGGER.trace( "Retrieving snapshot of current persistent state for "
					+ infoString( entityDescriptor, id ) );
		}

		final var jdbcParameterBindings = new JdbcParameterBindingsImpl(
				entityDescriptor.getIdentifierMapping().getJdbcTypeCount()
		);

		final int offset =
				jdbcParameterBindings.registerParametersForEachJdbcValue(
						id,
						entityDescriptor.getIdentifierMapping(),
						jdbcParameters,
						session
				);
		assert offset == jdbcParameters.size();
		if ( tenantIdParameter != null ) {
			final var jdbcMapping = tenantIdParameter.getExpressionType().getSingleJdbcMapping();
			jdbcParameterBindings.addBinding( tenantIdParameter,
					new JdbcParameterBindingImpl( jdbcMapping, jdbcMapping.convertToRelationalValue( tenantId ) ) );
		}

		final List<?> list =
				session.getJdbcServices().getJdbcSelectExecutor().list(
						jdbcSelect,
						jdbcParameterBindings,
						new BaseExecutionContext( session ),
						RowTransformerArrayImpl.instance(),
						null,
						ListResultsConsumer.UniqueSemantic.FILTER,
						1
				);

		final int size = list.size();
		assert size <= 1;

		if ( size == 0 ) {
			return null;
		}
		else {
			final var entitySnapshot = (Object[]) list.get( 0 );
			// The result of this method is treated like the entity state array which doesn't include the id
			// So we must exclude it from the array
			if ( entitySnapshot.length == 1 ) {
				return EMPTY_OBJECT_ARRAY;
			}
			else {
				final Object[] state = new Object[entitySnapshot.length - 1];
				System.arraycopy( entitySnapshot, 1, state, 0, state.length );
				return state;
			}
		}
	}

}
