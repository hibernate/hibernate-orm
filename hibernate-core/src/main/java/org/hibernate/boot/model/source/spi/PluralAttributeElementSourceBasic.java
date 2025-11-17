/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.spi;

import org.hibernate.boot.model.naming.ImplicitBasicColumnNameSource;

/**
 * Describes the source for the elements of persistent collections (plural
 * attributes) where the elements are basic types
 *
 * @author Steve Ebersole
 */
public interface PluralAttributeElementSourceBasic
		extends PluralAttributeElementSource,
				RelationalValueSourceContainer,
				ImplicitBasicColumnNameSource {
	HibernateTypeSource getExplicitHibernateTypeSource();
}
