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

import java.util.HashMap;
import java.util.Map;

import org.hibernate.metamodel.spi.relational.Identifier;
import org.hibernate.metamodel.spi.relational.ObjectName;
import org.hibernate.tool.schema.extract.spi.ColumnInformation;
import org.hibernate.tool.schema.extract.spi.ForeignKeyInformation;
import org.hibernate.tool.schema.extract.spi.IndexInformation;
import org.hibernate.tool.schema.extract.spi.PrimaryKeyInformation;
import org.hibernate.tool.schema.extract.spi.SchemaMetaDataExtractor;
import org.hibernate.tool.schema.extract.spi.TableInformation;

/**
 * Provides access to information about existing schema objects (tables, sequences etc) of existing database.
 *
 * @author Christoph Sturm
 * @author Max Rydahl Andersen
 * @author Steve Ebersole
 */
public class TableInformationImpl implements TableInformation {
	private final SchemaMetaDataExtractor metaDataExtractor;
	private final ObjectName tableName;
	private final boolean physicalTable;
	private final String comment;

	private Map<Identifier, ColumnInformation> columns;
	private PrimaryKeyInformation primaryKey;
	private Map<Identifier, ForeignKeyInformation> foreignKeys;
	private Map<Identifier, IndexInformation> indexes;

	private boolean wasPrimaryKeyLoaded = false; // to avoid multiple db reads since primary key can be null.

	public TableInformationImpl(
			SchemaMetaDataExtractor metaDataExtractor,
			ObjectName tableName,
			boolean physicalTable,
			String comment) {
		this.metaDataExtractor = metaDataExtractor;
		this.tableName = tableName;
		this.physicalTable = physicalTable;
		this.comment = comment;
	}

	@Override
	public ObjectName getName() {
		return tableName;
	}

	@Override
	public boolean isPhysicalTable() {
		return physicalTable;
	}

	@Override
	public String getComment() {
		return comment;
	}

	@Override
	public Iterable<ColumnInformation> getColumns() {
		return columns().values();
	}

	protected Map<Identifier, ColumnInformation> columns() {
		if ( this.columns == null ) {
			final Map<Identifier, ColumnInformation> columnMap = new HashMap<Identifier, ColumnInformation>();
			final Iterable<ColumnInformation> columnInformationItr = metaDataExtractor.getColumns( this );
			for ( ColumnInformation columnInformation : columnInformationItr ) {
				columnMap.put( columnInformation.getColumnIdentifier(), columnInformation );
			}
			this.columns = columnMap;
		}
		return this.columns;
	}

	@Override
	public ColumnInformation getColumn(Identifier columnIdentifier) {
		return columns().get( columnIdentifier );
	}

	@Override
	public PrimaryKeyInformation getPrimaryKey() {
		if ( ! wasPrimaryKeyLoaded ) {
			primaryKey = metaDataExtractor.getPrimaryKey( this );
			wasPrimaryKeyLoaded = true;
		}
		return primaryKey;
	}

	@Override
	public Iterable<ForeignKeyInformation> getForeignKeys() {
		return foreignKeys().values();
	}

	protected Map<Identifier, ForeignKeyInformation> foreignKeys() {
		if ( foreignKeys == null ) {
			final Map<Identifier, ForeignKeyInformation> fkMap = new HashMap<Identifier, ForeignKeyInformation>();
			final Iterable<ForeignKeyInformation> fks = metaDataExtractor.getForeignKeys( this );
			for ( ForeignKeyInformation fk : fks ) {
				fkMap.put( fk.getForeignKeyIdentifier(), fk );
			}
			this.foreignKeys = fkMap;
		}
		return foreignKeys;
	}

	@Override
	public ForeignKeyInformation getForeignKey(Identifier fkIdentifier) {
		return foreignKeys().get( fkIdentifier );
	}

	@Override
	public Iterable<IndexInformation> getIndexes() {
		return indexes().values();
	}

	protected Map<Identifier, IndexInformation> indexes() {
		if ( indexes == null ) {
			final Map<Identifier, IndexInformation> indexMap = new HashMap<Identifier, IndexInformation>();
			final Iterable<IndexInformation> indexes = metaDataExtractor.getIndexes( this );
			for ( IndexInformation index : indexes ) {
				indexMap.put( index.getIndexIdentifier(), index );
			}
			this.indexes = indexMap;
		}
		return indexes;
	}

	@Override
	public IndexInformation getIndex(Identifier indexName) {
		return indexes().get( indexName );
	}

	@Override
	public String toString() {
		return "TableInformationImpl(" + tableName.toString() + ')';
	}
}
