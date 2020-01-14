/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.modifiedflags;

import java.util.List;
import javax.persistence.EntityManager;

import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.integration.auditReader.AuditedTestEntity;
import org.hibernate.envers.test.integration.auditReader.NotAuditedTestEntity;

import org.hibernate.envers.test.integration.modifiedflags.entities.EnumEntity;
import org.hibernate.envers.test.integration.modifiedflags.entities.EnumOption;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * A test which checks the correct behavior of AuditReader.isEntityClassAudited(Class entityClass).
 *
 * @author Hernan Chanfreau
 * @author Michal Skowronek (mskowr at o2 dot pl)
 */
public class HasChangedAPITest extends AbstractModifiedFlagsEntityTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {AuditedTestEntity.class, NotAuditedTestEntity.class, EnumEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();
		em.getTransaction().begin();
		AuditedTestEntity ent1 = new AuditedTestEntity( 1, "str1" );
		NotAuditedTestEntity ent2 = new NotAuditedTestEntity( 1, "str1" );
		EnumEntity ent3 = new EnumEntity( 1, EnumOption.A );


		em.persist( ent1 );
		em.persist( ent2 );
		em.persist( ent3 );
		em.getTransaction().commit();

		em.getTransaction().begin();

		ent1 = em.find( AuditedTestEntity.class, 1 );
		ent2 = em.find( NotAuditedTestEntity.class, 1 );
		ent3 = em.find( EnumEntity.class, 1 );
		ent1.setStr1( "str2" );
		ent2.setStr1( "str2" );
		ent3.setOption( EnumOption.B );
		em.getTransaction().commit();
	}

	@Test
	public void testHasChangedHasNotChangedCriteria() throws Exception {
		List list = getAuditReader().createQuery().forRevisionsOfEntity( AuditedTestEntity.class, true, true )
				.add( AuditEntity.property( "str1" ).hasChanged() ).getResultList();
		assertEquals( 2, list.size() );
		assertEquals( "str1", ((AuditedTestEntity) list.get( 0 )).getStr1() );
		assertEquals( "str2", ((AuditedTestEntity) list.get( 1 )).getStr1() );

		list = getAuditReader().createQuery().forRevisionsOfEntity( AuditedTestEntity.class, true, true )
				.add( AuditEntity.property( "str1" ).hasNotChanged() ).getResultList();
		assertTrue( list.isEmpty() );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13770")
	public void testHasChangedHasNotChangedEnum() {
		List list = getAuditReader().createQuery().forRevisionsOfEntity( EnumEntity.class, true, true )
				.add( AuditEntity.property( "option" ).hasChanged() ).getResultList();
		assertEquals( 2, list.size() );
		assertEquals( EnumOption.A, ( (EnumEntity) list.get( 0 ) ).getOption() );
		assertEquals( EnumOption.B, ( (EnumEntity) list.get( 1 ) ).getOption() );

		list = getAuditReader().createQuery().forRevisionsOfEntity( EnumEntity.class, true, true )
				.add( AuditEntity.property( "option" ).hasNotChanged() ).getResultList();
		assertTrue( list.isEmpty() );
	}

}
