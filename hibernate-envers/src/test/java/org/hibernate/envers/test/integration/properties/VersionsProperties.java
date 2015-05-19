/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.properties;

import java.util.Arrays;
import java.util.Map;
import javax.persistence.EntityManager;

import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class VersionsProperties extends BaseEnversJPAFunctionalTestCase {
	private Integer id1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {PropertiesTestEntity.class};
	}

	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		options.put( EnversSettings.AUDIT_TABLE_PREFIX, "VP_" );
		options.put( EnversSettings.AUDIT_TABLE_SUFFIX, "_VS" );
		options.put( EnversSettings.REVISION_FIELD_NAME, "ver_rev" );
		options.put( EnversSettings.REVISION_TYPE_FIELD_NAME, "ver_rev_type" );
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		PropertiesTestEntity pte = new PropertiesTestEntity( "x" );
		em.persist( pte );
		id1 = pte.getId();
		em.getTransaction().commit();

		em.getTransaction().begin();
		pte = em.find( PropertiesTestEntity.class, id1 );
		pte.setStr( "y" );
		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( PropertiesTestEntity.class, id1 ) );
	}

	@Test
	public void testHistoryOfId1() {
		PropertiesTestEntity ver1 = new PropertiesTestEntity( id1, "x" );
		PropertiesTestEntity ver2 = new PropertiesTestEntity( id1, "y" );

		assert getAuditReader().find( PropertiesTestEntity.class, id1, 1 ).equals( ver1 );
		assert getAuditReader().find( PropertiesTestEntity.class, id1, 2 ).equals( ver2 );
	}
}