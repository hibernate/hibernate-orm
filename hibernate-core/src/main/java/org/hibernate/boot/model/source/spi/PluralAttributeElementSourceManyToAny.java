/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

import org.hibernate.Remove;

/**
 * Describes the source for the elements of persistent collections (plural
 * attributes) where the elements are defined by Hibernate's any mapping
 *
 * @author Steve Ebersole
 */
@Remove
public interface PluralAttributeElementSourceManyToAny
		extends PluralAttributeElementSource, AnyMappingSource {
}
