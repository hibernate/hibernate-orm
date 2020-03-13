/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.query;

import java.util.Set;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.LockModeType;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.TypedQuery;

import org.hibernate.graph.GraphSemantic;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Nathan Xu
 */
@TestForIssue( jiraKey = "HHH-13882" )
public class QueryWithEntityGraphOnCollectionAndLockmodeTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Person.class };
	}

	@Test
	public void test() {
		inTransaction( session -> {
			TypedQuery<Person> query = session.createQuery( "select p from Person p", Person.class );
			query.setLockMode( LockModeType.READ );
			query.setHint( GraphSemantic.FETCH.getJpaHintName(), session.createEntityGraph( "withNicknames" ) );
			query.getResultList(); // NPE thrown here in HHH-13882
		} );
	}

	@Entity( name = "Person" )
	@NamedEntityGraph( name = "withNicknames", attributeNodes = @NamedAttributeNode( "nicknames" ) )
	public static class Person {
		@Id
		private long id;

		@ElementCollection
		private Set<String> nicknames;
	}
}
