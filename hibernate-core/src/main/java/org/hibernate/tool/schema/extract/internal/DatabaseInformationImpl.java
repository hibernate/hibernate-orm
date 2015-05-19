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
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.model.relational.Schema;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;
import org.hibernate.tool.schema.extract.spi.TableInformation;

/**
 * @author Steve Ebersole
 */
public class DatabaseInformationImpl implements DatabaseInformation, ExtractionContext.DatabaseObjectAccess {
	private final Map<QualifiedTableName,TableInformation> tableInformationMap = new HashMap<QualifiedTableName, TableInformation>();
	private final Map<QualifiedSequenceName,SequenceInformation> sequenceInformationMap = new HashMap<QualifiedSequenceName, SequenceInformation>();

	public DatabaseInformationImpl() {
	}


	// DatabaseInformation implementation ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean schemaExists(Schema.Name schema) {
		return false;
	}

	@Override
	public TableInformation getTableInformation(Identifier catalogName, Identifier schemaName, Identifier tableName) {
		return locateTableInformation( new QualifiedTableName( catalogName, schemaName, tableName ) );
	}

	@Override
	public TableInformation getTableInformation(Schema.Name schemaName, Identifier tableName) {
		return locateTableInformation( new QualifiedTableName( schemaName, tableName ) );
	}

	@Override
	public TableInformation getTableInformation(QualifiedTableName tableName) {
		return locateTableInformation( tableName );
	}

	@Override
	public SequenceInformation getSequenceInformation(Identifier catalogName, Identifier schemaName, Identifier sequenceName) {
		return locateSequenceInformation( new QualifiedSequenceName( catalogName, schemaName, sequenceName ) );
	}

	@Override
	public SequenceInformation getSequenceInformation(Schema.Name schemaName, Identifier sequenceName) {
		return locateSequenceInformation( new QualifiedSequenceName( schemaName, sequenceName ) );
	}

	@Override
	public SequenceInformation getSequenceInformation(QualifiedSequenceName sequenceName) {
		return locateSequenceInformation( sequenceName );
	}


	// RegisteredObjectAccess implementation ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public void registerTableInformation(TableInformation tableInformation) {
		tableInformationMap.put( tableInformation.getName(), tableInformation );
	}

	public void registerSequenceInformation(SequenceInformation sequenceInformation) {
		sequenceInformationMap.put( sequenceInformation.getSequenceName(), sequenceInformation );
	}

	@Override
	public TableInformation locateTableInformation(QualifiedTableName tableName) {
		return tableInformationMap.get( tableName );
	}

	@Override
	public SequenceInformation locateSequenceInformation(QualifiedSequenceName sequenceName) {
		return sequenceInformationMap.get( sequenceName );
	}

	@Override
	public void registerTable(TableInformation tableInformation) {
		tableInformationMap.put( tableInformation.getName(), tableInformation );
	}
}
