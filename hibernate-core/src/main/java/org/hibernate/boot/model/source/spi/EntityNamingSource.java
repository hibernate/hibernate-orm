/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

import org.hibernate.boot.model.naming.EntityNaming;

/**
 * Naming information about an entity.
 *
 * @author Steve Ebersole
 */
public interface EntityNamingSource extends EntityNaming {
	/**
	 * Decode the name that we should expect to be used elsewhere to reference
	 * the modeled entity by decoding the entity-name/class-name combo.
	 *
	 * @return The reference-able type name
	 */
	String getTypeName();
}
