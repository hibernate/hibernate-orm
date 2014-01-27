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

import org.hibernate.metamodel.spi.relational.ObjectName;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;
import org.hibernate.tool.schema.extract.spi.TableInformation;

/**
 * @author Steve Ebersole
 */
public class DatabaseInformationImpl implements DatabaseInformation, ExtractionContext.RegisteredObjectAccess {
	private final Map<ObjectName,TableInformation> tables = new HashMap<ObjectName, TableInformation>();
	private final Map<ObjectName,SequenceInformation> sequences = new HashMap<ObjectName, SequenceInformation>();

	public DatabaseInformationImpl() {
	}

	// DatabaseInformation implementation ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public TableInformation getTableInformation(ObjectName tableName) {
		return locateRegisteredTableInformation( tableName );
	}

	@Override
	public SequenceInformation getSequenceInformation(ObjectName sequenceName) {
		return locateRegisteredSequenceInformation( sequenceName );
	}


	// RegisteredObjectAccess implementation ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public TableInformation locateRegisteredTableInformation(ObjectName tableName) {
		return tables.get( tableName );
	}

	public void registerTableInformation(TableInformation tableInformation) {
		tables.put( tableInformation.getName(), tableInformation );
	}

	@Override
	public SequenceInformation locateRegisteredSequenceInformation(ObjectName sequenceName) {
		return sequences.get( sequenceName );
	}

	public void registerSequenceInformation(SequenceInformation sequenceInformation) {
		sequences.put( sequenceInformation.getSequenceName(), sequenceInformation );
	}
}
