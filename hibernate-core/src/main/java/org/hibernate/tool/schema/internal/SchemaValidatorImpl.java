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
package org.hibernate.tool.schema.internal;

import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.Database;
import org.hibernate.metamodel.spi.relational.Schema;
import org.hibernate.metamodel.spi.relational.Sequence;
import org.hibernate.metamodel.spi.relational.Table;
import org.hibernate.metamodel.spi.relational.Value;
import org.hibernate.tool.schema.extract.spi.ColumnInformation;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;
import org.hibernate.tool.schema.extract.spi.TableInformation;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.hibernate.tool.schema.spi.SchemaValidator;

/**
 * @author Steve Ebersole
 */
public class SchemaValidatorImpl implements SchemaValidator {
	
	private final Dialect dialect;
	
	public SchemaValidatorImpl(Dialect dialect) {
		this.dialect = dialect;
	}
	
	@Override
	public void doValidation(Database database, DatabaseInformation databaseInformation) {
		for ( Schema schema : database.getSchemas() ) {
			for ( Table table : schema.getTables() ) {
				if( !table.isPhysicalTable() ){
					continue;
				}
				final TableInformation tableInformation = databaseInformation.getTableInformation(
						table.getTableName()
				);
				validateTable( table, tableInformation );
			}
		}

		for ( Schema schema : database.getSchemas() ) {
			for ( Sequence sequence : schema.getSequences() ) {
				final SequenceInformation sequenceInformation = databaseInformation.getSequenceInformation(
						sequence.getName()
				);
				validateSequence( sequence, sequenceInformation );
			}
		}
	}

	protected void validateTable(Table table, TableInformation tableInformation) {
		if ( tableInformation == null ) {
			throw new SchemaManagementException(
					String.format(
							"Schema-validation: missing table [%s]",
							table.getTableName().toText()
					)
			);
		}

		for ( Value value : table.values() ) {
			if ( Column.class.isInstance( value ) ) {
				final Column column = (Column) value;
				final ColumnInformation columnInformation = tableInformation.getColumn( column.getColumnName() );
				if ( columnInformation == null ) {
					throw new SchemaManagementException(
							String.format(
									"Schema-validation: missing column [%s] in table [%s]",
									column.getColumnName().getText(),
									table.getTableName().toText()
							)
					);
				}

				validateColumnType( table, column, columnInformation );
			}
		}
	}

	protected void validateColumnType(Table table, Column column, ColumnInformation columnInformation) {
		// this is the old Hibernate check...
		final boolean typesMatch = column.getJdbcDataType().getTypeCode() == columnInformation.getTypeCode()
				|| column.getSqlTypeString(dialect).toLowerCase().startsWith( columnInformation.getTypeName().toLowerCase() );
		if ( !typesMatch ) {
			throw new SchemaManagementException(
					String.format(
							"Schema-validation: wrong column type encountered in column [%s] in table [%s]; found [%s], but expecting [%s]",
							column.getColumnName().getText(),
							table.getTableName().toText(),
							columnInformation.getTypeName().toLowerCase(),
							column.getSqlType().toLowerCase()
					)
			);
		}

		// but I think a better check involves checks against type code and then the type code family, not
		// just the type name.
		//
		// See org.hibernate.type.descriptor.sql.JdbcTypeFamilyInformation

	}

	protected void validateSequence(Sequence sequence, SequenceInformation sequenceInformation) {
		if ( sequenceInformation == null ) {
			throw new SchemaManagementException(
					String.format( "Schema-validation: missing sequence [%s]", sequence.getName().toText() )
			);
		}

		if ( sequenceInformation.getIncrementSize() > 0
				&& sequence.getIncrementSize() != sequenceInformation.getIncrementSize() ) {
			throw new SchemaManagementException(
					String.format(
							"Schema-validation: sequence [%s] defined inconsistent increment-size; found [%s] but expecting [%s]",
							sequence.getName().toText(),
							sequenceInformation.getIncrementSize(),
							sequence.getIncrementSize()
					)
			);
		}
	}
}
