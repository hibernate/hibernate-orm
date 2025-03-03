/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

import org.hibernate.metamodel.CollectionClassification;

/**
 * Used as a SettingProvider to enable backwards compatibility regarding
 * plural {@link java.util.List} mappings when no {@link jakarta.persistence.OrderColumn},
 * {@link org.hibernate.annotations.ListIndexBase},
 * {@link org.hibernate.annotations.CollectionId} or other annotations hinting at
 * the classification to use.
 *
 * Historically, Hibernate classified these as BAG.  6.0 changes that to LIST (with an
 * implied {@link jakarta.persistence.OrderColumn}).
 *
 * This setting provider is used to enable the legacy classification
 *
 * @author Steve Ebersole
 */
public class ImplicitListAsBagProvider implements SettingProvider.Provider<CollectionClassification> {
	@Override
	public CollectionClassification getSetting() {
		return CollectionClassification.BAG;
	}
}
