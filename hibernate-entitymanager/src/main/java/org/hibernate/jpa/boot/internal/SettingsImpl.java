/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
