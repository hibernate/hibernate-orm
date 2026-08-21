/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cascade.circle.delete;


import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@DomainModel(
		annotatedClasses = {
				Person.class,
				Address.class
		}
)
@SessionFactory
@JiraKey(value = "HHH-15218")
public class CascadeDeleteTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Person person = new Person();
					Address currentAddress = new Address( "Localita S. Egidio Gradoli (VT)" );
					person.addCurrentAddress( currentAddress );
					session.persist( person );
				}
		);
	}

	@Test
	public void testDelete(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createSelectionQuery( "from Person", Person.class )
							.list().forEach( session::remove );
				}
		);
	}
}
