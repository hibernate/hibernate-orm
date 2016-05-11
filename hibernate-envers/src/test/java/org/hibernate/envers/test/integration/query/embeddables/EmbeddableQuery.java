/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.query.embeddables;

import java.util.List;

import javax.persistence.EntityManager;

import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;

/**
 * Test which supports using {@link AuditEntity} to test equality/inequality
 * between embeddable components.
 *
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-9178")
public class EmbeddableQuery extends BaseEnversJPAFunctionalTestCase {
	private Integer personId;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Person.class, NameInfo.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getOrCreateEntityManager();
		try {
			// Revision 1
			em.getTransaction().begin();
			NameInfo ni = new NameInfo( "John", "Doe" );
			Person person1 = new Person( "JDOE", ni );
			em.persist( person1 );
			em.getTransaction().commit();

			// Revision 2
			em.getTransaction().begin();
			person1 = em.find( Person.class, person1.getId() );
			person1.getNameInfo().setFirstName( "Jane" );
			em.merge( person1 );
			em.getTransaction().commit();

			// Revision 3
			em.getTransaction().begin();
			person1 = em.find( Person.class, person1.getId() );
			person1.setName( "JDOE2" );
			em.merge( person1 );
			em.getTransaction().commit();

			personId = person1.getId();
		}
		finally {
			em.close();
		}
	}

	@Test
	public void testRevisionCounts() {
		assertEquals( 3, getAuditReader().getRevisions( Person.class, personId ).size() );
	}

	@Test
	public void testAuditQueryUsingEmbeddableEquals() {
		final NameInfo nameInfo = new NameInfo( "John", "Doe" );
		final AuditQuery query = getAuditReader().createQuery().forEntitiesAtRevision( Person.class, 1 );
		query.add( AuditEntity.property( "nameInfo" ).eq( nameInfo ) );
		List<?> results = query.getResultList();
		assertEquals( 1, results.size() );
		final Person person = (Person) results.get( 0 );
		assertEquals( nameInfo, person.getNameInfo() );
	}

	@Test
	public void testAuditQueryUsingEmbeddableNotEquals() {
		final NameInfo nameInfo = new NameInfo( "Jane", "Doe" );
		final AuditQuery query = getAuditReader().createQuery().forEntitiesAtRevision( Person.class, 1 );
		query.add( AuditEntity.property( "nameInfo" ).ne( nameInfo ) );
		assertEquals( 0, query.getResultList().size() );
	}

	@Test
	public void testAuditQueryUsingEmbeddableNonEqualityCheck() {
		try {
			final NameInfo nameInfo = new NameInfo( "John", "Doe" );
			final AuditQuery query = getAuditReader().createQuery().forEntitiesAtRevision( Person.class, 1 );
			query.add( AuditEntity.property( "nameInfo" ).le( nameInfo ) );
		}
		catch ( Exception ex ) {
			assertTyping( AuditException.class, ex );
		}
	}
}
