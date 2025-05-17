/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.component.cascading.toone;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@ServiceRegistry
@DomainModel(xmlMappings = "org/hibernate/orm/test/component/cascading/toone/Mappings.xml")
@SessionFactory
public class CascadeToComponentAssociationTest {
	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testMerging(SessionFactoryScope factoryScope) {
		// step1, we create a document with owner
		factoryScope.inTransaction( (session) -> {
			User user = new User( 1, null );
			Document document = new Document( 1, null, null );
			document.setOwner( user );
			session.persist( document );
		} );

		// step2, we verify that the document has owner and that owner has no personal-info; then we detach
		final Document detached = factoryScope.fromTransaction( (session) -> {
			final Document document = session.find( Document.class, 1 );
			assertNotNull( document.getOwner() );
			assertNull( document.getOwner().getPersonalInfo() );
			return document;
		} );

		// step3, try to specify the personal-info during detachment
		Address addr = new Address( 1 );
		addr.setStreet1( "123 6th St" );
		addr.setCity( "Austin" );
		addr.setState( "TX" );
		detached.getOwner().setPersonalInfo( new PersonalInfo( addr ) );

		// step4 we merge the document
		factoryScope.inTransaction( (session) -> {
			session.merge( detached );
		} );

		// step5, final test
		factoryScope.inTransaction( (session) -> {
			final Document document = session.find( Document.class, 1 );
			assertNotNull( document.getOwner() );
			assertNotNull( document.getOwner().getPersonalInfo() );
			assertNotNull( document.getOwner().getPersonalInfo().getHomeAddress() );
		} );
	}
}
