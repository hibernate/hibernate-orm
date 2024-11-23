/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.spi;

import org.hibernate.Incubating;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.resource.transaction.spi.DdlTransactionIsolator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.InformationExtractor;

/**
 * Encapsulates the functionality for extracting database metadata used by
 * {@link SchemaManagementTool}.
 *
 * @author Gail Badner
 */
@Incubating
public interface ExtractionTool {

	ExtractionContext createExtractionContext(
			ServiceRegistry serviceRegistry,
			JdbcEnvironment jdbcEnvironment,
			SqlStringGenerationContext sqlStringGenerationContext,
			DdlTransactionIsolator ddlTransactionIsolator,
			ExtractionContext.DatabaseObjectAccess databaseObjectAccess);

	InformationExtractor createInformationExtractor(ExtractionContext extractionContext);
}
