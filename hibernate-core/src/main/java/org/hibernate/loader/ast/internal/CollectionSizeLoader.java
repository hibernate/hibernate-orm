package org.hibernate.loader.ast.internal;

import jakarta.annotation.Nonnull;

import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.SelfRenderingAggregateFunctionSqlAstExpression;
import org.hibernate.sql.ast.spi.creation.SqlAliasBaseManager;
import org.hibernate.sql.ast.spi.query.expression.Star;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.internal.RowTransformerStandardImpl;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;

import static java.util.Collections.singletonList;

/**
 * Counts the rows of a collection, subject to the restrictions which loading
 * the collection is subject to, for example, to the rows of the collection as
 * of the changeset the session reads at.
 */
public class CollectionSizeLoader {
	private final PluralAttributeMapping attributeMapping;
	private final SelectStatement sqlAst;
	private final JdbcParametersList jdbcParameters;

	public CollectionSizeLoader(
			@Nonnull PluralAttributeMapping attributeMapping,
			@Nonnull LoadQueryInfluencers influencers,
			@Nonnull SessionFactoryImplementor sessionFactory) {
		this.attributeMapping = attributeMapping;

		final var keyDescriptor = attributeMapping.getKeyDescriptor();
		final var jdbcParametersBuilder = JdbcParametersList.newBuilder( keyDescriptor.getJdbcTypeCount() );
		final var sqlAliasBaseGenerator = new SqlAliasBaseManager();
		// the keys of the rows of the collection, restricted like the rows which load the collection
		final var rows = LoaderSelectBuilder.createSelect(
				attributeMapping,
				List.of( keyDescriptor.getKeyPart() ),
				keyDescriptor,
				null,
				1,
				influencers,
				new LockOptions(),
				jdbcParametersBuilder::add,
				sqlAliasBaseGenerator,
				sessionFactory
		).getQueryPart().getFirstQuerySpec();
		final var tableGroup = rows.getFromClause().getRoots().get( 0 );
		attributeMapping.applyAuxiliaryRestrictions(
				tableGroup,
				rows::applyPredicate,
				influencers,
				sqlAliasBaseGenerator
		);
		jdbcParameters = jdbcParametersBuilder.build();

		final var count = new QuerySpec( true );
		count.getFromClause().addRoot( tableGroup );
		count.applyPredicate( rows.getWhereClauseRestrictions() );
		final var integerType =
				sessionFactory.getTypeConfiguration().getBasicTypeForJavaType( Integer.class );
		final var countFunction =
				(AbstractSqmSelfRenderingFunctionDescriptor)
						sessionFactory.getQueryEngine().getSqmFunctionRegistry()
								.findFunctionDescriptor( "count" );
		count.getSelectClause().addSqlSelection( new SqlSelectionImpl( 0,
				new SelfRenderingAggregateFunctionSqlAstExpression<>(
						countFunction.getName(),
						countFunction,
						singletonList( Star.INSTANCE ),
						null,
						integerType,
						integerType
				) ) );
		sqlAst = new SelectStatement( count, singletonList( new BasicResult<>( 0, null, integerType ) ) );
	}

	public int load(@Nonnull Object key, @Nonnull SharedSessionContractImplementor session) {
		final var jdbcParameterBindings = new JdbcParameterBindingsImpl( jdbcParameters.size() );
		final int offset = jdbcParameterBindings.registerParametersForEachJdbcValue(
				key,
				attributeMapping.getKeyDescriptor(),
				jdbcParameters,
				session
		);
		assert offset == jdbcParameters.size();

		final var sessionFactory = session.getFactory();
		final var jdbcServices = sessionFactory.getJdbcServices();
		final var jdbcSelect =
				jdbcServices.getJdbcEnvironment().getSqlAstTranslatorFactory()
						.buildTranslator( new SqlAstTranslationRequest.Select( sessionFactory, sqlAst ) )
						.translate( jdbcParameterBindings, QueryOptions.NONE );
		final List<Integer> list = jdbcServices.getJdbcSelectExecutor().list(
				jdbcSelect,
				jdbcParameterBindings,
				new BaseExecutionContext( session ),
				RowTransformerStandardImpl.instance(),
				null,
				ListResultsConsumer.UniqueSemantic.NONE,
				1
		);
		return list.isEmpty() || list.get( 0 ) == null ? 0 : list.get( 0 );
	}
}
