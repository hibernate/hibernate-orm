/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.models.bind;

import org.hibernate.boot.model.naming.PhysicalNamingStrategy;

import org.hibernate.testing.orm.junit.SettingProvider;


/**
 * @author Steve Ebersole
 */
public class CustomNamingStrategyProvider implements SettingProvider.Provider<PhysicalNamingStrategy> {
	@Override
	public PhysicalNamingStrategy getSetting() {
		return new CustomNamingStrategy();
	}
}
