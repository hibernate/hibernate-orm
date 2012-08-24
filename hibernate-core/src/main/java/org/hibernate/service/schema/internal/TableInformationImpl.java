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

import java.util.Map;

import org.hibernate.metamodel.spi.relational.Identifier;
import org.hibernate.metamodel.spi.relational.ObjectName;
import org.hibernate.service.schema.spi.ColumnInformation;
import org.hibernate.service.schema.spi.ForeignKeyInformation;
import org.hibernate.service.schema.spi.IndexInformation;
import org.hibernate.service.schema.spi.TableInformation;

/**
 * Provides access to information about existing schema objects (tables, sequences etc) of existing database.
 *
 * @author Christoph Sturm
 * @author Max Rydahl Andersen
 * @author Steve Ebersole
 */
public class TableInformationImpl implements TableInformation {
	private final DatabaseInformationImpl database;
	private final ObjectName tableName;
	private final Map<Identifier, ColumnInformation> columns;

	private Map<Identifier, ForeignKeyInformationImpl> foreignKeys;
	private Map<Identifier, IndexInformationImpl> indexes;

	public TableInformationImpl(DatabaseInformationImpl database, ObjectName tableName) {
		this.database = database;
		this.tableName = tableName;
		this.columns = database.getColumnMetadata( this );
	}

	@Override
	public ObjectName getName() {
		return tableName;
	}

	@Override
	public ColumnInformation getColumnInformation(Identifier columnIdentifier) {
		return columns.get( columnIdentifier );
	}

	protected Map<Identifier, ForeignKeyInformationImpl> foreignKeys() {
		if ( foreignKeys == null ) {
			foreignKeys = database.getForeignKeyMetadata( this );
		}
		return foreignKeys;
	}

	@Override
	public ForeignKeyInformation getForeignKeyInformation(Identifier fkIdentifier) {
		return foreignKeys().get( fkIdentifier );
	}

	@Override
	@SuppressWarnings("unchecked")
	public Iterable getForeignKeyInformations() {
		return foreignKeys().values();
	}

	@Override
	public String toString() {
		return "TableInformationImpl(" + tableName.toString() + ')';
	}

	protected Map<Identifier, IndexInformationImpl> indexes() {
		if ( indexes == null ) {
			indexes = database.getIndexInformation( this );
		}
		return indexes;
	}

	@Override
	public IndexInformation getIndexInformation(Identifier indexName) {
		return indexes().get( indexName );
	}
}
