/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.ejb3configuration;

import org.hibernate.orm.test.jpa.NotSerializableClass;

import org.hibernate.testing.orm.junit.SettingProvider;

/**
 * Add a non-serializable object to the EMF to ensure that the EM can be serialized even if its EMF is not serializable.
 * This will ensure that the fix for HHH-6897 doesn't regress
 */
public class NotSerializableClassSettingProvider implements SettingProvider.Provider<NotSerializableClass> {
	@Override
	public NotSerializableClass getSetting() {
		return new NotSerializableClass();
	}
}
