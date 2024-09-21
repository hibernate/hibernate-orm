/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

import org.hibernate.metamodel.CollectionClassification;

/**
 * @author Christian Beikov
 */
public class ImplicitListAsListProvider implements SettingProvider.Provider<CollectionClassification> {
	@Override
	public CollectionClassification getSetting() {
		return CollectionClassification.LIST;
	}
}
