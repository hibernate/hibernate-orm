/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.inheritance.discriminator;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies that inserting entities works correctly when a subclass uses
 * {@code discriminator-value="null"} and has a not-null property.
 * <p>
 * The subclass not-null check constraint must use "is not null" (meaning
 * "if you're NOT this subclass, skip the check") rather than "is null"
 * (which would fail for all other subclasses).
 */
@JiraKey("HHH-20691")
@DomainModel(xmlMappings = "org/hibernate/orm/test/mapping/inheritance/discriminator/null-disc-subclass-check.orm.xml")
@SessionFactory
public class NullDiscriminatorSubclassCheckConstraintTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testInsertBase(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var base = new NullDiscSubclassCheckBase();
			base.setId( 1L );
			base.setName( "base" );
			session.persist( base );
		} );
	}

	@Test
	public void testInsertChild(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var child = new NullDiscSubclassCheckChild();
			child.setId( 2L );
			child.setName( "child" );
			child.setRequiredProp( "required" );
			session.persist( child );
		} );
	}

	@Test
	public void testInsertOther(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var other = new NullDiscSubclassCheckOther();
			other.setId( 3L );
			other.setName( "other" );
			other.setRequiredProp("required");
			other.setOtherProp( "otherValue" );
			session.persist( other );
		} );
	}

	@Test
	public void testInsertAllAndRead(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var base = new NullDiscSubclassCheckBase();
			base.setId( 1L );
			base.setName( "base" );
			session.persist( base );

			final var child = new NullDiscSubclassCheckChild();
			child.setId( 2L );
			child.setName( "child" );
			child.setRequiredProp( "required" );
			session.persist( child );

			final var other = new NullDiscSubclassCheckOther();
			other.setId( 3L );
			other.setName( "other" );
			other.setRequiredProp("required");
			other.setOtherProp( "otherValue" );
			session.persist( other );
		} );

		scope.inTransaction( session -> {
			final var base = session.find( NullDiscSubclassCheckBase.class, 1L );
			assertNotNull( base );
			assertEquals( "base", base.getName() );
			assertFalse( base instanceof NullDiscSubclassCheckChild );
			assertFalse( base instanceof NullDiscSubclassCheckOther );

			final var child = session.find( NullDiscSubclassCheckChild.class, 2L );
			assertNotNull( child );
			assertEquals( "child", child.getName() );
			assertEquals( "required", child.getRequiredProp() );

			final var other = session.find( NullDiscSubclassCheckOther.class, 3L );
			assertNotNull( other );
			assertEquals( "other", other.getName() );
			assertEquals( "otherValue", other.getOtherProp() );
		} );
	}
}
