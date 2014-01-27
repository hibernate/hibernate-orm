/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.tool.schema.extract.internal;

import java.util.List;

import org.hibernate.metamodel.spi.relational.Identifier;
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
