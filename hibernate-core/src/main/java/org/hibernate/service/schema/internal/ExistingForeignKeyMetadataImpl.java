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
package org.hibernate.service.schema.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.metamodel.spi.relational.Identifier;
import org.hibernate.service.schema.spi.ExistingColumnMetadata;
import org.hibernate.service.schema.spi.ExistingForeignKeyMetadata;
import org.hibernate.service.schema.spi.SchemaManagementException;

/**
 * @author Steve Ebersole
 */
public class ExistingForeignKeyMetadataImpl implements ExistingForeignKeyMetadata {
	private final Identifier fkIdentifier;
	private final List<ColumnReferenceMapping> columnMappingList;

	public ExistingForeignKeyMetadataImpl(
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
	public List<ColumnReferenceMapping> getColumnReferenceMappingList() {
		return columnMappingList;
	}

	public static Builder builder(Identifier fkIdentifier) {
		return new Builder( fkIdentifier );
	}

	public static class Builder {
		private final Identifier fkIdentifier;
		private final List<ColumnReferenceMapping> columnMappingList = new ArrayList<ColumnReferenceMapping>();

		public Builder(Identifier fkIdentifier) {
			this.fkIdentifier = fkIdentifier;
		}
		
		public Builder addColumnMapping(ExistingColumnMetadata referencing, ExistingColumnMetadata referenced) {
			columnMappingList.add( new ColumnReferenceMappingImpl( referencing, referenced ) );
			return this;
		}

		public ExistingForeignKeyMetadataImpl build() {
			if ( columnMappingList.isEmpty() ) {
				throw new SchemaManagementException(
						"Attempt to resolve foreign key metadata from JDBC metadata failed to find " +
								"column mappings for foreign key named [" + fkIdentifier.getText() + "]"
				);
			}
			return new ExistingForeignKeyMetadataImpl( fkIdentifier, columnMappingList );
		}
	}
	
	public static class ColumnReferenceMappingImpl implements ColumnReferenceMapping {
		private final ExistingColumnMetadata referencing;
		private final ExistingColumnMetadata referenced;

		public ColumnReferenceMappingImpl(ExistingColumnMetadata referencing, ExistingColumnMetadata referenced) {
			this.referencing = referencing;
			this.referenced = referenced;
		}

		@Override
		public ExistingColumnMetadata getReferencingColumnMetadata() {
			return referencing;
		}

		@Override
		public ExistingColumnMetadata getReferencedColumnMetadata() {
			return referenced;
		}
	}
}
