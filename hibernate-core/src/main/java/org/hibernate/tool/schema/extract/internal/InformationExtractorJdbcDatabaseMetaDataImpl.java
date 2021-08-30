/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.extract.internal;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.StringTokenizer;

import org.hibernate.boot.model.naming.DatabaseIdentifier;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.TableInformation;

/**
 * Implementation of the InformationExtractor contract which uses the standard JDBC {@link java.sql.DatabaseMetaData}
 * API for extraction.
 *
 * @author Steve Ebersole
 * @author Gail Badner
 */
public class InformationExtractorJdbcDatabaseMetaDataImpl extends AbstractInformationExtractorImpl {

	public InformationExtractorJdbcDatabaseMetaDataImpl(ExtractionContext extractionContext) {
		super( extractionContext );
	}

	@Override
	protected String getResultSetTableTypesPhysicalTableConstant() {
		return "TABLE";
	}

	@Override
	public <T> T processCatalogsResultSet(ExtractionContext.ResultSetProcessor<T> processor) throws SQLException {
		try (ResultSet resultSet = getExtractionContext().getJdbcDatabaseMetaData().getCatalogs() ) {
			return processor.process( resultSet );
		}
	}

	@Override
	protected <T> T processSchemaResultSet(
			String catalog,
			String schemaPattern,
			ExtractionContext.ResultSetProcessor<T> processor) throws SQLException {
		try (ResultSet resultSet = getExtractionContext().getJdbcDatabaseMetaData().getSchemas(
				catalog,
				schemaPattern ) ) {
			return processor.process( resultSet );
		}
	}

	@Override
	protected <T> T processTableResultSet(
			String catalog,
			String schemaPattern,
			String tableNamePattern,
			String[] types,
			ExtractionContext.ResultSetProcessor<T> processor
	) throws SQLException {
		try (ResultSet resultSet = getExtractionContext().getJdbcDatabaseMetaData().getTables(
				catalog,
				schemaPattern,
				tableNamePattern,
				types)) {
			return processor.process( resultSet );
		}
	}

	@Override
	protected <T> T processColumnsResultSet(
			String catalog,
			String schemaPattern,
			String tableNamePattern,
			String columnNamePattern,
			ExtractionContext.ResultSetProcessor<T> processor) throws SQLException {
		try (ResultSet resultSet = getExtractionContext().getJdbcDatabaseMetaData().getColumns(
				catalog,
				schemaPattern,
				tableNamePattern,
				columnNamePattern )) {
			return processor.process( resultSet );
		}
	}

	@Override
	protected <T> T processPrimaryKeysResultSet(
			String catalogFilter,
			String schemaFilter,
			Identifier tableName,
			ExtractionContext.ResultSetProcessor<T> processor) throws SQLException {
		try( ResultSet resultSet = getExtractionContext().getJdbcDatabaseMetaData().getPrimaryKeys(
				catalogFilter,
				schemaFilter,
				tableName.getText() ) ) {
			return processor.process( resultSet );
		}
	}

	@Override
	protected <T> T processIndexInfoResultSet(
			String catalog,
			String schema,
			String table,
			boolean unique,
			boolean approximate,
			ExtractionContext.ResultSetProcessor<T> processor) throws SQLException {

		try (ResultSet resultSet = getExtractionContext().getJdbcDatabaseMetaData().getIndexInfo(
				catalog,
				schema,
				table,
				unique,
				approximate ) ) {
			return processor.process( resultSet );
		}
	}

	@Override
	protected <T> T processImportedKeysResultSet(
			String catalog,
			String schema,
			String table,
			ExtractionContext.ResultSetProcessor<T> processor) throws SQLException {
		try (ResultSet resultSet = getExtractionContext().getJdbcDatabaseMetaData().getImportedKeys(
				catalog,
				schema,
				table ) ) {
			return processor.process( resultSet );
		}
	}

	protected void addColumns(TableInformation tableInformation) {
		final ExtractionContext extractionContext = getExtractionContext();
		// We use this dummy query to retrieve the table information through the ResultSetMetaData
		// This is significantly better than to use the DatabaseMetaData especially on Oracle with synonyms enable
		final String tableName = extractionContext.getJdbcEnvironment().getQualifiedObjectNameFormatter().format(
				// The name comes from the database, so the case is correct
				// But we quote here to avoid issues with reserved words
				tableInformation.getName().quote(),
				extractionContext.getJdbcEnvironment().getDialect()
		);

		try {
			extractionContext.getQueryResults(
					"select * from " + tableName + " where 1=0",
					null,
					resultSet -> {
						final ResultSetMetaData metaData = resultSet.getMetaData();
						final int columnCount = metaData.getColumnCount();

						for ( int i = 1; i <= columnCount; i++ ) {
							final String columnName = metaData.getColumnName( i );
							final ColumnInformationImpl columnInformation = new ColumnInformationImpl(
									tableInformation,
									DatabaseIdentifier.toIdentifier( columnName ),
									metaData.getColumnType( i ),
									new StringTokenizer( metaData.getColumnTypeName( i ), "() " ).nextToken(),
									metaData.getPrecision( i ),
									metaData.getScale( i ),
									interpretNullable( metaData.isNullable( i ) )
							);
							tableInformation.addColumn( columnInformation );
						}
						return null;
					}
			);
		}
		catch (SQLException e) {
			throw convertSQLException(
					e,
					"Error accessing column metadata: " + tableInformation.getName().toString()
			);
		}
	}
}
