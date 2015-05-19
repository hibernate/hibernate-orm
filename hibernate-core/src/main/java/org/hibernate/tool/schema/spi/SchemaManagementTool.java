/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.spi;

import java.util.Map;

import org.hibernate.service.Service;

/**
 * Contract for schema management tool integration.
 *
 * @author Steve Ebersole
 */
public interface SchemaManagementTool extends Service {
	public SchemaCreator getSchemaCreator(Map options);
	public SchemaDropper getSchemaDropper(Map options);
	public SchemaMigrator getSchemaMigrator(Map options);
	public SchemaValidator getSchemaValidator(Map options);
}
