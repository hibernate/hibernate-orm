/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
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
public class DatabaseInformationImpl implements DatabaseInformation, ExtractionContext.RegisteredObjectAccess {
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
		return locateRegisteredTableInformation( new QualifiedTableName( catalogName, schemaName, tableName ) );
	}

	@Override
	public TableInformation getTableInformation(Schema.Name schemaName, Identifier tableName) {
		return locateRegisteredTableInformation( new QualifiedTableName( schemaName, tableName ) );
	}

	@Override
	public TableInformation getTableInformation(QualifiedTableName tableName) {
		return locateRegisteredTableInformation( tableName );
	}

	@Override
	public SequenceInformation getSequenceInformation(Identifier catalogName, Identifier schemaName, Identifier sequenceName) {
		return locateRegisteredSequenceInformation( new QualifiedSequenceName( catalogName, schemaName, sequenceName ) );
	}

	@Override
	public SequenceInformation getSequenceInformation(Schema.Name schemaName, Identifier sequenceName) {
		return locateRegisteredSequenceInformation( new QualifiedSequenceName( schemaName, sequenceName ) );
	}

	@Override
	public SequenceInformation getSequenceInformation(QualifiedSequenceName sequenceName) {
		return locateRegisteredSequenceInformation( sequenceName );
	}


	// RegisteredObjectAccess implementation ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public void registerTableInformation(TableInformation tableInformation) {
		tableInformationMap.put( tableInformation.getName(), tableInformation );
	}

	public void registerSequenceInformation(SequenceInformation sequenceInformation) {
		sequenceInformationMap.put( sequenceInformation.getSequenceName(), sequenceInformation );
	}

	@Override
	public TableInformation locateRegisteredTableInformation(QualifiedTableName tableName) {
		return tableInformationMap.get( tableName );
	}

	@Override
	public SequenceInformation locateRegisteredSequenceInformation(QualifiedSequenceName sequenceName) {
		return sequenceInformationMap.get( sequenceName );
	}

	@Override
	public void registerTable(TableInformation tableInformation) {
		tableInformationMap.put( tableInformation.getName(), tableInformation );
	}
}
