/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.spi;

import org.hibernate.mapping.Property;

/**
 * @author Steve Ebersole
 */
public interface NaturalIdUniqueKeyBinder {
	/**
	 * Adds an attribute binding.  The attribute is a (top-level) part of the natural-id
	 *
	 * @param attributeBinding The attribute binding that is part of the natural-id
	 */
	void addAttributeBinding(Property attributeBinding);

	void process();
}
