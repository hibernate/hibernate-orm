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
package org.hibernate.testing.jta;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;

/**
 * @author Steve Ebersole
 */
public final class TestingJtaBootstrap {
	public static final TestingJtaBootstrap INSTANCE = new TestingJtaBootstrap();

	@SuppressWarnings("unchecked")
	public static void prepare(Map configValues) {
		configValues.put( AvailableSettings.JTA_PLATFORM, TestingJtaPlatformImpl.INSTANCE );
		configValues.put( Environment.CONNECTION_PROVIDER, JtaAwareConnectionProviderImpl.class.getName() );
		configValues.put( "javax.persistence.transactionType", "JTA" );
	}

	private TestingJtaBootstrap() {
	}
}
