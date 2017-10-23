/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.superclass.auditAtMethodSuperclassLevel.auditMethodSubclass;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.hibernate.envers.exception.NotAuditedException;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.integration.superclass.auditAtMethodSuperclassLevel.AuditedMethodMappedSuperclass;
import org.hibernate.envers.test.integration.superclass.auditAtMethodSuperclassLevel.NotAuditedSubclassEntity;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 * @author Hern&aacut;n Chanfreau
 */
public class MappedSubclassingMethodAuditedTest extends BaseEnversJPAFunctionalTestCase {
	private Integer id2_1;
	private Integer id1_1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				AuditedMethodMappedSuperclass.class,
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

		//this property is not audited on superclass
		assert (rev1.getOtherStr() == null);
		assert (rev2.getOtherStr() == null);

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
