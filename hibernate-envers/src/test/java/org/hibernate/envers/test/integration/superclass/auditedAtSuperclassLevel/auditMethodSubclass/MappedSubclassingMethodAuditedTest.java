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
package org.hibernate.envers.test.integration.superclass.auditedAtSuperclassLevel.auditMethodSubclass;

import javax.persistence.EntityManager;
import java.util.Arrays;

import org.hibernate.envers.exception.NotAuditedException;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.integration.superclass.auditedAtSuperclassLevel.AuditedAllMappedSuperclass;
import org.hibernate.envers.test.integration.superclass.auditedAtSuperclassLevel.NotAuditedSubclassEntity;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Hern&aacut;n Chanfreau
 */
@FailureExpectedWithNewMetamodel( message = "@MappedSuperclass not supported with new metamodel by envers yet.")
public class MappedSubclassingMethodAuditedTest extends BaseEnversJPAFunctionalTestCase {
	private Integer id2_1;
	private Integer id1_1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				AuditedAllMappedSuperclass.class,
				AuditedMethodSubclassEntity.class,
				NotAuditedSubclassEntity.class
		};
	}

	@Test
	@Priority(10)
	public void initData() {
		// Revision 1
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		NotAuditedSubclassEntity nas = new NotAuditedSubclassEntity( "nae", "super str", "not audited str" );
		em.persist( nas );
		AuditedMethodSubclassEntity ae = new AuditedMethodSubclassEntity( "ae", "super str", "audited str" );
		em.persist( ae );
		id1_1 = ae.getId();
		id2_1 = nas.getId();
		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();
		ae = em.find( AuditedMethodSubclassEntity.class, id1_1 );
		ae.setStr( "ae new" );
		ae.setSubAuditedStr( "audited str new" );
		nas = em.find( NotAuditedSubclassEntity.class, id2_1 );
		nas.setStr( "nae new" );
		nas.setNotAuditedStr( "not aud str new" );
		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsCountsForAudited() {
		assert Arrays.asList( 1, 2 ).equals(
				getAuditReader().getRevisions( AuditedMethodSubclassEntity.class, id1_1 )
		);
	}

	@Test(expected = NotAuditedException.class)
	public void testRevisionsCountsForNotAudited() {
		try {
			getAuditReader().getRevisions( NotAuditedSubclassEntity.class, id2_1 );
			assert (false);
		}
		catch (NotAuditedException nae) {
			throw nae;
		}
	}


	@Test
	public void testHistoryOfAudited() {
		AuditedMethodSubclassEntity ver1 = new AuditedMethodSubclassEntity( id1_1, "ae", "super str", "audited str" );
		AuditedMethodSubclassEntity ver2 = new AuditedMethodSubclassEntity(
				id1_1,
				"ae new",
				"super str",
				"audited str new"
		);

		AuditedMethodSubclassEntity rev1 = getAuditReader().find( AuditedMethodSubclassEntity.class, id1_1, 1 );
		AuditedMethodSubclassEntity rev2 = getAuditReader().find( AuditedMethodSubclassEntity.class, id1_1, 2 );

		assert (rev1.getOtherStr() != null);
		assert (rev2.getOtherStr() != null);

		assert rev1.equals( ver1 );
		assert rev2.equals( ver2 );
	}

	@Test(expected = NotAuditedException.class)
	public void testHistoryOfNotAudited() {
		try {
			getAuditReader().find( NotAuditedSubclassEntity.class, id2_1, 1 );
			assert (false);
		}
		catch (NotAuditedException nae) {
			throw nae;
		}
	}

}
