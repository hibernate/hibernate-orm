/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any.annotations;


import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@DomainModel(
		annotatedClasses = {
				PropertySetWithCascade.class,
				IntegerProperty.class,
				StringProperty.class
		}
)
@SessionFactory
@JiraKey( "HHH-19971" )
public class AnyCascadeAttributeTest {

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	public void testManyToAnyCascadePersist(SessionFactoryScope scope) {
		// Persist parent only, children should cascade
		scope.inTransaction( session -> {
			PropertySetWithCascade set = new PropertySetWithCascade( "test" );
			set.setSomeProperty( new StringProperty( "name", "alex" ) );
			set.addGeneralProperty( new IntegerProperty( "age", 23 ) );
			session.persist( set );

		} );

		// Verify children were persisted via cascade
		scope.inTransaction( session -> {
			PropertySetWithCascade result = session
					.createQuery( "select s from PropertySetWithCascade s where name = :name", PropertySetWithCascade.class )
					.setParameter( "name", "test" )
					.uniqueResult();
			assertNotNull( result );
			assertNotNull( result.getSomeProperty() );
			assertEquals( "alex", result.getSomeProperty().asString() );
			assertEquals( 1, result.getGeneralProperties().size() );
			assertEquals( "23", result.getGeneralProperties().get( 0 ).asString());
		} );
	}

	@Test
	public void testAnyCascadePersist(SessionFactoryScope scope) {
		// Persist parent with @Any child - should cascade
		scope.inTransaction( session -> {

			PropertySetWithCascade set = new PropertySetWithCascade( "any-test" );
			set.setSomeProperty( new IntegerProperty("score", 100) );
			session.persist( set );

		} );

		scope.inTransaction( session -> {
			PropertySetWithCascade result = session
					.createQuery( "select s from PropertySetWithCascade s where name = :name", PropertySetWithCascade.class )
					.setParameter( "name", "any-test" )
					.uniqueResult();
			assertNotNull( result );
			assertNotNull( result.getSomeProperty() );
			assertInstanceOf( IntegerProperty.class, result.getSomeProperty() );
			assertEquals( "100", result.getSomeProperty().asString());
		} );
	}

	@Test
	public void testAnyCascadeMerge(SessionFactoryScope scope) {
		Integer setId = scope.fromTransaction( session -> {
			PropertySetWithCascade set = new PropertySetWithCascade( "merge-test" );
			set.setSomeProperty( new StringProperty( "key", "original" ) );
			session.persist( set );
			return set.getId();
		} );

		scope.inTransaction( session -> {
			PropertySetWithCascade set = session.find( PropertySetWithCascade.class, setId );
			( (StringProperty) set.getSomeProperty() ).setValue( "updated" );
			session.merge( set );
		} );

		scope.inTransaction( session -> {
			PropertySetWithCascade result = session.find( PropertySetWithCascade.class, setId );
			assertNotNull( result.getSomeProperty() );
			assertEquals( "updated", result.getSomeProperty().asString() );
		} );
	}

	@Test
	public void testManyToAnyCascadeMerge(SessionFactoryScope scope) {
		Integer setId = scope.fromTransaction( session -> {
			PropertySetWithCascade set = new PropertySetWithCascade( "many-merge-test" );
			set.addGeneralProperty( new StringProperty( "key", "original" ) );
			session.persist( set );
			return set.getId();
		} );

		scope.inTransaction( session -> {
			PropertySetWithCascade set = session.find( PropertySetWithCascade.class, setId );
			( (StringProperty) set.getGeneralProperties().get( 0 ) ).setValue( "updated" );
			session.merge( set );
		} );

		scope.inTransaction( session -> {
			PropertySetWithCascade result = session.find( PropertySetWithCascade.class, setId );
			assertEquals( 1, result.getGeneralProperties().size() );
			assertEquals( "updated", result.getGeneralProperties().get( 0 ).asString() );
		} );
	}

	@Test
	public void testAnyCascadeRemove(SessionFactoryScope scope) {
		Integer[] ids = new Integer[2];

		scope.inTransaction( session -> {
			PropertySetWithCascade set = new PropertySetWithCascade( "remove-test" );
			StringProperty prop = new StringProperty( "key", "value" );
			set.setSomeProperty( prop );
			session.persist( set );
			ids[0] = set.getId();
		} );

		// get the property id before removal
		Integer propId = scope.fromTransaction( session -> {
			PropertySetWithCascade set = session.find( PropertySetWithCascade.class, ids[0] );
			return ( (StringProperty) set.getSomeProperty() ).getId();
		} );

		scope.inTransaction( session -> {
			PropertySetWithCascade set = session.find( PropertySetWithCascade.class, ids[0] );
			session.remove( set );
		} );

		scope.inTransaction( session -> {
			assertNull( session.find( PropertySetWithCascade.class, ids[0] ) );
			assertNull( session.find( StringProperty.class, propId ) );
		} );
	}

	@Test
	public void testManyToAnyCascadeRemove(SessionFactoryScope scope) {
		Integer setId = scope.fromTransaction( session -> {
			PropertySetWithCascade set = new PropertySetWithCascade( "many-remove-test" );
			set.addGeneralProperty( new IntegerProperty( "count", 5 ) );
			session.persist( set );
			return set.getId();
		} );

		Integer propId = scope.fromTransaction( session -> {
			PropertySetWithCascade set = session.find( PropertySetWithCascade.class, setId );
			return ( (IntegerProperty) set.getGeneralProperties().get( 0 ) ).getId();
		} );

		scope.inTransaction( session -> {
			PropertySetWithCascade set = session.find( PropertySetWithCascade.class, setId );
			session.remove( set );
		} );

		scope.inTransaction( session -> {
			assertNull( session.find( PropertySetWithCascade.class, setId ) );
			assertNull( session.find( IntegerProperty.class, propId ) );
		} );
	}

	@Test
	public void testAnyCascadeRefresh(SessionFactoryScope scope) {
		Integer setId = scope.fromTransaction( session -> {
			PropertySetWithCascade set = new PropertySetWithCascade( "refresh-test" );
			set.setSomeProperty( new StringProperty( "key", "value" ) );
			session.persist( set );
			return set.getId();
		} );

		scope.inTransaction( session -> {
			PropertySetWithCascade set = session.find( PropertySetWithCascade.class, setId );
			// dirty the child in-memory without flushing
			( (StringProperty) set.getSomeProperty() ).setValue( "dirty" );
			// refresh should reload from DB, cascading to child
			session.refresh( set );
			assertEquals( "value", set.getSomeProperty().asString() );
		} );
	}

	@Test
	public void testManyToAnyCascadeRefresh(SessionFactoryScope scope) {
		Integer setId = scope.fromTransaction( session -> {
			PropertySetWithCascade set = new PropertySetWithCascade( "many-refresh-test" );
			set.addGeneralProperty( new IntegerProperty( "count", 10 ) );
			session.persist( set );
			return set.getId();
		} );

		scope.inTransaction( session -> {
			PropertySetWithCascade set = session.find( PropertySetWithCascade.class, setId );
			( (IntegerProperty) set.getGeneralProperties().get( 0 ) ).setValue( 999 );
			session.refresh( set );
			assertEquals( "10", set.getGeneralProperties().get( 0 ).asString() );
		} );
	}
}
