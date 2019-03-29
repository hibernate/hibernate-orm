/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.query.hql;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.boot.MetadataSources;
import org.hibernate.query.Query;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.domain.contacts.Contact;
import org.hibernate.testing.orm.domain.contacts.ContactsDomainModel;
import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@SuppressWarnings("WeakerAccess")
@TestForIssue(jiraKey = "HHH-10893")
public class ParameterCollectionTest extends BaseSessionFactoryFunctionalTest {

	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		ContactsDomainModel.applyContactsModel( metadataSources );
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
						session.save( p1 );
					}
				}
		);
	}

	@Test
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

	@AfterAll
	public void cleanupData() {
		inTransaction( session -> session.createQuery( "delete Contact" ).executeUpdate() );
	}
}
