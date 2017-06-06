/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.spi;

import java.util.Map;

import org.hibernate.Incubating;
import org.hibernate.metamodel.model.relational.spi.DatabaseModel;
import org.hibernate.service.Service;

/**
 * Contract for schema management tool integration.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface SchemaManagementTool extends Service {
	SchemaCreator getSchemaCreator(DatabaseModel databaseModel, Map options);

	SchemaDropper getSchemaDropper(DatabaseModel databaseModel, Map options);

	SchemaMigrator getSchemaMigrator(DatabaseModel databaseModel, Map options);

	SchemaValidator getSchemaValidator(DatabaseModel databaseModel, Map options);
}
