/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.xml.ejb3;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.JiraKeyGroup;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@JiraKeyGroup(value = {
		@JiraKey(value = "HHH-6271"),
		@JiraKey(value = "HHH-14529")
})
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/annotations/xml/ejb3/orm2.xml"
)
@SessionFactory
public class OrmVersion1SupportedTest {

	@Test
	public void testOrm1Support(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Light light = new Light();
					light.name = "the light at the end of the tunnel";
					session.persist( light );
					session.flush();
					session.clear();

					assertThat( session.getNamedQuery( "find.the.light" ).list().size() ).isEqualTo( 1 );
				}
		);
	}
}
