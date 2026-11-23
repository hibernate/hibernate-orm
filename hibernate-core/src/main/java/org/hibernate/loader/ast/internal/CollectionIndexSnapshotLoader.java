package org.hibernate.loader.ast.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.sql.ast.spi.creation.SqlAliasBaseManager;
import org.hibernate.sql.exec.internal.BaseExecutionContext;
import org.hibernate.sql.exec.internal.JdbcParameterBindingsImpl;
import org.hibernate.sql.exec.spi.JdbcParametersList;
import org.hibernate.sql.exec.spi.JdbcSelect;
import org.hibernate.sql.results.internal.RowTransformerArrayImpl;
import org.hibernate.sql.results.spi.ListResultsConsumer;

/// The stored indexes and managed elements of an audited inverse collection, before replacement.
public class CollectionIndexSnapshotLoader {
	private final PluralAttributeMapping mapping;
	private final JdbcSelect select;
	private final JdbcParametersList parameters;

	public CollectionIndexSnapshotLoader(PluralAttributeMapping mapping, LoadQueryInfluencers influencers) {
		this.mapping = mapping;
		final var factory = influencers.getSessionFactory();
		final var builder = JdbcParametersList.newBuilder();
		final var statement = LoaderSelectBuilder.createSelect(
				mapping, List.of( mapping.getIndexDescriptor(), mapping.getElementDescriptor() ),
				mapping.getKeyDescriptor(), null, 1, influencers, new LockOptions(),
				builder::add, new SqlAliasBaseManager(), factory
		);
		parameters = builder.build();
		select = CollectionLookupLoader.translate( statement, factory );
	}

	public Map<Object, Object> load(Object key, SharedSessionContractImplementor session) {
		final var bindings = new JdbcParameterBindingsImpl( parameters.size() );
		bindings.registerParametersForEachJdbcValue( key, mapping.getKeyDescriptor(), parameters, session );
		final var rows = session.getJdbcServices().getJdbcSelectExecutor().list(
				select, bindings, new BaseExecutionContext( session ),
				RowTransformerArrayImpl.instance(), ListResultsConsumer.UniqueSemantic.NONE
		);
		final int base = mapping.getIndexMetadata().getListIndexBase();
		final Map<Object, Object> elements = new HashMap<>();
		for ( var row : rows ) {
			final Object index = row[0];
			if ( index != null ) {
				elements.put( base > 0 ? (Integer) index - base : index, row[1] );
			}
		}
		return elements;
	}
}
