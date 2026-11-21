package org.hibernate.loader.ast.internal;

import jakarta.annotation.Nullable;

import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
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
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.results.internal.RowTransformerStandardImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;

import static org.hibernate.engine.internal.ForeignKeys.getEntityIdentifierIfNotUnsaved;
import static org.hibernate.sql.exec.spi.JdbcParameterBindings.NO_BINDINGS;

/** Executes a single-result query against the visible rows of a collection. */
public final class CollectionScalarLoader {
	public enum Operation { SIZE, INDEX_EXISTS, ELEMENT_EXISTS }

	private final PluralAttributeMapping collection;
	private final @Nullable CollectionPart match;
	private final boolean selectLastIndex;
	private final JdbcSelect jdbcSelect;
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
		final var factory = influencers.getSessionFactory();
		final var sqlAst = LoaderSelectBuilder.createCollectionScalarSelect( collection, match, selectLastIndex,
				influencers, builder::add, factory );
		jdbcSelect = translate( sqlAst, factory );
		parameters = builder.build();
	}

	static JdbcSelect translate(SelectStatement sqlAst, SessionFactoryImplementor factory) {
		return factory.getJdbcServices().getJdbcEnvironment().getSqlAstTranslatorFactory()
				.buildTranslator( new SqlAstTranslationRequest.Select( factory, sqlAst ) )
				.translate( NO_BINDINGS, QueryOptions.NONE );
	}

	public int select(Object key, @Nullable Object value, SharedSessionContractImplementor session) {
		final ModelPart bindable;
		if ( match instanceof EntityCollectionPart part ) {
			final var entity = part.getAssociatedEntityMappingType();
			bindable = entity.getIdentifierMapping();
			value = getEntityIdentifierIfNotUnsaved( entity.getEntityName(), value, session );
		}
		else {
			bindable = match;
		}
		final var result = (Integer) load( collection, key, bindable, value, jdbcSelect, parameters, session );
		if ( result == null ) {
			return 0;
		}
		// List size includes gaps preceding the last visible index.
		return selectLastIndex ? result + 1 - Math.max( 0, collection.getIndexMetadata().getListIndexBase() ) : result;
	}

	static @Nullable Object load(
			PluralAttributeMapping collection, Object key, @Nullable ModelPart match, @Nullable Object value,
			JdbcSelect jdbcSelect, JdbcParametersList jdbcParameters, SharedSessionContractImplementor session) {
		final var jdbcParameterBindings = new JdbcParameterBindingsImpl( jdbcParameters.size() );
		int offset = jdbcParameterBindings.registerParametersForEachJdbcValue(
				key, collection.getKeyDescriptor(), jdbcParameters, session );
		if ( match != null ) {
			offset += jdbcParameterBindings.registerParametersForEachJdbcValue(
					value, offset, match, jdbcParameters, session );
		}
		assert offset == jdbcParameters.size();

		final var list = session.getJdbcServices().getJdbcSelectExecutor().list(
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
