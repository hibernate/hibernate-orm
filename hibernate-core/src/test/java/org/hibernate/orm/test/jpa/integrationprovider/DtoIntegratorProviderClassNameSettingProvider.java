/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.integrationprovider;

import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;

import org.hibernate.testing.orm.jpa.NonStringValueSettingProvider;

/**
 * @author Jan Schatteman
 */
public class DtoIntegratorProviderClassNameSettingProvider extends NonStringValueSettingProvider {
	@Override
	public String getKey() {
		return EntityManagerFactoryBuilderImpl.INTEGRATOR_PROVIDER;
	}

	@Override
	public Object getValue() {
		return DtoIntegratorProvider.class.getName();
	}
}
