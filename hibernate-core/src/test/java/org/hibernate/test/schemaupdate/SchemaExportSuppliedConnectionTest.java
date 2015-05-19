/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.hbm2ddl.SchemaExport;

/**
 * @author Gail Badner
 */
public class SchemaExportSuppliedConnectionTest extends SchemaExportTest {
	@Override
	protected SchemaExport createSchemaExport(MetadataImplementor metadata, ServiceRegistry serviceRegistry) {
		return new SchemaExport( serviceRegistry, metadata );
	}
}
