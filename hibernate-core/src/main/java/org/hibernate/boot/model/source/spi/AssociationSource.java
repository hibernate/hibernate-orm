/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

/**
 * @author Gail Badner
 */
public interface AssociationSource {

	AttributeSource getAttributeSource();

	/**
	 * Obtain the name of the referenced entity.
	 *
	 * @return The name of the referenced entity
	 */
	String getReferencedEntityName();

	boolean isIgnoreNotFound();

	boolean isMappedBy();
}
