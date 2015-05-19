/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.data;

import java.util.Arrays;
import java.util.Map;
import javax.persistence.EntityManager;

import org.hibernate.dialect.PostgreSQL82Dialect;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@RequiresDialectFeature(DialectChecks.SupportsExpectedLobUsagePattern.class)
public class Lobs extends BaseEnversJPAFunctionalTestCase {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {LobTestEntity.class};
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
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		LobTestEntity lte = new LobTestEntity( "abc", new byte[] {0, 1, 2}, new char[] {'x', 'y', 'z'} );
		em.persist( lte );
		id1 = lte.getId();
		em.getTransaction().commit();

		em.getTransaction().begin();
		lte = em.find( LobTestEntity.class, id1 );
		lte.setStringLob( "def" );
		lte.setByteLob( new byte[] {3, 4, 5} );
		lte.setCharLob( new char[] {'h', 'i', 'j'} );
		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( LobTestEntity.class, id1 ) );
	}

	@Test
	public void testHistoryOfId1() {
		LobTestEntity ver1 = new LobTestEntity( id1, "abc", new byte[] {0, 1, 2}, new char[] {'x', 'y', 'z'} );
		LobTestEntity ver2 = new LobTestEntity( id1, "def", new byte[] {3, 4, 5}, new char[] {'h', 'i', 'j'} );

		assert getAuditReader().find( LobTestEntity.class, id1, 1 ).equals( ver1 );
		assert getAuditReader().find( LobTestEntity.class, id1, 2 ).equals( ver2 );
	}
}