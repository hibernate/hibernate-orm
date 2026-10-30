package org.hibernate.tool.schema.extract.spi;

import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.mapping.Table;

/**
 * @author Andrea Boriero
 */
public class NameSpaceTablesInformation {
	private final @Nonnull IdentifierHelper identifierHelper;
	private final @Nonnull Map<String, TableInformation> tables = new HashMap<>();

	public NameSpaceTablesInformation(@Nonnull IdentifierHelper identifierHelper) {
		this.identifierHelper = identifierHelper;
	}

	public void addTableInformation(@Nonnull TableInformation tableInformation) {
		tables.put( tableInformation.getName().getTableName().getText(), tableInformation );
	}

	public @Nullable TableInformation getTableInformation(@Nonnull Table table) {
		return tables.get( identifierHelper.toMetaDataObjectName( table.getQualifiedTableName().getTableName() ) );
	}

	public @Nullable TableInformation getTableInformation(String tableName) {
		return tables.get( tableName );
	}
}
