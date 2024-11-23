/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

/**
 * Describes possible natures of a singular attribute.
 *
 * @author Steve Ebersole
 */
public enum SingularAttributeNature {
	BASIC,
	// TODO: COMPOSITE should be changed to AGGREGATE
	// when non-aggregated composite IDs are no longer
	// modelled as an AttributeBinding
	COMPOSITE,
	MANY_TO_ONE,
	ONE_TO_ONE,
	ANY
}
