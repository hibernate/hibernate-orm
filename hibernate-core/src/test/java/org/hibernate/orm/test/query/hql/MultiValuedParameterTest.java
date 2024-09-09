/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.hql;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.boot.MetadataSources;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.domain.contacts.Contact;
import org.hibernate.testing.orm.domain.contacts.ContactsDomainModel;
import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.hibernate.testing.orm.junit.Jira;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Andrea Boriero
 */
public class MultiValuedParameterTest extends BaseSessionFactoryFunctionalTest {

	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		ContactsDomainModel.applyContactsModel( metadataSources );
		metadataSources.addAnnotatedClass( EntityWithNumericId.class );
	}

	@BeforeAll
	public void prepareData() {
		inTransaction(
				session -> {
					for ( int i = 0; i < 20; i++ ) {
						Contact p1 = new Contact(
								i,
								new Contact.Name( "first[" + i + "]", "last[" + i + "]" ),
								Contact.Gender.MALE,
								LocalDate.now()
						);
						session.persist( p1 );
						if ( i < 3 ) {
							session.persist( new EntityWithNumericId( BigInteger.valueOf( i ) ) );
						}
					}
				}
		);
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-10893" )
	public void testParameterListIn() {
		inTransaction(
				session -> {
					Collection<Integer> ids = new ArrayList<>();
					Query q = session.createQuery( "select id from Contact where id in (:ids) order by id" );
					for ( int i = 0; i < 10; i++ ) {
						ids.add( i );
					}
					q.setParameterList( "ids", ids );
					q.list();

					ids.clear();
					for ( int i = 10; i < 20; i++ ) {
						ids.add( i );
					}
					// reuse the same query, but set new collection parameter
					q.setParameterList( "ids", ids );
					List<Long> foundIds = q.list();

					assertThat( "Wrong number of results", foundIds.size(), is( ids.size() ) );
					assertThat( foundIds, is( ids ) );
				}
		);
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-17492" )
	public void test() {
		inTransaction( session -> {
			final List<BigInteger> ids = List.of( BigInteger.ZERO, BigInteger.ONE, BigInteger.TWO );
			final List<BigInteger> resultList = session.createQuery(
					"select id from EntityWithNumericId e WHERE e.id in (:ids)",
					BigInteger.class
			).setParameter( "ids", ids ).getResultList();
			assertThat( resultList.size(), is( 3 ) );
			assertThat( resultList, is( ids ) );
		} );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-18575" )
	void testMultiValuedBigDecimals() {
		inTransaction( session -> {
			assertEquals(
					1,
					session.createQuery("SELECT 1 WHERE :value IN (:list)", Integer.class)
							.setParameter( "value", BigDecimal.valueOf( 2.0))
							.setParameter("list", List.of(BigDecimal.valueOf(2.0), BigDecimal.valueOf(3.0)))
							.getSingleResult()
			);
		});
	}

	@AfterAll
	public void cleanupData() {
		inTransaction( session -> {
			session.createMutationQuery( "delete Contact" ).executeUpdate();
			session.createMutationQuery( "delete EntityWithNumericId" ).executeUpdate();
		} );
	}

	@Entity( name = "EntityWithNumericId" )
	public static class EntityWithNumericId {
		@Id
		private BigInteger id;

		public EntityWithNumericId() {
		}

		public EntityWithNumericId(BigInteger id) {
			this.id = id;
		}
	}
}
