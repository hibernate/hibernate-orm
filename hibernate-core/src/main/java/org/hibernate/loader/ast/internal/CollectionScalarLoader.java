package org.hibernate.loader.ast.internal;

import jakarta.annotation.Nullable;

import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.EntityCollectionPart;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.sql.ast.spi.query.select.SelectStatement;
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.results.internal.RowTransformerStandardImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;

import static org.hibernate.engine.internal.ForeignKeys.getEntityIdentifierIfNotUnsaved;

/** Executes a single-result query against the visible rows of a collection. */
public final class CollectionScalarLoader {
	public enum Operation { SIZE, INDEX_EXISTS, ELEMENT_EXISTS }

	private final PluralAttributeMapping collection;
	private final @Nullable CollectionPart match;
	private final boolean selectLastIndex;
	private final SelectStatement sqlAst;
	private final JdbcParametersList parameters;

	public CollectionScalarLoader(PluralAttributeMapping collection, Operation operation, LoadQueryInfluencers influencers) {
		this.collection = collection;
		match = switch ( operation ) {
			case SIZE -> null;
			case INDEX_EXISTS -> collection.getIndexDescriptor();
			case ELEMENT_EXISTS -> collection.getElementDescriptor();
		};
		final var classification = collection.getCollectionDescriptor().getCollectionSemantics().getCollectionClassification();
		selectLastIndex = operation == Operation.SIZE && classification.isIndexed() && !classification.isMap();
		final var builder = JdbcParametersList.newBuilder();
		sqlAst = LoaderSelectBuilder.createCollectionScalarSelect( collection, match, selectLastIndex,
				influencers, builder::add, influencers.getSessionFactory() );
		parameters = builder.build();
	}

	public int select(Object key, @Nullable Object value, SharedSessionContractImplementor session) {
		final var result = (Integer) load( collection, key, match, value, sqlAst, parameters, session );
		if ( result == null ) {
			return 0;
		}
		// List size includes gaps preceding the last visible index.
		return selectLastIndex ? result + 1 - Math.max( 0, collection.getIndexMetadata().getListIndexBase() ) : result;
	}

	static @Nullable Object load(
			PluralAttributeMapping collection, Object key, @Nullable CollectionPart match, @Nullable Object value,
			SelectStatement sqlAst, JdbcParametersList jdbcParameters, SharedSessionContractImplementor session) {
		final var jdbcParameterBindings = new JdbcParameterBindingsImpl( jdbcParameters.size() );
		int offset = jdbcParameterBindings.registerParametersForEachJdbcValue(
				key, collection.getKeyDescriptor(), jdbcParameters, session );
		if ( match != null ) {
			final ModelPart bindable;
			if ( match instanceof EntityCollectionPart part ) {
				final var entity = part.getAssociatedEntityMappingType();
				bindable = entity.getIdentifierMapping();
				value = getEntityIdentifierIfNotUnsaved( entity.getEntityName(), value, session );
			}
			else {
				bindable = match;
			}
			offset += jdbcParameterBindings.registerParametersForEachJdbcValue(
					value, offset, bindable, jdbcParameters, session );
		}
		assert offset == jdbcParameters.size();

		final var sessionFactory = session.getFactory();
		final var jdbcServices = sessionFactory.getJdbcServices();
		final var jdbcSelect =
				jdbcServices.getJdbcEnvironment().getSqlAstTranslatorFactory()
						.buildTranslator( new SqlAstTranslationRequest.Select( sessionFactory, sqlAst ) )
						.translate( jdbcParameterBindings, QueryOptions.NONE );
		var list = jdbcServices.getJdbcSelectExecutor().list(
				jdbcSelect,
				jdbcParameterBindings,
				new BaseExecutionContext( session ),
				RowTransformerStandardImpl.instance(),
				null,
				ListResultsConsumer.UniqueSemantic.FILTER,
				1
		);
		return list.isEmpty() ? null : list.get( 0 );
	}
}
