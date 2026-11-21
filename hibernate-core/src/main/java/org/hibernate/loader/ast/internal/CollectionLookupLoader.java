package org.hibernate.loader.ast.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.TransientObjectException;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.function.AbstractSqmSelfRenderingFunctionDescriptor;
import org.hibernate.query.sqm.function.SelfRenderingAggregateFunctionSqlAstExpression;
import org.hibernate.sql.ast.spi.SqlAstNode;
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
 * Looks up the size of a collection, or whether it has a given element or index,
 * without initializing it, subject to the restrictions which loading the collection
 * is subject to, for example, to the rows of the collection as of the changeset the
 * session reads at.
 */
public class CollectionLookupLoader {
	private final PluralAttributeMapping attributeMapping;
	private final @Nullable CollectionPartRestriction restriction;
	private final SelectStatement sqlAst;
	private final JdbcParametersList jdbcParameters;

	/**
	 * The size of the collection: the number of its rows, or, when it is indexed by
	 * integers, one more than its highest index.
	 */
	public static int size(
			@Nonnull PluralAttributeMapping attributeMapping,
			boolean integerIndexed,
			int baseIndex,
			@Nonnull Object key,
			@Nonnull SharedSessionContractImplementor session) {
		final var loader = new CollectionLookupLoader( attributeMapping, null, integerIndexed, session );
		final Integer result = loader.load( key, null, session );
		return result == null ? 0 : integerIndexed ? result + 1 - baseIndex : result;
	}

	/**
	 * Whether the collection has a row with the given value of its element or index.
	 */
	public static boolean exists(
			@Nonnull PluralAttributeMapping attributeMapping,
			@Nonnull CollectionPart part,
			@Nonnull Object key,
			@Nullable Object value,
			@Nonnull SharedSessionContractImplementor session) {
		try {
			final var loader = new CollectionLookupLoader( attributeMapping, part, false, session );
			final Integer count = loader.load( key, value, session );
			return count != null && count > 0;
		}
		catch (TransientObjectException e) {
			return false;
		}
	}

	private CollectionLookupLoader(
			PluralAttributeMapping attributeMapping,
			@Nullable CollectionPart restrictedPart,
			boolean highestIndex,
			SharedSessionContractImplementor session) {
		this.attributeMapping = attributeMapping;
		this.restriction =
				restrictedPart == null ? null : CollectionPartRestriction.of( attributeMapping, restrictedPart );
		final var sessionFactory = session.getFactory();
		final var influencers = session.getLoadQueryInfluencers();

		final var keyDescriptor = attributeMapping.getKeyDescriptor();
		final List<ModelPart> restrictedParts = new ArrayList<>( 2 );
		restrictedParts.add( keyDescriptor );
		if ( restriction != null ) {
			restriction.addRestrictedPart( restrictedParts );
		}
		final var jdbcParametersBuilder = JdbcParametersList.newBuilder();
		final var sqlAliasBaseGenerator = new SqlAliasBaseManager();
		// the rows of the collection, restricted like the rows which load the collection
		final var rows = LoaderSelectBuilder.createSelect(
				attributeMapping,
				List.of( highestIndex ? attributeMapping.getIndexDescriptor() : keyDescriptor.getKeyPart() ),
				restrictedParts,
				null,
				1,
				influencers,
				new LockOptions(),
				jdbcParametersBuilder::add,
				sqlAliasBaseGenerator,
				sessionFactory
		).getQueryPart().getFirstQuerySpec();
		if ( restriction != null ) {
			restriction.restrictJoined( rows, jdbcParametersBuilder::add );
		}
		final var tableGroup = rows.getFromClause().getRoots().get( 0 );
		attributeMapping.applyAuxiliaryRestrictions(
				tableGroup,
				rows::applyPredicate,
				influencers,
				sqlAliasBaseGenerator
		);
		jdbcParameters = jdbcParametersBuilder.build();

		final var lookup = new QuerySpec( true );
		lookup.getFromClause().addRoot( tableGroup );
		lookup.applyPredicate( rows.getWhereClauseRestrictions() );
		final var integerType =
				sessionFactory.getTypeConfiguration().getBasicTypeForJavaType( Integer.class );
		final String functionName = highestIndex ? "max" : "count";
		final var function =
				(AbstractSqmSelfRenderingFunctionDescriptor)
						sessionFactory.getQueryEngine().getSqmFunctionRegistry()
								.findFunctionDescriptor( functionName );
		final List<SqlAstNode> arguments = singletonList( highestIndex
				? rows.getSelectClause().getSqlSelections().get( 0 ).getExpression()
				: Star.INSTANCE );
		lookup.getSelectClause().addSqlSelection( new SqlSelectionImpl( 0,
				new SelfRenderingAggregateFunctionSqlAstExpression<>(
						function.getName(),
						function,
						arguments,
						null,
						integerType,
						integerType
				) ) );
		sqlAst = new SelectStatement( lookup, singletonList( new BasicResult<>( 0, null, integerType ) ) );
	}

	private @Nullable Integer load(
			@Nonnull Object key,
			@Nullable Object value,
			@Nonnull SharedSessionContractImplementor session) {
		final var jdbcParameterBindings = new JdbcParameterBindingsImpl( jdbcParameters.size() );
		int offset = jdbcParameterBindings.registerParametersForEachJdbcValue(
				key,
				attributeMapping.getKeyDescriptor(),
				jdbcParameters,
				session
		);
		if ( restriction != null ) {
			offset += restriction.bind( jdbcParameterBindings, value, offset, jdbcParameters, session );
		}
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
		return list.isEmpty() ? null : list.get( 0 );
	}
}
