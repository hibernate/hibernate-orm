/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.reflection;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.JiraKeyGroup;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@JiraKeyGroup(value = {
		@JiraKey(value = "HHH-11924"),
		@JiraKey(value = "HHH-14529")
})
@DomainModel(
		annotatedClasses = {
				Company.class
		},
		xmlMappings = "org/hibernate/orm/test/annotations/reflection/element-collection-converter-orm.xml"
)
@SessionFactory
public class ElementCollectionConverterTest {

	@Test
	public void testConverterIsAppliedToElementCollection(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Company company = new Company();
			company.setId( 1L );

			Organization org1 = new Organization();
			org1.setOrganizationId( "ACME" );

			company.getOrganizations().add( org1 );

			session.persist( company );
		} );

		scope.inTransaction( session -> {
			String organizationId = session
					.createNativeQuery( "select organizations from Company_organizations", String.class )
					.getSingleResult();
			assertThat( organizationId ).isEqualTo( "ORG-ACME" );

			Company company = session.find( Company.class, 1L );

			assertThat( company.getOrganizations().size() ).isEqualTo( 1 );
			assertThat( company.getOrganizations().get( 0 ).getOrganizationId() ).isEqualTo( "ACME" );
		} );
	}
}
