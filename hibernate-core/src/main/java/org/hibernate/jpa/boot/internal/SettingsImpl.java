/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.boot.internal;

import javax.persistence.spi.PersistenceUnitTransactionType;

import org.hibernate.Interceptor;
import org.hibernate.jpa.boot.spi.Settings;

/**
 * @author Steve Ebersole
 */
public class SettingsImpl implements Settings {
	private PersistenceUnitTransactionType transactionType;
	private boolean releaseResourcesOnCloseEnabled;
	private Class<? extends Interceptor> sessionInterceptorClass;

	@Override
	public PersistenceUnitTransactionType getTransactionType() {
		return transactionType;
	}

	public SettingsImpl setTransactionType(PersistenceUnitTransactionType transactionType) {
		this.transactionType = transactionType;
		return this;
	}

	@Override
	public boolean isReleaseResourcesOnCloseEnabled() {
		return releaseResourcesOnCloseEnabled;
	}

	public SettingsImpl setReleaseResourcesOnCloseEnabled(boolean releaseResourcesOnCloseEnabled) {
		this.releaseResourcesOnCloseEnabled = releaseResourcesOnCloseEnabled;
		return this;
	}

	@Override
	public Class<? extends Interceptor> getSessionInterceptorClass() {
		return sessionInterceptorClass;
	}

	public SettingsImpl setSessionInterceptorClass(Class<? extends Interceptor> sessionInterceptorClass) {
		this.sessionInterceptorClass = sessionInterceptorClass;
		return this;
	}
}
