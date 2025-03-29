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
	private final Identifier fkIdentifier;
	private final List<ColumnReferenceMapping> columnMappingList;

	public ForeignKeyInformationImpl(
			Identifier fkIdentifier,
			List<ColumnReferenceMapping> columnMappingList) {
		this.fkIdentifier = fkIdentifier;
		this.columnMappingList = columnMappingList;
	}

	@Override
	public Identifier getForeignKeyIdentifier() {
		return fkIdentifier;
	}

	@Override
	public Iterable<ColumnReferenceMapping> getColumnReferenceMappings() {
		return columnMappingList;
	}

	public static class ColumnReferenceMappingImpl implements ColumnReferenceMapping {
		private final ColumnInformation referencing;
		private final ColumnInformation referenced;

		public ColumnReferenceMappingImpl(ColumnInformation referencing, ColumnInformation referenced) {
			this.referencing = referencing;
			this.referenced = referenced;
		}

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
