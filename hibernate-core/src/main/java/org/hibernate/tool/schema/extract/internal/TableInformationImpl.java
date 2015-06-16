/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.extract.internal;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.tool.schema.extract.spi.ColumnInformation;
import org.hibernate.tool.schema.extract.spi.ForeignKeyInformation;
import org.hibernate.tool.schema.extract.spi.IndexInformation;
import org.hibernate.tool.schema.extract.spi.InformationExtractor;
import org.hibernate.tool.schema.extract.spi.PrimaryKeyInformation;
import org.hibernate.tool.schema.extract.spi.TableInformation;

/**
 * Provides access to information about existing schema objects (tables, sequences etc) of existing database.
 *
 * @author Christoph Sturm
 * @author Max Rydahl Andersen
 * @author Steve Ebersole
 */
public class TableInformationImpl implements TableInformation {
	private final InformationExtractor extractor;
	private final QualifiedTableName tableName;
	private final boolean physicalTable;
	private final String comment;

	private PrimaryKeyInformation primaryKey;
	private Map<Identifier, ForeignKeyInformation> foreignKeys;
	private Map<Identifier, IndexInformation> indexes;

	private boolean wasPrimaryKeyLoaded = false; // to avoid multiple db reads since primary key can be null.

	public TableInformationImpl(
			InformationExtractor extractor,
			QualifiedTableName tableName,
			boolean physicalTable,
			String comment) {
		this.extractor = extractor;
		this.tableName = tableName;
		this.physicalTable = physicalTable;
		this.comment = comment;
	}

	@Override
	public QualifiedTableName getName() {
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
	public ColumnInformation getColumn(Identifier columnIdentifier) {
		return extractor.getColumn( this, columnIdentifier );
	}

	@Override
	public PrimaryKeyInformation getPrimaryKey() {
		if ( ! wasPrimaryKeyLoaded ) {
			primaryKey = extractor.getPrimaryKey( this );
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
			final Iterable<ForeignKeyInformation> fks = extractor.getForeignKeys( this );
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
			final Iterable<IndexInformation> indexes = extractor.getIndexes( this );
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
