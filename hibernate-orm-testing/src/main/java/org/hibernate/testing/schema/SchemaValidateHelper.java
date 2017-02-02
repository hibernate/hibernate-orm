/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing.schema;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.service.ServiceRegistry;

/**
 * @author Steve Ebersole
 */
public class SchemaValidateHelper {
	public static void validate(Metadata metadata) {
		validate( metadata, ( ( MetadataImplementor) metadata ).getMetadataBuildingOptions().getServiceRegistry() );
	}

	public static void validate(Metadata metadata, ServiceRegistry serviceRegistry) {

	}
}
