/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.spi;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.Incubating;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.service.Service;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.extract.internal.InformationExtractorJdbcDatabaseMetaDataImpl;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.InformationExtractor;
import org.hibernate.tool.schema.internal.exec.GenerationTarget;
import org.hibernate.tool.schema.internal.exec.ImprovedExtractionContextImpl;

/**
 * Contract for schema management tool integration.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface SchemaManagementTool extends Service {
	SchemaCreator getSchemaCreator(Map options);
	SchemaDropper getSchemaDropper(Map options);
	SchemaMigrator getSchemaMigrator(Map options);
	SchemaValidator getSchemaValidator(Map options);

	/**
	 * This allows to set an alternative implementation for the Database
	 * generation target.
	 * Used by Hibernate Reactive so that it can use the reactive database
	 * access rather than needing a JDBC connection.
	 * @param generationTarget the custom instance to use.
	 */
	void setCustomDatabaseGenerationTarget(GenerationTarget generationTarget);

	default ExtractionContext createExtractionContext(
			ServiceRegistry serviceRegistry,
			JdbcEnvironment jdbcEnvironment,
			DdlTransactionIsolator ddlTransactionIsolator,
			Identifier defaultCatalog,
			Identifier defaultSchema,
			ExtractionContext.DatabaseObjectAccess databaseObjectAccess) {
		return new ImprovedExtractionContextImpl(
				serviceRegistry,
				jdbcEnvironment,
				ddlTransactionIsolator,
				defaultCatalog,
				defaultSchema,
				databaseObjectAccess
		);
	}

	default InformationExtractor createInformationExtractor(ExtractionContext extractionContext) {
		return new InformationExtractorJdbcDatabaseMetaDataImpl( extractionContext );
	}
}
