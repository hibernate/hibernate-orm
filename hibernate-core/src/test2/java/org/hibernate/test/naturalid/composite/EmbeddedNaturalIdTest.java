/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.naturalid.composite;

import org.hibernate.Session;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-11255")
public class EmbeddedNaturalIdTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				PostalCarrier.class
		};
	}

	@Test
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			PostalCarrier postalCarrier = new PostalCarrier();
			postalCarrier.setId( 1L );
			postalCarrier.setPostalCode( new PostalCode() );
			postalCarrier.getPostalCode().setCode( "ABC123" );
			postalCarrier.getPostalCode().setCountry( "US" );

			entityManager.persist( postalCarrier );
		} );
		doInJPA( this::entityManagerFactory, entityManager -> {
			PostalCarrier postalCarrier = entityManager.unwrap( Session.class )
					.byNaturalId( PostalCarrier.class )
					.using( "postalCode", new PostalCode( "ABC123", "US" ) )
					.load();
			assertEquals( Long.valueOf( 1L ), postalCarrier.getId() );
		} );
		doInJPA( this::entityManagerFactory, entityManager -> {
			PostalCarrier postalCarrier = entityManager.unwrap( Session.class )
					.bySimpleNaturalId( PostalCarrier.class )
					.load( new PostalCode( "ABC123", "US" ) );
			assertEquals( Long.valueOf( 1L ), postalCarrier.getId() );
		} );
	}
}
