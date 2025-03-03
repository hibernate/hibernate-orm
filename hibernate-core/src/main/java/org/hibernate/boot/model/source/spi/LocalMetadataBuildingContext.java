/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.spi.MetadataBuildingContext;

/**
 * Specialization of the MetadataBuildingContext contract specific to a given origin.
 *
 * @author Steve Ebersole
 */
public interface LocalMetadataBuildingContext extends MetadataBuildingContext {
	/**
	 * Obtain the origin for this context
	 *
	 * @return The origin
	 */
	Origin getOrigin();
}
