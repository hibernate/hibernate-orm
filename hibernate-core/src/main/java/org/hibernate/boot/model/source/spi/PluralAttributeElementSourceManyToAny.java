/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

/**
 * Describes the source for the elements of persistent collections (plural
 * attributes) where the elements are defined by Hibernate's any mapping
 *
 * @author Steve Ebersole
 */
public interface PluralAttributeElementSourceManyToAny
		extends PluralAttributeElementSource, AnyMappingSource {
}
