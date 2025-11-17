/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.cascade;

import java.util.ArrayList;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Check some of the individual cascade styles
 * <p>
 * TODO: do something for refresh
 *
 * @author Emmanuel Bernard
 */
@DomainModel(
		annotatedClasses = {
				Mouth.class,
				Tooth.class
		}
)
@SessionFactory
public class CascadeTest {

	@Test
	public void testPersist(SessionFactoryScope scope) {
		Tooth leftTooth = new Tooth();
		scope.inTransaction(
				session -> {
					Tooth tooth = new Tooth();
					tooth.leftNeighbour = leftTooth;
					session.persist( tooth );
				}
		);


		scope.inTransaction(
				session -> {
					Tooth tooth = ( session.get( Tooth.class, leftTooth.id ) );
					assertNotNull( tooth );
				}
		);
	}

	@Test
	public void testMerge(SessionFactoryScope scope) {
		Tooth t = new Tooth();
		Tooth rightTooth = new Tooth();
		scope.inTransaction(
				session -> {
					t.type = "canine";
					t.rightNeighbour = rightTooth;
					rightTooth.type = "incisive";
					session.persist( rightTooth );
					session.persist( t );
				}
		);

		Tooth tooth = scope.fromTransaction(
				session -> {
					Tooth result = session.get( Tooth.class, t.id );
					assertEquals( "incisive", t.rightNeighbour.type );
					return result;
				}
		);


		scope.inTransaction(
				session -> {
					tooth.rightNeighbour.type = "premolars";
					session.merge( tooth );
				}
		);

		scope.inTransaction(
				session -> {
					Tooth result = session.get( Tooth.class, rightTooth.id );
					assertEquals( "premolars", result.type );
				}
		);
	}

	@Test
	public void testRemove(SessionFactoryScope scope) {
		Tooth tooth = new Tooth();
		scope.inTransaction(
				session -> {
					Mouth mouth = new Mouth();
					session.persist( mouth );
					session.persist( tooth );
					tooth.mouth = mouth;
					mouth.teeth = new ArrayList<>();
					mouth.teeth.add( tooth );
				}
		);

		scope.inTransaction(
				session -> {
					Tooth t = session.get( Tooth.class, tooth.id );
					assertNotNull( t );
					session.remove( t.mouth );
				}
		);
		scope.inTransaction(
				session -> {
					Tooth t = session.get( Tooth.class, tooth.id );
					assertNull( t );
				}
		);
	}

	@Test
	public void testDetach(SessionFactoryScope scope) {
		Mouth mouth = new Mouth();
		scope.inTransaction(
				session -> {
					Tooth tooth = new Tooth();
					session.persist( mouth );
					session.persist( tooth );
					tooth.mouth = mouth;
					mouth.teeth = new ArrayList<>();
					mouth.teeth.add( tooth );
				}
		);

		scope.inTransaction(
				session -> {
					Mouth m = session.get( Mouth.class, mouth.id );
					assertNotNull( m );
					assertEquals( 1, m.teeth.size() );
					Tooth tooth = m.teeth.iterator().next();
					session.evict( m );
					assertFalse( session.contains( tooth ) );
				}
		);

		scope.inTransaction(
				session ->
						session.remove( session.get( Mouth.class, mouth.id ) )
		);
	}
}
