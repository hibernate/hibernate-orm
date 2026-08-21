/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.stateless;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;

@SessionFactory
@DomainModel(annotatedClasses =
		{UpsertWithDiscriminatorTest.X.class,
		UpsertWithDiscriminatorTest.Y.class,
		UpsertWithDiscriminatorTest.Z.class})
@JiraKey( "HHH-18742" )
@JiraKey( "HHH-20155" )
class UpsertWithDiscriminatorTest {
	@Test void test(SessionFactoryScope scope) {
		scope.getSessionFactory().inStatelessTransaction( s -> {
			s.upsert( new X() );
			s.upsert( new Y() );
			s.upsert( new Z() );
		});
		scope.getSessionFactory().inStatelessTransaction( s -> {
			var results = s.createQuery( "from X order by id asc", X.class ).getResultList();
			assertEquals( X.class, results.get(0).getClass() );
			assertEquals( Y.class, results.get(1).getClass() );
			assertEquals( Z.class, results.get(2).getClass() );
		});
		scope.getSessionFactory().inStatelessTransaction( s -> {
			s.upsert( new X() );
			s.upsert( new Y() );
			s.upsert( new Z() );
		});
	}

	@Entity(name = "X")
	@DiscriminatorValue("X")
	static class X {
		@Id
		long id;
	}
	@Entity(name = "Y")
	@DiscriminatorValue("Y")
	static class Y extends X {
		{
			id = 1;
		}
	}
	@Entity(name = "Z")
	@DiscriminatorValue("Z")
	static class Z extends X {
		{
			id = 2;
		}
	}
}
