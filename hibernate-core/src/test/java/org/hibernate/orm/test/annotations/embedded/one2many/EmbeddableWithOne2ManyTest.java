/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.embedded.one2many;

import java.util.List;


import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.FailureExpected;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Steve Ebersole
 */
@DomainModel(annotatedClasses = {
//		Alias.class, Person.class
})
@SessionFactory
public class EmbeddableWithOne2ManyTest {

	@Test
	@FailureExpected(jiraKey = "HHH-4883")
	public void testJoinAcrossEmbedded(SessionFactoryScope scope) {
		// NOTE : this may or may not work now with HHH-4883 fixed,
		// but i cannot do this checking until HHH-4599 is done.
		scope.inTransaction(
				session -> {
					session.createQuery( "from Person p join p.name.aliases a where a.source = 'FBI'", Person.class )
							.list();
				}
		);
	}

	@Test
	@FailureExpected(jiraKey = "HHH-4599")
	public void testBasicOps(SessionFactoryScope scope) {
		Person person = new Person( "John", "Dillinger" );
		scope.inTransaction(
				session -> {
					Alias alias = new Alias( "Public Enemy", "Number 1", "FBI" );
					session.persist( alias );
					person.getName().getAliases().add( alias );
					session.persist( person );
				}
		);
		scope.inTransaction(
				session -> {
					Person p = session.getReference( Person.class, person.getId() );
					session.remove( p );
					List<Alias> aliases = session.createQuery( "from Alias", Alias.class ).list();
					assertEquals( 0, aliases.size() );
				}
		);
	}
}
