/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.component.cascading.toone;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/component/cascading/toone/Mappings.hbm.xml/"
)
@SessionFactory
public class CascadeToComponentAssociationTest {

	@Test
	public void testMerging(SessionFactoryScope scope) {
		// step1, we create a document with owner
		Document doc = new Document();
		scope.inTransaction( session -> {
					User user = new User();
					doc.setOwner( user );
					session.persist( doc );
				}
		);

		// step2, we verify that the document has owner and that owner has no personal-info; then we detach
		Document d = scope.fromTransaction( session -> {
					Document document = session.find( Document.class, doc.getId() );
					assertThat( document.getOwner() ).isNotNull();
					assertThat( document.getOwner().getPersonalInfo() ).isNull();
					return document;
				}
		);

		// step3, try to specify the personal-info during detachment
		Address addr = new Address();
		addr.setStreet1( "123 6th St" );
		addr.setCity( "Austin" );
		addr.setState( "TX" );
		d.getOwner().setPersonalInfo( new PersonalInfo( addr ) );

		// step4 we merge the document
		scope.inTransaction( session ->
				session.merge( d )
		);

		// step5, final test
		scope.inTransaction(
				session -> {
					Document document = session.find( Document.class, d.getId() );
					assertThat( document.getOwner() ).isNotNull();
					assertThat( document.getOwner().getPersonalInfo() ).isNotNull();
					assertThat( document.getOwner().getPersonalInfo().getHomeAddress() ).isNotNull();
				}
		);
	}
}
