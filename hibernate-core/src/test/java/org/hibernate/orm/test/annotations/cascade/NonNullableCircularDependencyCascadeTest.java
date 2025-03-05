/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cascade;

import java.util.HashSet;

import org.hibernate.TransientPropertyValueException;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Jeff Schnitzer
 * @author Gail Badner
 */
@DomainModel(
		annotatedClasses = {
				Child.class,
				Parent.class
		}
)
@SessionFactory
public class NonNullableCircularDependencyCascadeTest {

	@Test
	public void testIdClassInSuperclass(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent p = new Parent();
					p.setChildren( new HashSet<Child>() );

					Child ch = new Child( p );
					p.getChildren().add( ch );
					p.setDefaultChild( ch );

					try {
						session.persist( p );
						session.flush();
						fail( "should have failed because of transient entities have non-nullable, circular dependency." );
					}
					catch (IllegalStateException ex) {
						// expected
						assertThat( ex.getCause(), instanceOf( TransientPropertyValueException.class ) );
					}
				}
		);
	}
}
