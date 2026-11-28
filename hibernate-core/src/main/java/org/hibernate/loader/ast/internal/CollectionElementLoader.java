package org.hibernate.loader.ast.internal;

import java.util.List;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.sql.ast.spi.creation.SqlAliasBaseManager;
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.results.internal.RowTransformerSingularReturnImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;

/// The stored elements of a collection, read without initializing the collection.
public class CollectionElementLoader {
	private final PluralAttributeMapping mapping;
	private final JdbcSelect select;
	private final JdbcParametersList parameters;

	public CollectionElementLoader(PluralAttributeMapping mapping, LoadQueryInfluencers influencers) {
		this.mapping = mapping;
		final var factory = influencers.getSessionFactory();
		final var builder = JdbcParametersList.newBuilder();
		final var statement = LoaderSelectBuilder.createSelect(
				mapping, List.of( mapping.getElementDescriptor() ),
				mapping.getKeyDescriptor(), null, 1, influencers, new LockOptions(),
				builder::add, new SqlAliasBaseManager(), factory
		);
		parameters = builder.build();
		select = CollectionLookupLoader.translate( statement, factory );
	}

	public List<Object> load(Object key, SharedSessionContractImplementor session) {
		final var bindings = new JdbcParameterBindingsImpl( parameters.size() );
		bindings.registerParametersForEachJdbcValue( key, mapping.getKeyDescriptor(), parameters, session );
		return session.getJdbcServices().getJdbcSelectExecutor().list(
				select, bindings, new BaseExecutionContext( session ),
				RowTransformerSingularReturnImpl.instance(), ListResultsConsumer.UniqueSemantic.NONE
		);
	}
}
