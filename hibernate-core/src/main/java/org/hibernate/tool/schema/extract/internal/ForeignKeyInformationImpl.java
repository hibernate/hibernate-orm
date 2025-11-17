/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.extract.internal;

import java.util.List;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.tool.schema.extract.spi.ColumnInformation;
import org.hibernate.tool.schema.extract.spi.ForeignKeyInformation;

/**
 * @author Steve Ebersole
 */
public class ForeignKeyInformationImpl implements ForeignKeyInformation {
	private final Identifier foreignKeyIdentifier;
	private final List<ColumnReferenceMapping> columnMappingList;

	public ForeignKeyInformationImpl(
			Identifier foreignKeyIdentifier,
			List<ColumnReferenceMapping> columnMappingList) {
		this.foreignKeyIdentifier = foreignKeyIdentifier;
		this.columnMappingList = columnMappingList;
	}

	@Override
	public Identifier getForeignKeyIdentifier() {
		return foreignKeyIdentifier;
	}

	@Override
	public Iterable<ColumnReferenceMapping> getColumnReferenceMappings() {
		return columnMappingList;
	}

	public record ColumnReferenceMappingImpl(ColumnInformation referencing, ColumnInformation referenced)
			implements ColumnReferenceMapping {
		@Override
		public ColumnInformation getReferencingColumnMetadata() {
			return referencing;
		}

		@Override
		public ColumnInformation getReferencedColumnMetadata() {
			return referenced;
		}
	}
}
