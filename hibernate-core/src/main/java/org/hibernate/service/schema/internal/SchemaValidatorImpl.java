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

import org.hibernate.metamodel.spi.relational.Database;
import org.hibernate.metamodel.spi.relational.Schema;
import org.hibernate.metamodel.spi.relational.Sequence;
import org.hibernate.metamodel.spi.relational.Table;
import org.hibernate.service.schema.spi.ExistingDatabaseMetaData;
import org.hibernate.service.schema.spi.ExistingSequenceMetadata;
import org.hibernate.service.schema.spi.ExistingTableMetadata;
import org.hibernate.service.schema.spi.SchemaValidator;

/**
 * @author Steve Ebersole
 */
public class SchemaValidatorImpl implements SchemaValidator {
	@Override
	public void doValidation(Database database, ExistingDatabaseMetaData existingDatabaseMetaData) {
		for ( Schema schema : database.getSchemas() ) {
			for ( Table table : schema.getTables() ) {
				final ExistingTableMetadata existingTableMetadata = existingDatabaseMetaData.getTableMetadata(
						table.getTableName()
				);
				validateTable( table, existingTableMetadata );
			}
		}

		for ( Schema schema : database.getSchemas() ) {
			for ( Sequence sequence : schema.getSequences() ) {
				final ExistingSequenceMetadata existingSequenceMetadata = existingDatabaseMetaData.getSequenceMetadata(
						sequence.getName()
				);
				validateSequence( sequence, existingSequenceMetadata );
			}
		}
	}

	private void validateTable(Table table, ExistingTableMetadata existingTableMetadata) {
		//To change body of created methods use File | Settings | File Templates.
	}

	private void validateSequence(Sequence sequence, ExistingSequenceMetadata existingSequenceMetadata) {
		//To change body of created methods use File | Settings | File Templates.
	}
}
