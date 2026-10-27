package org.hibernate.tool.schema.extract.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.tool.schema.extract.spi.ColumnInformation;
import org.hibernate.tool.schema.extract.spi.PrimaryKeyInformation;

/**
 * @author Steve Ebersole
 */
public class PrimaryKeyInformationImpl implements PrimaryKeyInformation {
	private final Identifier identifier;
	private final Iterable<ColumnInformation> columns;

	public PrimaryKeyInformationImpl(Identifier identifier, Iterable<ColumnInformation> columns) {
		this.identifier = identifier;
		this.columns = columns;
	}

	@Override
	public @Nullable Identifier getPrimaryKeyIdentifier() {
		return identifier;
	}

	@Override
	public @Nonnull Iterable<ColumnInformation> getColumns() {
		return columns;
	}
}
