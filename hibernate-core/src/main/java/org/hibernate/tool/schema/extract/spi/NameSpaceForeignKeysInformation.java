package org.hibernate.tool.schema.extract.spi;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.mapping.Table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @since 7.2
 */
public class NameSpaceForeignKeysInformation {
	private final @Nonnull IdentifierHelper identifierHelper;
	private final @Nonnull Map<String, List<ForeignKeyInformation>> foreignKeys = new HashMap<>();

	public NameSpaceForeignKeysInformation(@Nonnull IdentifierHelper identifierHelper) {
		this.identifierHelper = identifierHelper;
	}

	public void addForeignKeyInformation(@Nonnull TableInformation tableInformation, @Nonnull ForeignKeyInformation foreignKeyInformation) {
		foreignKeys.computeIfAbsent( tableInformation.getName().getTableName().getText(), k -> new ArrayList<>() )
				.add( foreignKeyInformation );
	}

	public @Nullable List<ForeignKeyInformation> getForeignKeysInformation(@Nonnull Table table) {
		return foreignKeys.get( identifierHelper.toMetaDataObjectName( table.getQualifiedTableName().getTableName() ) );
	}

	public @Nullable List<ForeignKeyInformation> getForeignKeysInformation(String tableName) {
		return foreignKeys.get( tableName );
	}
}
