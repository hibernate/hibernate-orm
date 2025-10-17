/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.naturalid.composite;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@DomainModel( annotatedClasses = PostalCarrier.class )
@SessionFactory
@JiraKey(value = "HHH-11255")
public class EmbeddedNaturalIdTest {

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					PostalCarrier postalCarrier = new PostalCarrier();
					postalCarrier.setId( 1L );
					postalCarrier.setPostalCode( new PostalCode() );
					postalCarrier.getPostalCode().setCode( "ABC123" );
					postalCarrier.getPostalCode().setCountry( "US" );

					session.persist( postalCarrier );
				}
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final PostalCarrier postalCarrier = session
							.byNaturalId( PostalCarrier.class )
							.using( "postalCode", new PostalCode( "ABC123", "US" ) )
							.load();
					assertEquals( Long.valueOf( 1L ), postalCarrier.getId() );
				}
		);
		scope.inTransaction(
				(session) -> {
					final PostalCarrier postalCarrier = session
							.bySimpleNaturalId( PostalCarrier.class )
							.load(new PostalCode( "ABC123", "US" ) );
					assertEquals( Long.valueOf( 1L ), postalCarrier.getId() );
				}
		);
	}
}
