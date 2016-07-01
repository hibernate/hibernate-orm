/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.reventity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.persistence.EntityManager;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrTestEntity;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class LongRevNumber extends BaseEnversJPAFunctionalTestCase {
	private Integer id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {StrTestEntity.class, LongRevNumberRevEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() throws InterruptedException {
		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		StrTestEntity te = new StrTestEntity( "x" );
		em.persist( te );
		id = te.getId();
		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();
		te = em.find( StrTestEntity.class, id );
		te.setStr( "y" );
		em.getTransaction().commit();
	}

	@Test
	public void testFindRevision() {
		AuditReader vr = getAuditReader();

		assert vr.findRevision( LongRevNumberRevEntity.class, 1l ).getCustomId() == 1l;
		assert vr.findRevision( LongRevNumberRevEntity.class, 2l ).getCustomId() == 2l;
	}

	@Test
	public void testFindRevisions() {
		AuditReader vr = getAuditReader();

		Set<Number> revNumbers = new HashSet<Number>();
		revNumbers.add( 1l );
		revNumbers.add( 2l );

		Map<Number, LongRevNumberRevEntity> revisionMap = vr.findRevisions( LongRevNumberRevEntity.class, revNumbers );
		assert (revisionMap.size() == 2);
		assert (revisionMap.get( 1l ).equals( vr.findRevision( LongRevNumberRevEntity.class, 1l ) ));
		assert (revisionMap.get( 2l ).equals( vr.findRevision( LongRevNumberRevEntity.class, 2l ) ));
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1l, 2l ).equals( getAuditReader().getRevisions( StrTestEntity.class, id ) );
	}

	@Test
	public void testHistoryOfId1() {
		StrTestEntity ver1 = new StrTestEntity( "x", id );
		StrTestEntity ver2 = new StrTestEntity( "y", id );

		assert getAuditReader().find( StrTestEntity.class, id, 1l ).equals( ver1 );
		assert getAuditReader().find( StrTestEntity.class, id, 2l ).equals( ver2 );
	}
}
