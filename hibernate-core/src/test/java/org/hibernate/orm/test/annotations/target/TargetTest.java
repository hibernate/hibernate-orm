/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.target;


import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Emmanuel Bernard
 */
@DomainModel(
		annotatedClasses = {
				LuggageImpl.class,
				Brand.class
		}
)
@SessionFactory
public class TargetTest {

	@AfterEach
	public void afterEach(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Test
	public void testTargetOnEmbedded(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					LuggageImpl l = new LuggageImpl();
					l.setHeight( 12 );
					l.setWidth( 12 );
					Owner o = new OwnerImpl();
					o.setName( "Emmanuel" );
					l.setOwner( o );
					session.persist( l );
					session.flush();
					session.clear();
					l = session.find( LuggageImpl.class, l.getId() );
					assertEquals( "Emmanuel", l.getOwner().getName() );
				}
		);
	}

	@Test
	public void testTargetOnMapKey(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Luggage l = new LuggageImpl();
					l.setHeight( 12 );
					l.setWidth( 12 );
					Size size = new SizeImpl();
					size.setName( "S" );
					Owner o = new OwnerImpl();
					o.setName( "Emmanuel" );
					l.setOwner( o );
					session.persist( l );
					Brand b = new Brand();
					session.persist( b );
					b.getLuggagesBySize().put( size, l );
					session.flush();
					session.clear();
					b = session.find( Brand.class, b.getId() );
					assertEquals( "S", b.getLuggagesBySize().keySet().iterator().next().getName() );
				}
		);
	}

	@Test
	public void testTargetOnMapKeyManyToMany(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Luggage l = new LuggageImpl();
					l.setHeight( 12 );
					l.setWidth( 12 );
					Size size = new SizeImpl();
					size.setName( "S" );
					Owner o = new OwnerImpl();
					o.setName( "Emmanuel" );
					l.setOwner( o );
					session.persist( l );
					Brand b = new Brand();
					session.persist( b );
					b.getSizePerLuggage().put( l, size );
					session.flush();
					session.clear();
					b = session.find( Brand.class, b.getId() );
					assertEquals( 12d, b.getSizePerLuggage().keySet().iterator().next().getWidth(), 0.01 );
				}
		);
	}

}
