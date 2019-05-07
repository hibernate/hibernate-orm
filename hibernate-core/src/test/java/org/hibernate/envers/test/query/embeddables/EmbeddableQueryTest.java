/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.query.embeddables;

import java.util.List;

import org.hibernate.envers.exception.AuditException;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.query.embeddables.NameInfo;
import org.hibernate.envers.test.support.domains.query.embeddables.Person;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * Test which supports using {@link AuditEntity} to test equality/inequality
 * between embeddable components.
 *
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-9178")
public class EmbeddableQueryTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer personId;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Person.class, NameInfo.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inTransactions(
				// Revision 1
				entityManager -> {
					NameInfo ni = new NameInfo( "John", "Doe" );
					Person person1 = new Person( "JDOE", ni );
					entityManager.persist( person1 );

					this.personId = person1.getId();
				},

				// Revision 2
				entityManager -> {
					final Person person1 = entityManager.find( Person.class, personId );
					person1.getNameInfo().setFirstName( "Jane" );
					entityManager.merge( person1 );
				},

				// Revision 3
				entityManager -> {
					final Person person1 = entityManager.find( Person.class, personId );
					person1.setName( "JDOE2" );
					entityManager.merge( person1 );
				}
		);
	}

	@DynamicTest
	public void testRevisionCounts() {
		assertThat( getAuditReader().getRevisions( Person.class, personId ), contains( 1, 2, 3 ) );
	}

	@DynamicTest
	@SuppressWarnings("unchecked")
	public void testAuditQueryUsingEmbeddableEquals() {
		final NameInfo nameInfo = new NameInfo( "John", "Doe" );
		final AuditQuery query = getAuditReader().createQuery().forEntitiesAtRevision( Person.class, 1 );
		query.add( AuditEntity.property( "nameInfo" ).eq( nameInfo ) );
		List<Person> results = query.getResultList();
		assertThat( results, CollectionMatchers.hasSize( 1 ) );
		assertThat( results.get( 0 ).getNameInfo(), equalTo( nameInfo ) );
	}

	@DynamicTest
	public void testAuditQueryUsingEmbeddableNotEquals() {
		final NameInfo nameInfo = new NameInfo( "Jane", "Doe" );
		final AuditQuery query = getAuditReader().createQuery().forEntitiesAtRevision( Person.class, 1 );
		query.add( AuditEntity.property( "nameInfo" ).ne( nameInfo ) );
		assertThat( query.getResultList(), CollectionMatchers.isEmpty() );
	}

	@DynamicTest(expected = AuditException.class)
	public void testAuditQueryUsingEmbeddableNonEqualityCheck() {
		final NameInfo nameInfo = new NameInfo( "John", "Doe" );
		final AuditQuery query = getAuditReader().createQuery().forEntitiesAtRevision( Person.class, 1 );
		query.add( AuditEntity.property( "nameInfo" ).le( nameInfo ) );
		query.getResultList();
	}
}
