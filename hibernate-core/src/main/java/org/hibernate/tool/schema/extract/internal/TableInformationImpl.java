/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.extract.internal;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
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
	private final IdentifierHelper identifierHelper;

	private final QualifiedTableName tableName;
	private final boolean physicalTable;
	private final String comment;

	private PrimaryKeyInformation primaryKey;
	private Map<Identifier, ForeignKeyInformation> foreignKeys;
	private Map<Identifier, IndexInformation> indexes;
	private final Map<Identifier, ColumnInformation> columns = new HashMap<>();

	private boolean wasPrimaryKeyLoaded = false; // to avoid multiple db reads since primary key can be null.

	public TableInformationImpl(
			InformationExtractor extractor,
			IdentifierHelper identifierHelper,
			QualifiedTableName tableName,
			boolean physicalTable,
			String comment ) {
		this.extractor = extractor;
		this.identifierHelper = identifierHelper;
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
		return columns.get( new Identifier(
				identifierHelper.toMetaDataObjectName( columnIdentifier ),
				false
		) );
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
			final Map<Identifier, ForeignKeyInformation> result = new HashMap<>();
			for ( var foreignKeyInformation : extractor.getForeignKeys( this ) ) {
				result.put( foreignKeyInformation.getForeignKeyIdentifier(), foreignKeyInformation );
			}
			foreignKeys = result;
		}
		return foreignKeys;
	}

	@Override
	public ForeignKeyInformation getForeignKey(Identifier fkIdentifier) {
		return foreignKeys().get( new Identifier(
				identifierHelper.toMetaDataObjectName( fkIdentifier ),
				false
		)  );
	}

	@Override
	public Iterable<IndexInformation> getIndexes() {
		return indexes().values();
	}

	protected Map<Identifier, IndexInformation> indexes() {
		if ( indexes == null ) {
			final Map<Identifier, IndexInformation> indexMap = new HashMap<>();
			for ( var index : extractor.getIndexes( this ) ) {
				indexMap.put( index.getIndexIdentifier(), index );
			}
			this.indexes = indexMap;
		}
		return indexes;
	}

	@Override
	public void addColumn(ColumnInformation columnIdentifier) {
		columns.put( columnIdentifier.getColumnIdentifier(), columnIdentifier );
	}

	@Override
	public IndexInformation getIndex(Identifier indexName) {
		return indexes().get( new Identifier(
				identifierHelper.toMetaDataObjectName( indexName ),
				false
		) );
	}

	@Override
	public String toString() {
		return "TableInformationImpl(" + tableName.toString() + ')';
	}
}
