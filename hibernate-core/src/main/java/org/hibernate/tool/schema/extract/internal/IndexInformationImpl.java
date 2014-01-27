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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.metamodel.spi.relational.Identifier;
import org.hibernate.tool.schema.extract.spi.ColumnInformation;
import org.hibernate.tool.schema.extract.spi.IndexInformation;
import org.hibernate.tool.schema.spi.SchemaManagementException;

/**
 * @author Steve Ebersole
 */
public class IndexInformationImpl implements IndexInformation {
	private final Identifier indexIdentifier;
	private final List<ColumnInformation> columnList;

	public IndexInformationImpl(Identifier indexIdentifier, List<ColumnInformation> columnList) {
		this.indexIdentifier = indexIdentifier;
		this.columnList = columnList;
	}

	@Override
	public Identifier getIndexIdentifier() {
		return indexIdentifier;
	}

	@Override
	public List<ColumnInformation> getIndexedColumns() {
		return columnList;
	}

	public static Builder builder(Identifier indexIdentifier) {
		return new Builder( indexIdentifier );
	}

	public static class Builder {
		private final Identifier indexIdentifier;
		private final List<ColumnInformation> columnList = new ArrayList<ColumnInformation>();

		public Builder(Identifier indexIdentifier) {
			this.indexIdentifier = indexIdentifier;
		}

		public Builder addColumn(ColumnInformation columnInformation) {
			columnList.add( columnInformation );
			return this;
		}

		public IndexInformationImpl build() {
			if ( columnList.isEmpty() ) {
				throw new SchemaManagementException(
						"Attempt to resolve JDBC metadata failed to find columns for index [" + indexIdentifier.getText() + "]"
				);
			}
			return new IndexInformationImpl( indexIdentifier, Collections.unmodifiableList( columnList ) );
		}
	}
}
