/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.flush;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.hibernate.FlushMode;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrTestEntity;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class DoubleFlushAddDel extends AbstractFlushTest {
	private Integer id;

	public FlushMode getFlushMode() {
		return FlushMode.MANUAL;
	}

	@Test
	@Priority(10)
	public void initData() {
		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();

		StrTestEntity fe = new StrTestEntity( "x" );
		em.persist( fe );

		em.flush();

		em.remove( em.find( StrTestEntity.class, fe.getId() ) );

		em.flush();

		em.getTransaction().commit();

		//

		id = fe.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList().equals( getAuditReader().getRevisions( StrTestEntity.class, id ) );
	}

	@Test
	public void testHistoryOfId() {
		assert getAuditReader().find( StrTestEntity.class, id, 1 ) == null;
	}
}