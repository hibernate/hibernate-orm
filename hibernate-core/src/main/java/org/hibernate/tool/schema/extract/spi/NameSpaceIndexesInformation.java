package org.hibernate.tool.schema.extract.spi;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.mapping.Table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NameSpaceIndexesInformation {
	private final @Nonnull IdentifierHelper identifierHelper;
	private final @Nonnull Map<String, List<IndexInformation>> indexes = new HashMap<>();

	public NameSpaceIndexesInformation(@Nonnull IdentifierHelper identifierHelper) {
		this.identifierHelper = identifierHelper;
	}

	public void addIndexInformation(@Nonnull TableInformation tableInformation, @Nonnull IndexInformation indexInformation) {
		indexes.computeIfAbsent( tableInformation.getName().getTableName().getText(), k -> new ArrayList<>() )
				.add( indexInformation );
	}

	public @Nullable List<IndexInformation> getIndexesInformation(@Nonnull Table table) {
		return indexes.get( identifierHelper.toMetaDataObjectName( table.getQualifiedTableName().getTableName() ) );
	}

	public @Nullable List<IndexInformation> getIndexesInformation(String tableName) {
		return indexes.get( tableName );
	}
}
