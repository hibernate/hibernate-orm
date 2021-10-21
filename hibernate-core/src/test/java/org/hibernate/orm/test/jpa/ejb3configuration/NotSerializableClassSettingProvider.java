/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
