/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.internal;

import java.util.Locale;

import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.model.relational.spi.DatabaseModel;
import org.hibernate.metamodel.model.relational.spi.ExportableTable;
import org.hibernate.metamodel.model.relational.spi.Namespace;
import org.hibernate.metamodel.model.relational.spi.PhysicalColumn;
import org.hibernate.metamodel.model.relational.spi.Sequence;
import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.tool.schema.extract.spi.ColumnInformation;
import org.hibernate.tool.schema.extract.spi.DatabaseInformation;
import org.hibernate.tool.schema.extract.spi.SequenceInformation;
import org.hibernate.tool.schema.extract.spi.TableInformation;
import org.hibernate.tool.schema.internal.exec.JdbcContext;
import org.hibernate.tool.schema.spi.ExecutionOptions;
import org.hibernate.tool.schema.spi.SchemaFilter;
import org.hibernate.tool.schema.spi.SchemaManagementException;
import org.hibernate.tool.schema.spi.SchemaValidator;
import org.hibernate.type.descriptor.sql.spi.JdbcTypeNameMapper;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSchemaValidator implements SchemaValidator {
	private static final Logger log = Logger.getLogger( AbstractSchemaValidator.class );

	protected final HibernateSchemaManagementTool tool;
	protected final DatabaseModel databaseModel;
	protected final SchemaFilter schemaFilter;

	public AbstractSchemaValidator(
			HibernateSchemaManagementTool tool,
			DatabaseModel databaseModel,
			SchemaFilter validateFilter) {
		this.tool = tool;
		this.databaseModel = databaseModel;
		if ( validateFilter == null ) {
			this.schemaFilter = DefaultSchemaFilter.INSTANCE;
		}
		else {
			this.schemaFilter = validateFilter;
		}
	}

	@Override
	public void doValidation(ExecutionOptions options) {
		final JdbcContext jdbcContext = tool.resolveJdbcContext( options.getConfigurationValues() );

		final DdlTransactionIsolator isolator = tool.getDdlTransactionIsolator( jdbcContext );

		final DatabaseInformation databaseInformation = Helper.buildDatabaseInformation(
				tool.getServiceRegistry(),
				isolator,
				databaseModel.getDefaultNamespace()
		);

		try {
			performValidation( databaseInformation, options, jdbcContext.getDialect() );
		}
		finally {
			try {
				databaseInformation.cleanup();
			}
			catch (Exception e) {
				log.debug( "Problem releasing DatabaseInformation : " + e.getMessage() );
			}

			isolator.release();
		}
	}

	public void performValidation(
			DatabaseInformation databaseInformation,
			ExecutionOptions options,
			Dialect dialect) {
		for ( Namespace namespace : databaseModel.getNamespaces() ) {
			if ( schemaFilter.includeNamespace( namespace ) ) {
				validateTables( databaseInformation, options, dialect, namespace );
			}
		}

		for ( Namespace namespace : databaseModel.getNamespaces() ) {
			if ( schemaFilter.includeNamespace( namespace ) ) {
				for ( Sequence sequence : namespace.getSequences() ) {
					if ( schemaFilter.includeSequence( sequence ) ) {
						final SequenceInformation sequenceInformation = databaseInformation.getSequenceInformation(
								sequence.getQualifiedName()
						);
						validateSequence( sequence, sequenceInformation );
					}
				}
			}
		}
	}

	protected abstract void validateTables(
			DatabaseInformation databaseInformation,
			ExecutionOptions options,
			Dialect dialect,
			Namespace namespace);

	protected void validateTable(
			ExportableTable table,
			TableInformation tableInformation,
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

		for ( PhysicalColumn column : table.getPhysicalColumns() ) {
			final ColumnInformation existingColumn = tableInformation.getColumn( column.getName() );
			if ( existingColumn == null ) {
				throw new SchemaManagementException(
						String.format(
								"Schema-validation: missing column [%s] in table [%s]",
								column.getName(),
								table.getQualifiedTableName()
						)
				);
			}
			validateColumnType( table, column, existingColumn, options, dialect );
		}
	}

	protected void validateColumnType(
			ExportableTable table,
			PhysicalColumn column,
			ColumnInformation columnInformation,
			ExecutionOptions options,
			Dialect dialect) {
		boolean typesMatch = column.getSqlTypeDescriptor().getJdbcTypeCode() == columnInformation.getTypeCode()
				|| column.getSqlTypeName().toLowerCase( Locale.ROOT ).startsWith( columnInformation.getTypeName().toLowerCase( Locale.ROOT ) );

		if ( !typesMatch ) {
			throw new SchemaManagementException(
					String.format(
							"Schema-validation: wrong column type encountered in column [%s] in " +
									"table [%s]; found [%s (Types#%s)], but expecting [%s (Types#%s)]",
							column.getName(),
							table.getQualifiedTableName(),
							columnInformation.getTypeName().toLowerCase( Locale.ROOT ),
							JdbcTypeNameMapper.getTypeName( columnInformation.getTypeCode() ),
							column.getSqlTypeName().toLowerCase( Locale.ROOT ),
							JdbcTypeNameMapper.getTypeName( column.getSqlTypeDescriptor().getJdbcTypeCode() )
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
