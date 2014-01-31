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
package org.hibernate.test.multitenancy.schema;

import org.junit.Assert;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.env.ConnectionProviderBuilder;

/**
 * SessionFactory has to use the {@link CurrentTenantIdentifierResolver} when
 * {@link SessionFactory#openSession()} is called.
 *
 * @author Stefan Schulze
 * @author Steve Ebersole
 */
@TestForIssue(jiraKey = "HHH-7306")
@RequiresDialectFeature( value = ConnectionProviderBuilder.class )
public class CurrentTenantResolverMultiTenancyTest extends SchemaBasedMultiTenancyTest {
//
//	private TestCurrentTenantIdentifierResolver currentTenantResolver = new TestCurrentTenantIdentifierResolver();
//
//
//	@Override
//	protected Configuration buildConfiguration() {
//		Configuration cfg = super.buildConfiguration();
//		cfg.setCurrentTenantIdentifierResolver( currentTenantResolver );
//		return cfg;
//	}
//
//	@Override
//	protected Session getNewSession(String tenant) {
//		currentTenantResolver.currentTenantIdentifier = tenant;
//		Session session = sessionFactory.openSession();
//		Assert.assertEquals( tenant, session.getTenantIdentifier() );
//		return session;
//	}
//
//
//	private static class TestCurrentTenantIdentifierResolver implements CurrentTenantIdentifierResolver {
//		private String currentTenantIdentifier;
//
//		@Override
//		public boolean validateExistingCurrentSessions() {
//			return false;
//		}
//
//		@Override
//		public String resolveCurrentTenantIdentifier() {
//			return currentTenantIdentifier;
//		}
//	}
}
