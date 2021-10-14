/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

/**
 * @author Steve Ebersole
 */
public @interface SettingProvider {
	interface Provider<S> {
		S getSetting();
	}

	String settingName();
	Class<? extends Provider<?>> provider();
}
