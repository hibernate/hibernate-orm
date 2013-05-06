/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
package org.hibernate.envers.test.integration.data;

import javax.persistence.EntityManager;
import java.util.Arrays;
import java.util.Map;

import org.hibernate.dialect.PostgreSQL82Dialect;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;

import org.junit.Test;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@RequiresDialectFeature(DialectChecks.SupportsExpectedLobUsagePattern.class)
public class LobSerializables extends BaseEnversJPAFunctionalTestCase {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {LobSerializableTestEntity.class};
	}

	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		if ( getDialect() instanceof PostgreSQL82Dialect ) {
			// In PostgreSQL LOBs cannot be used in auto-commit mode.
			options.put( "hibernate.connection.autocommit", "false" );
		}
	}

	@Test
	public void initData() {
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		LobSerializableTestEntity ste = new LobSerializableTestEntity( new SerObject( "d1" ) );
		em.persist( ste );
		id1 = ste.getId();
		em.getTransaction().commit();

		em.getTransaction().begin();
		ste = em.find( LobSerializableTestEntity.class, id1 );
		ste.setObj( new SerObject( "d2" ) );
		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( LobSerializableTestEntity.class, id1 ) );
	}

	@Test
	public void testHistoryOfId1() {
		LobSerializableTestEntity ver1 = new LobSerializableTestEntity( id1, new SerObject( "d1" ) );
		LobSerializableTestEntity ver2 = new LobSerializableTestEntity( id1, new SerObject( "d2" ) );

		assert getAuditReader().find( LobSerializableTestEntity.class, id1, 1 ).equals( ver1 );
		assert getAuditReader().find( LobSerializableTestEntity.class, id1, 2 ).equals( ver2 );
	}
}