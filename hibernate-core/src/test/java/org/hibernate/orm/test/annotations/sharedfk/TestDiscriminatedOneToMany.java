/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.sharedfk;

import org.hibernate.PropertyValueException;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hibernate.Hibernate.isInitialized;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@JiraKey(value = "JPA-46")
@SessionFactory
@DomainModel(annotatedClasses = {AbstractChild.class, Parent.class, ConcreteChild1.class, ConcreteChild2.class})
public class TestDiscriminatedOneToMany {
	@Test
	void test(SessionFactoryScope scope) {
		ConcreteChild1 child0 = new ConcreteChild1();
		child0.value = "0";
		ConcreteChild1 child1 = new ConcreteChild1();
		child1.value = "1";
		ConcreteChild2 child2 = new ConcreteChild2();
		child2.value = "2";
		Parent parent = new Parent();
		parent.child1s = List.of( child0, child1 );
		parent.child2s = List.of( child2 );
		scope.inTransaction( s -> {
			s.persist( child0 );
			s.persist( child1 );
			s.persist( child2 );
			s.persist( parent );
		} );
		scope.inTransaction( s -> {
			Parent p = s.find(Parent.class, parent.id);
			assertEquals( 1, p.child2s.size() );
			assertEquals( 2, p.child1s.size() );
		} );
		scope.inTransaction( s -> {
			Parent p = s.createQuery("from Parent", Parent.class).getSingleResult();
			assertTrue( isInitialized( p.child1s ) ); //mapped EAGER
			assertTrue( isInitialized( p.child2s ) ); //mapped EAGER
			assertEquals( 1, p.child2s.size() );
			assertEquals( 2, p.child1s.size() );
		} );
		scope.inTransaction( s -> {
			Parent p = s.createQuery("from Parent left join fetch child1s", Parent.class).getSingleResult();
			assertTrue( isInitialized( p.child1s ) );
			assertTrue( isInitialized( p.child2s ) ); //mapped EAGER
			assertEquals( 1, p.child2s.size() );
			assertEquals( 2, p.child1s.size() );
		} );
		scope.inTransaction( s -> {
			Parent p = s.createQuery("from Parent join fetch child1s join fetch child2s", Parent.class).getSingleResult();
			assertTrue( isInitialized( p.child1s ) );
			assertTrue( isInitialized( p.child2s ) );
			assertEquals( 1, p.child2s.size() );
			assertEquals( 2, p.child1s.size() );
		} );
	}
	@Test
	public void testNonoptionalEnforced(SessionFactoryScope scope) {
		ConcreteChild1 child0 = new ConcreteChild1();
		Parent parent = new Parent();
		parent.child1s = List.of( child0 );
		try {
			scope.inTransaction( s -> {
				s.persist( parent );
				s.persist( child0 );
			} );
			fail();
		}
		catch (PropertyValueException e) {
			assertTrue( e.getMessage().contains("ConcreteChild1.value") );
		}
	}
}
