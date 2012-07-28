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

import org.hibernate.metamodel.spi.relational.ForeignKey;
import org.hibernate.metamodel.spi.relational.Identifier;
import org.hibernate.metamodel.spi.relational.ObjectName;
import org.hibernate.service.schema.spi.ExistingColumnMetadata;
import org.hibernate.service.schema.spi.ExistingForeignKeyMetadata;
import org.hibernate.service.schema.spi.ExistingTableMetadata;
import org.hibernate.tool.hbm2ddl.IndexMetadata;

/**
 * Provides access to information about existing schema objects (tables, sequences etc) of existing database.
 *
 * @author Christoph Sturm
 * @author Max Rydahl Andersen
 * @author Steve Ebersole
 */
public class ExistingTableMetadataImpl implements ExistingTableMetadata {
	private final ExistingDatabaseMetaDataImpl database;
	private final ObjectName tableName;
	private final Map<Identifier, ExistingColumnMetadata> columns;

	private Map<Identifier, ExistingForeignKeyMetadataImpl> foreignKeys;

	public ExistingTableMetadataImpl(ExistingDatabaseMetaDataImpl database, ObjectName tableName) {
		this.database = database;
		this.tableName = tableName;
		this.columns = database.getColumnMetadata( this );
	}

	@Override
	public ObjectName getName() {
		return tableName;
	}

	@Override
	public ExistingColumnMetadata getColumnMetadata(Identifier columnIdentifier) {
		return columns.get( columnIdentifier );
	}

	@Override
	public ExistingForeignKeyMetadata getForeignKeyMetadata(Identifier fkIdentifier) {
		if ( foreignKeys == null ) {
			foreignKeys = database.getForeignKeyMetadata( this );
		}
		return foreignKeys.get( fkIdentifier );
	}

	@Override
	public ExistingForeignKeyMetadata getForeignKeyMetadata(ForeignKey fk) {
//		Iterator it = foreignKeys.values().iterator();
//		while ( it.hasNext() ) {
//			ForeignKeyMetadata existingFk = ( ForeignKeyMetadata ) it.next();
//			if ( existingFk.matches( fk ) ) {
//				return existingFk;
//			}
//		}
		return null;
	}

	@Override
	public String toString() {
		return "ExistingTableMetadataImpl(" + tableName.toString() + ')';
	}

	@Override
	public IndexMetadata getIndexMetadata(Identifier indexName) {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}
}
