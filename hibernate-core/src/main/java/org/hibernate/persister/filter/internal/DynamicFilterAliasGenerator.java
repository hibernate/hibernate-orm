package org.hibernate.persister.filter.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.persister.filter.FilterAliasGenerator;

import static org.hibernate.persister.entity.AbstractEntityPersister.generateTableAlias;
import static org.hibernate.persister.entity.AbstractEntityPersister.getTableId;

/**
 * @author Rob Worsnop
 */
public class DynamicFilterAliasGenerator implements FilterAliasGenerator {
	private final String[] tables;
	private final String rootAlias;

	public DynamicFilterAliasGenerator(@Nonnull String[] tables, @Nonnull String rootAlias) {
		this.tables = tables;
		this.rootAlias = rootAlias;
	}

	@Nullable
	@Override
	public String getAlias(@Nullable String table) {
		return table == null
				? rootAlias
				: generateTableAlias( rootAlias,
						getTableId( table, tables ) );
	}
}
