package org.hibernate.persister.filter.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.persister.filter.FilterAliasGenerator;
import org.hibernate.sql.ast.spi.query.from.TableGroup;

/**
 * @author Rob Worsnop
 */
public class TableGroupFilterAliasGenerator implements FilterAliasGenerator {
	private final String defaultTable;
	private final TableGroup tableGroup;

	public TableGroupFilterAliasGenerator(@Nonnull String defaultTable, @Nonnull TableGroup tableGroup) {
		this.defaultTable = defaultTable;
		this.tableGroup = tableGroup;
	}

	@Nullable
	@Override
	public String getAlias(@Nullable String table) {
		final var tableReference =
				tableGroup.getTableReference( null, table == null ? defaultTable : table, true );
		return tableReference == null ? null : tableReference.getIdentificationVariable();
	}

}
