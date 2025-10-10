/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.fetch;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import org.hibernate.Hibernate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author Emmanuel Bernard
 */
@DomainModel(
		annotatedClasses = {
				Person.class,
				Stay.class,
				Branch.class,
				Leaf.class
		}
)
@SessionFactory
public class FetchingTest {

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testLazy(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Person p = new Person( "Gavin", "King", "JBoss Inc" );
					Stay stay = new Stay( p, new Date(), new Date(), "A380", "Blah", "Blah" );
					p.addStay( stay );
					session.persist( p );
					session.getTransaction().commit();
					session.clear();
					session.beginTransaction();
					p = session.createQuery( "from Person p where p.firstName = :name", Person.class )
							.setParameter( "name", "Gavin" ).uniqueResult();
					assertThat( Hibernate.isInitialized( p.getStays() ) ).isFalse();
					session.remove( p );
				}
		);
	}

	@Test
	public void testHibernateFetchingLazy(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Person p = new Person( "Gavin", "King", "JBoss Inc" );
					Stay stay = new Stay( null, new Date(), new Date(), "A380", "Blah", "Blah" );
					Stay stay2 = new Stay( null, new Date(), new Date(), "A320", "Blah", "Blah" );
					Stay stay3 = new Stay( null, new Date(), new Date(), "A340", "Blah", "Blah" );
					stay.setOldPerson( p );
					stay2.setVeryOldPerson( p );
					stay3.setVeryOldPerson( p );
					p.addOldStay( stay );
					p.addVeryOldStay( stay2 );
					p.addVeryOldStay( stay3 );
					session.persist( p );
					session.getTransaction().commit();
					session.clear();
					session.beginTransaction();
					p = session.createQuery( "from Person p where p.firstName = :name", Person.class )
							.setParameter( "name", "Gavin" ).uniqueResult();
					assertThat( Hibernate.isInitialized( p.getOldStays() ) ).isFalse();
					assertThat( p.getOldStays().size() ).isEqualTo( 1 );
					assertThat( Hibernate.isInitialized( p.getOldStays() ) ).isTrue();
					session.clear();
					stay = session.find( Stay.class, stay.getId() );
					assertThat( !Hibernate.isInitialized( stay.getOldPerson() ) ).isTrue();
					session.clear();
					stay3 = session.find( Stay.class, stay3.getId() );
					assertThat( Hibernate.isInitialized( stay3.getVeryOldPerson() ) ).isTrue();
					session.remove( stay3.getVeryOldPerson() );
				}
		);
	}

	@Test
	public void testOneToManyFetchEager(SessionFactoryScope scope) {
		Branch b = new Branch();
		scope.inTransaction(
				session -> {
					session.persist( b );
					session.flush();
					Leaf l = new Leaf();
					l.setBranch( b );
					session.persist( l );
					session.flush();

					session.clear();

					CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
					CriteriaQuery<Branch> criteria = criteriaBuilder.createQuery( Branch.class );
					criteria.from( Branch.class );
					session.createQuery( criteria ).list();
				}
		);
	}
}
