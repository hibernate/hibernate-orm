/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.array;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Piotr Krauzowicz
 * @author Gail Badner
 */
@DomainModel(
		annotatedClasses = PrimitiveCharacterArrayIdTest.DemoEntity.class
)
@SessionFactory
public class PrimitiveCharacterArrayIdTest {

	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					for ( int i = 0; i < 3; i++ ) {
						DemoEntity entity = new DemoEntity();
						entity.id = new char[] {
								(char) ( i + 'a' ),
								(char) ( i + 'b' ),
								(char) ( i + 'c' ),
								(char) ( i + 'd' )
						};
						entity.name = "Simple name " + i;
						session.persist( entity );
					}
				}
		);
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	/**
	 * Removes two records from database.
	 */
	@Test
	@JiraKey(value = "HHH-8999")
	public void testMultipleDeletions(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Query query = session.createQuery( "SELECT s FROM PrimitiveCharacterArrayIdTest$DemoEntity s" );
					List results = query.list();
					session.remove( results.get( 0 ) );
					session.remove( results.get( 1 ) );
				}
		);

		scope.inTransaction(
				session -> {
					Query query = session.createQuery( "SELECT s FROM PrimitiveCharacterArrayIdTest$DemoEntity s" );
					assertEquals( 1, query.list().size() );
				}
		);
	}

	/**
	 * Updates two records from database.
	 */
	@Test
	@JiraKey(value = "HHH-8999")
	public void testMultipleUpdates(SessionFactoryScope scope) {
		final String lastResultName = scope.fromTransaction(
				session -> {
					Query query = session.createQuery( "SELECT s FROM PrimitiveCharacterArrayIdTest$DemoEntity s" );
					List<DemoEntity> results = (List<DemoEntity>) query.list();
					results.get( 0 ).name = "Different 0";
					results.get( 1 ).name = "Different 1";
					return results.get( 0 ).name;
				}
		);

		scope.inTransaction(
				session -> {
					Query query = session.createQuery( "SELECT s FROM PrimitiveCharacterArrayIdTest$DemoEntity s" );
					List<DemoEntity> results = (List<DemoEntity>) query.list();
					final Set<String> names = new HashSet<String>();
					for ( DemoEntity entity : results ) {
						names.add( entity.name );
					}
					assertTrue( names.contains( "Different 0" ) );
					assertTrue( names.contains( "Different 1" ) );
					assertTrue( names.contains( lastResultName ) );
				}
		);
	}

	@Entity
	@Table(name = "DemoEntity")
	public static class DemoEntity {
		@Id
		public char[] id;
		public String name;
	}
}
