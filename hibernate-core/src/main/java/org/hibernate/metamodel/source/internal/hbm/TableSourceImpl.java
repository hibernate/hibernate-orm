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
package org.hibernate.metamodel.source.internal.hbm;

import org.hibernate.metamodel.source.spi.TableSource;

/**
 * @author Steve Ebersole
 */
public class TableSourceImpl
		extends AbstractHbmSourceNode
		implements TableSource {
	private final String schema;
	private final String catalog;
	private final String tableName;
	private final String rowId;

	TableSourceImpl(
			MappingDocument mappingDocument,
			String schema,
			String catalog,
			String tableName) {
		this( mappingDocument, schema, catalog, tableName, null );
	}

	TableSourceImpl(
			MappingDocument mappingDocument,
			String schema,
			String catalog,
			String tableName,
			String rowId) {
		super( mappingDocument );
		this.schema = schema;
		this.catalog = catalog;
		this.tableName = tableName;
		this.rowId = rowId;
	}

	@Override
	public String getExplicitSchemaName() {
		return schema;
	}

	@Override
	public String getExplicitCatalogName() {
		return catalog;
	}

	@Override
	public String getExplicitTableName() {
		return tableName;
	}

	@Override
	public String getRowId() {
		return rowId;
	}
}
