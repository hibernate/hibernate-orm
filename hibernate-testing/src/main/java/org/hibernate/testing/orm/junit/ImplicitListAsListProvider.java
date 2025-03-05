/*
 * SPDX-License-Identifier: Apache-2.0
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
