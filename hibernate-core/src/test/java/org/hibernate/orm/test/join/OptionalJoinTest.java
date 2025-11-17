/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.join;

import java.util.List;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Chris Jones and Gail Badner
 */
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsNoColumnInsert.class)
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/join/Thing.hbm.xml"
)
@SessionFactory
public class OptionalJoinTest {


	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testUpdateNonNullOptionalJoinToDiffNonNull(SessionFactoryScope scope) {

		scope.inTransaction(
				session -> {
					// create a new thing with a non-null name
					Thing thing = new Thing();
					thing.setName( "one" );
					session.persist( thing );
				}
		);

		scope.inTransaction(
				session -> {
					List<Thing> things = session.createQuery( "from Thing" ).list();
					assertEquals( 1, things.size() );
					Thing thing = things.get( 0 );
					assertEquals( "one", thing.getName() );
					assertEquals( "ONE", thing.getNameUpper() );
					// give it a new non-null name and save it
					thing.setName( "one_changed" );
				}
		);

		scope.inTransaction(
				session -> {
					List<Thing> things = session.createQuery( "from Thing" ).list();
					assertEquals( 1, things.size() );
					Thing thing = things.get( 0 );
					assertEquals( "one_changed", thing.getName() );
					assertEquals( "ONE_CHANGED", thing.getNameUpper() );
					session.remove( thing );
				}
		);

	}

	@Test
	public void testMergeNonNullOptionalJoinToDiffNonNullDetached(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// create a new thing with a non-null name
					Thing thing = new Thing();
					thing.setName( "one" );
					session.persist( thing );
				}
		);

		Thing aThing = scope.fromTransaction(
				session -> {
					List<Thing> things = session.createQuery( "from Thing" ).list();
					assertEquals( 1, things.size() );
					Thing thing = things.get( 0 );
					assertEquals( "one", thing.getName() );
					assertEquals( "ONE", thing.getNameUpper() );
					return thing;
				}
		);


		// change detached thing name to a new non-null name and save it
		aThing.setName( "one_changed" );

		scope.inTransaction(
				session ->
						session.merge( aThing )
		);

		scope.inTransaction(
				session -> {
					List<Thing> things = session.createQuery( "from Thing" ).list();
					assertEquals( 1, things.size() );
					Thing thing = things.get( 0 );
					assertEquals( "one_changed", thing.getName() );
					assertEquals( "ONE_CHANGED", thing.getNameUpper() );
					session.remove( thing );
				}
		);

	}

	@Test
	public void testMergeNonNullOptionalJoinToNullDetached(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// create a new thing with a non-null name
					Thing thing = new Thing();
					thing.setName( "one" );
					session.persist( thing );
				}
		);

		Thing aThing = scope.fromTransaction(
				session -> {
					List<Thing> things = session.createQuery( "from Thing" ).list();
					assertEquals( 1, things.size() );
					Thing thing = things.get( 0 );
					assertEquals( "one", thing.getName() );
					assertEquals( "ONE", thing.getNameUpper() );
					return thing;
				}
		);

		// give detached thing a null name and save it
		aThing.setName( null );

		scope.inTransaction(
				session ->
						session.merge( aThing )
		);

		scope.inTransaction(
				session -> {
					List<Thing> things = session.createQuery( "from Thing" ).list();
					assertEquals( 1, things.size() );
					Thing thing = things.get( 0 );
					assertNull( thing.getName() );
					assertNull( thing.getNameUpper() );
					session.remove( thing );
				}
		);
	}

	@Test
	public void testUpdateNullOptionalJoinToNonNull(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// create a new thing with a null name
					Thing thing = new Thing();
					thing.setName( null );
					session.persist( thing );
				}
		);


		scope.inTransaction(
				session -> {
					List<Thing> things = session.createQuery( "from Thing" ).list();
					assertEquals( 1, things.size() );
					Thing thing = things.get( 0 );
					assertNull( thing.getName() );
					// change name to a non-null value
					thing.setName( "two" );
				}
		);

		scope.inTransaction(
				session -> {
					List<Thing> things = session.createQuery( "from Thing" ).list();
					assertEquals( 1, things.size() );
					Thing thing = things.get( 0 );
					assertEquals( "two", thing.getName() );
					assertEquals( "TWO", thing.getNameUpper() );
					session.remove( thing );
				}
		);
	}

	@Test
	public void testMergeNullOptionalJoinToNonNullDetached(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					// create a new thing with a null name
					Thing thing = new Thing();
					thing.setName( null );
					session.persist( thing );
				}
		);

		Thing aThing = scope.fromTransaction(
				session -> {
					List<Thing> things = session.createQuery( "from Thing" ).list();
					assertEquals( 1, things.size() );
					Thing thing = things.get( 0 );
					assertNull( thing.getName() );
					assertNull( thing.getNameUpper() );
					return thing;
				}
		);


		// change detached thing name to a non-null value
		aThing.setName( "two" );

		scope.inTransaction(
				session ->
						session.merge( aThing )
		);

		scope.inTransaction(
				session -> {
					List<Thing> things = session.createQuery( "from Thing" ).list();
					assertEquals( 1, things.size() );
					Thing thing = things.get( 0 );
					assertEquals( "two", thing.getName() );
					assertEquals( "TWO", thing.getNameUpper() );
					session.remove( thing );
				}
		);
	}
}
