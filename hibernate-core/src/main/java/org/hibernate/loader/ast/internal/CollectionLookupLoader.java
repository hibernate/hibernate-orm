package org.hibernate.loader.ast.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.TransientObjectException;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
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
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.results.graph.basic.BasicResult;
import org.hibernate.sql.results.internal.RowTransformerStandardImpl;
import org.hibernate.sql.results.internal.SqlSelectionImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;

import static java.util.Collections.singletonList;
import static org.hibernate.sql.exec.spi.JdbcParameterBindings.NO_BINDINGS;

/**
 * Looks up the size of a collection, or whether it has a given element or index,
 * without initializing it, subject to the restrictions which loading the collection
 * is subject to, for example, to the rows of the collection as of the changeset the
 * session reads at.
 * <p>
 * The statement is translated once, so a loader whose restrictions do not depend on
 * the session may be reused.
 */
public class CollectionLookupLoader {

	/**
	 * A lookup which does not initialize the collection.
	 */
	public enum Operation {
		/**
		 * The number of rows of the collection, or, when it is indexed by integers,
		 * one more than its highest index.
		 */
		SIZE,
		/**
		 * Whether the collection has a row with a given index.
		 */
		INDEX_EXISTS,
		/**
		 * Whether the collection has a row with a given element.
		 */
		ELEMENT_EXISTS
	}

	private final PluralAttributeMapping attributeMapping;
	private final @Nullable CollectionPartRestriction restriction;
	private final boolean highestIndex;
	private final JdbcSelect jdbcSelect;
	private final JdbcParametersList jdbcParameters;

	/**
	 * @param influencers the settings of the session, which the restrictions of the
	 * collection may depend on
	 */
	public CollectionLookupLoader(
			@Nonnull PluralAttributeMapping attributeMapping,
			@Nonnull Operation operation,
			@Nonnull LoadQueryInfluencers influencers) {
		this.attributeMapping = attributeMapping;
		final var restrictedPart = switch ( operation ) {
			case SIZE -> null;
			case INDEX_EXISTS -> attributeMapping.getIndexDescriptor();
			case ELEMENT_EXISTS -> attributeMapping.getElementDescriptor();
		};
		restriction =
				restrictedPart == null ? null : CollectionPartRestriction.of( attributeMapping, restrictedPart );
		final var classification =
				attributeMapping.getCollectionDescriptor().getCollectionSemantics().getCollectionClassification();
		highestIndex = operation == Operation.SIZE && classification.isIndexed() && !classification.isMap();
		final var sessionFactory = influencers.getSessionFactory();

		final var keyDescriptor = attributeMapping.getKeyDescriptor();
		final List<ModelPart> restrictedParts = new ArrayList<>( 2 );
		restrictedParts.add( keyDescriptor );
		if ( restriction != null ) {
			restriction.addRestrictedPart( restrictedParts );
		}
		final var jdbcParametersBuilder = JdbcParametersList.newBuilder();
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
				new SqlAliasBaseManager(),
				sessionFactory
		).getQueryPart().getFirstQuerySpec();
		if ( restriction != null ) {
			restriction.restrictJoined( rows, jdbcParametersBuilder::add );
		}
		final var tableGroup = rows.getFromClause().getRoots().get( 0 );
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
		jdbcSelect = translate(
				new SelectStatement( lookup, singletonList( new BasicResult<>( 0, null, integerType ) ) ),
				sessionFactory
		);
	}

	/**
	 * Translates a lookup once, with its parameters bound each time it is executed.
	 */
	static JdbcSelect translate(SelectStatement sqlAst, SessionFactoryImplementor sessionFactory) {
		return sessionFactory.getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory()
				.buildTranslator( new SqlAstTranslationRequest.Select( sessionFactory, sqlAst ) )
				.translate( NO_BINDINGS, QueryOptions.NONE );
	}

	/**
	 * The size of the collection: the number of its rows, or, when it is indexed by
	 * integers, one more than its highest index.
	 */
	public int size(@Nonnull Object key, @Nonnull SharedSessionContractImplementor session) {
		final Integer result = load( key, null, session );
		if ( result == null ) {
			return 0;
		}
		else if ( highestIndex ) {
			return result + 1 - Math.max( 0, attributeMapping.getIndexMetadata().getListIndexBase() );
		}
		else {
			return result;
		}
	}

	/**
	 * Whether the collection has a row with the given value of its element or index.
	 */
	public boolean exists(
			@Nonnull Object key,
			@Nullable Object value,
			@Nonnull SharedSessionContractImplementor session) {
		try {
			final Integer count = load( key, value, session );
			return count != null && count > 0;
		}
		catch (TransientObjectException e) {
			return false;
		}
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

		final List<Integer> list = session.getJdbcServices().getJdbcSelectExecutor().list(
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
