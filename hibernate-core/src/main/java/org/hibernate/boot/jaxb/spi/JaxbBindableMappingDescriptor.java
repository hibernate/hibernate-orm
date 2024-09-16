/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.jaxb.spi;

/**
 * Common type for things that can get be bound to a {@link Binding} for
 * mapping documents.
 *
 * @apiNote The models generated from the hbm.xml and mapping.xml schemas
 * both implement it.
 *
 * @author Steve Ebersole
 */
public interface JaxbBindableMappingDescriptor {
}
