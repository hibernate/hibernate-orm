package org.hibernate.tool.schema.extract.spi;

import jakarta.annotation.Nonnull;
import org.hibernate.mapping.NamedTable;

import jakarta.annotation.Nullable;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.mapping.Table;

import java.util.HashMap;
import java.util.Map;

/**
 * @since 7.2
 */
public class NameSpacePrimaryKeysInformation {
	private final @Nonnull IdentifierHelper identifierHelper;
	private final @Nonnull Map<String, PrimaryKeyInformation> primaryKeys = new HashMap<>();

	public NameSpacePrimaryKeysInformation(@Nonnull IdentifierHelper identifierHelper) {
		this.identifierHelper = identifierHelper;
	}

	public void addPrimaryKeyInformation(@Nonnull TableInformation tableInformation, @Nonnull PrimaryKeyInformation primaryKeyInformation) {
		primaryKeys.put( tableInformation.getName().getTableName().getText(), primaryKeyInformation );
	}

	public @Nullable PrimaryKeyInformation getPrimaryKeyInformation(Table table) {
		return primaryKeys.get( identifierHelper.toMetaDataObjectName( org.hibernate.boot.model.naming.internal.PhysicalNamingStrategyHelper.physicalIdentifier( ((NamedTable) table).getPhysicalName().objectName() ) ) );
	}

	public @Nullable PrimaryKeyInformation getPrimaryKeyInformation(String tableName) {
		return primaryKeys.get( tableName );
	}

	public void validate() {
		for ( var entry : primaryKeys.entrySet() ) {
			final var tableName = entry.getKey();
			final var primaryKeyInformation = entry.getValue();
			int i = 1;
			for ( var column : primaryKeyInformation.getColumns() ) {
				if ( column == null ) {
					throw new SchemaExtractionException(
							"Primary Key information was missing for key [" +
							primaryKeyInformation.getPrimaryKeyIdentifier() + "] on table [" + tableName +
							"] at KEY_SEQ = " + i
					);
				}
				i++;
			}
		}
	}
}
