/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import java.util.Iterator;
import java.util.Locale;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.dialect.Dialect;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Table;
import org.hibernate.tool.schema.extract.spi.ColumnInformation;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;
import org.hibernate.tool.schema.extract.spi.TableInformation;
import org.hibernate.tool.schema.internal.exec.JdbcConnectionContextNonSharedImpl;
import org.hibernate.tool.schema.internal.exec.JdbcContext;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaFilter;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.hibernate.tool.schema.spi.SchemaValidator;
import org.hibernate.type.descriptor.JdbcTypeNameMapper;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public class SchemaValidatorImpl implements SchemaValidator {
	private static final Logger log = Logger.getLogger( SchemaValidatorImpl.class );

	private final HibernateSchemaManagementTool tool;
	private final SchemaFilter schemaFilter;

	public SchemaValidatorImpl(HibernateSchemaManagementTool tool) {
		this( tool, DefaultSchemaFilter.INSTANCE );
	}

	public SchemaValidatorImpl(HibernateSchemaManagementTool tool, SchemaFilter schemaFilter) {
		this.tool = tool;
		this.schemaFilter = schemaFilter;
	}

	@Override
	public void doValidation(Metadata metadata, ExecutionOptions options) {
		final JdbcContext jdbcContext = tool.resolveJdbcContext( options.getConfigurationValues() );

		final DatabaseInformation databaseInformation = Helper.buildDatabaseInformation(
				tool.getServiceRegistry(),
				new JdbcConnectionContextNonSharedImpl(
						jdbcContext.getJdbcConnectionAccess(),
						jdbcContext.getSqlStatementLogger(),
						false
				),
				metadata.getDatabase().getDefaultNamespace().getName()
		);

		try {
			performValidation( metadata, databaseInformation, options, jdbcContext.getDialect() );
		}
		finally {
			try {
				databaseInformation.cleanup();
			}
			catch (Exception e) {
				log.debug( "Problem releasing DatabaseInformation : " + e.getMessage() );
			}
		}
	}

	public void performValidation(
			Metadata metadata,
			DatabaseInformation databaseInformation,
			ExecutionOptions options,
			Dialect dialect) {
		for ( Namespace namespace : metadata.getDatabase().getNamespaces() ) {
			if ( !schemaFilter.includeNamespace( namespace )) {
				continue;
			}
			
			for ( Table table : namespace.getTables() ) {
				if ( !schemaFilter.includeTable( table )) {
					continue;
				}
				if ( !table.isPhysicalTable() ) {
					continue;
				}

				final TableInformation tableInformation = databaseInformation.getTableInformation(
						table.getQualifiedTableName()
				);
				validateTable( table, tableInformation, metadata, options, dialect );
			}
		}

		for ( Namespace namespace : metadata.getDatabase().getNamespaces() ) {
			if ( !schemaFilter.includeNamespace( namespace )) {
				continue;
			}
			
			for ( Sequence sequence : namespace.getSequences() ) {
				if ( !schemaFilter.includeSequence( sequence )) {
					continue;
				}
				
				final SequenceInformation sequenceInformation = databaseInformation.getSequenceInformation(
						sequence.getName()
				);
				validateSequence( sequence, sequenceInformation );
			}
		}
	}

	protected void validateTable(
			Table table,
			TableInformation tableInformation,
			Metadata metadata,
			ExecutionOptions options,
			Dialect dialect) {
		if ( tableInformation == null ) {
			throw new SchemaManagementException(
					String.format(
							"Schema-validation: missing table [%s]",
							table.getQualifiedTableName().toString()
					)
			);
		}

		final Iterator selectableItr = table.getColumnIterator();
		while ( selectableItr.hasNext() ) {
			final Selectable selectable = (Selectable) selectableItr.next();
			if ( !Column.class.isInstance( selectable ) ) {
				continue;
			}

			final Column column = (Column) selectable;
			final ColumnInformation existingColumn = tableInformation.getColumn( Identifier.toIdentifier( column.getQuotedName() ) );
			if ( existingColumn == null ) {
				throw new SchemaManagementException(
						String.format(
								"Schema-validation: missing column [%s] in table [%s]",
								column.getName(),
								table.getQualifiedTableName()
						)
				);
			}
			validateColumnType( table, column, existingColumn, metadata, options, dialect );
		}
	}

	protected void validateColumnType(
			Table table,
			Column column,
			ColumnInformation columnInformation,
			Metadata metadata,
			ExecutionOptions options,
			Dialect dialect) {
		boolean typesMatch = column.getSqlTypeCode( metadata ) == columnInformation.getTypeCode()
				|| column.getSqlType( dialect, metadata ).toLowerCase(Locale.ROOT).startsWith( columnInformation.getTypeName().toLowerCase(Locale.ROOT) );
		if ( !typesMatch ) {
			throw new SchemaManagementException(
					String.format(
							"Schema-validation: wrong column type encountered in column [%s] in " +
									"table [%s]; found [%s (Types#%s)], but expecting [%s (Types#%s)]",
							column.getName(),
							table.getQualifiedTableName(),
							columnInformation.getTypeName().toLowerCase(Locale.ROOT),
							JdbcTypeNameMapper.getTypeName( columnInformation.getTypeCode() ),
							column.getSqlType().toLowerCase(Locale.ROOT),
							JdbcTypeNameMapper.getTypeName( column.getSqlTypeCode( metadata ) )
					)
			);
		}

		// this is the old Hibernate check...
		//
		// but I think a better check involves checks against type code and then the type code family, not
		// just the type name.
		//
		// See org.hibernate.type.descriptor.sql.JdbcTypeFamilyInformation
		// todo : this ^^
	}

	protected void validateSequence(Sequence sequence, SequenceInformation sequenceInformation) {
		if ( sequenceInformation == null ) {
			throw new SchemaManagementException(
					String.format( "Schema-validation: missing sequence [%s]", sequence.getName() )
			);
		}

		if ( sequenceInformation.getIncrementSize() > 0
				&& sequence.getIncrementSize() != sequenceInformation.getIncrementSize() ) {
			throw new SchemaManagementException(
					String.format(
							"Schema-validation: sequence [%s] defined inconsistent increment-size; found [%s] but expecting [%s]",
							sequence.getName(),
							sequenceInformation.getIncrementSize(),
							sequence.getIncrementSize()
					)
			);
		}
	}
}
