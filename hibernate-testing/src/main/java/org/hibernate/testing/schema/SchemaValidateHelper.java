/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
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
