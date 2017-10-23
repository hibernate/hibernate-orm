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

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.junit.Test;

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