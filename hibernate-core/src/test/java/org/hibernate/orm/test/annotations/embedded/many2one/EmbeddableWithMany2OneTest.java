/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.embedded.many2one;

import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {
				Person.class, Country.class
		}
)
@SessionFactory
public class EmbeddableWithMany2OneTest {

	@Test
	public void testJoinAcrossEmbedded(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "from Person p join p.address as a join a.country as c where c.name = 'US'", Person.class )
							.list();
					session.createQuery( "from Person p join p.address as a join a.country as c where c.id = 'US'", Person.class )
							.list();
				}
		);
	}

	@Test
	public void testBasicOps(SessionFactoryScope scope) {
		Person person = new Person( "Steve", new Address() );
		scope.inTransaction(
				session -> {
					Country country = new Country( "US", "United States of America" );
					session.persist( country );
					person.getAddress().setLine1( "123 Main" );
					person.getAddress().setCity( "Anywhere" );
					person.getAddress().setCountry( country );
					person.getAddress().setPostalCode( "123456789" );
					session.persist( person );
				}
		);

		scope.inTransaction(
				session -> {
					session.createQuery( "from Person p where p.address.country.iso2 = 'US'" )
							.list();
					// same query!
					session.createQuery( "from Person p where p.address.country.id = 'US'" )
							.list();
					Person p = session.getReference( Person.class, person.getId() );
					session.remove( p );
					List countries = session.createQuery( "from Country" ).list();
					assertEquals( 1, countries.size() );
					session.remove( countries.get( 0 ) );
				}
		);
	}
}
