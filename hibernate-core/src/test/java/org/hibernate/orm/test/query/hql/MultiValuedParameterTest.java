/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.boot.MetadataSources;

import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.testing.orm.domain.contacts.Contact;
import org.hibernate.testing.orm.domain.contacts.ContactsDomainModel;
import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
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

	@BeforeEach
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
					var q = session.createQuery( "select id from Contact where id in (:ids) order by id" );
					Collection<Integer> ids = new ArrayList<>();
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
	@SkipForDialect(dialectClass = InformixDialect.class,
			reason = "Informix does not like '? in (?,?)'")
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

	@AfterEach
	public void cleanupData() {
		sessionFactoryScope().getSessionFactory().getSchemaManager().truncate();
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
