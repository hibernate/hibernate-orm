/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.functional;

import java.sql.PreparedStatement;

import org.hibernate.Transaction;
import org.hibernate.dialect.HANADialect;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the correctness of the SAP HANA fulltext-search functions.
 *
 * @author Jonathan Bregler
 */
@DomainModel(
		annotatedClasses = { HANASearchTest.SearchEntity.class }
)
@SessionFactory(exportSchema = false)
@RequiresDialect(HANADialect.class)
@SkipForDialect(dialectClass = HANADialect.class, majorVersion = 4)
public class HANASearchTest {

	private static final String ENTITY_NAME = "SearchEntity";

	@BeforeAll
	protected void prepareTest(SessionFactoryScope scope) throws Exception {
		scope.inTransaction(
				session -> session.doWork(
						connection -> {
							try (PreparedStatement ps = connection.prepareStatement( "CREATE COLUMN TABLE " + ENTITY_NAME
																							+ " (key INTEGER, t TEXT, c NVARCHAR(255), PRIMARY KEY (key))" )) {
								ps.execute();
							}
							try (PreparedStatement ps = connection
									.prepareStatement( "CREATE FULLTEXT INDEX FTI ON " + ENTITY_NAME + " (c)" )) {
								ps.execute();
							}
						}
				)
		);
	}

	@AfterAll
	protected void cleanupTest(SessionFactoryScope scope) throws Exception {
		scope.inTransaction(
				session -> session.doWork(
						connection -> {
							try (PreparedStatement ps = connection.prepareStatement( "DROP TABLE " + ENTITY_NAME + " CASCADE" )) {
								ps.execute();
							}
							catch (Exception e) {
								// Ignore
							}
						}
				)
		);
	}

	@AfterEach
	protected void cleanupTestData(SessionFactoryScope scope) throws Exception {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey(value = "HHH-13021")
	public void testTextType(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					SearchEntity entity = new SearchEntity();
					entity.key = Integer.valueOf( 1 );
					entity.t = "TEST TEXT";
					entity.c = "TEST STRING";

					session.persist( entity );
					session.flush();

					Query<Object[]> legacyQuery = session.createQuery( "select b, snippets(t), highlighted(t), score() from "
																		+ ENTITY_NAME + " b where contains(b.t, 'text')", Object[].class );

					Object[] result = legacyQuery.getSingleResult();
					SearchEntity retrievedEntity = (SearchEntity) result[0];

					assertEquals( 4, result.length );

					assertEquals( Integer.valueOf( 1 ), retrievedEntity.key );
					assertEquals( "TEST TEXT", retrievedEntity.t );
					assertEquals( "TEST STRING", retrievedEntity.c );

					assertEquals( "TEST <b>TEXT</b>", result[1] );
					assertEquals( "TEST <b>TEXT</b>", result[2] );
					assertEquals( 0.75d, result[3] );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-13021")
	public void testTextTypeFalse(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					SearchEntity entity = new SearchEntity();
					entity.key = Integer.valueOf( 1 );
					entity.t = "TEST TEXT";
					entity.c = "TEST STRING";

					session.persist( entity );
					session.flush();

					Query<Object[]> legacyQuery = session.createQuery( "select b, snippets(t), highlighted(t), score() from " + ENTITY_NAME
																		+ " b where not contains(b.t, 'string')", Object[].class );

					Object[] result = legacyQuery.getSingleResult();
					SearchEntity retrievedEntity = (SearchEntity) result[0];

					assertEquals( 4, result.length );

					assertEquals( Integer.valueOf( 1 ), retrievedEntity.key );
					assertEquals( "TEST TEXT", retrievedEntity.t );
					assertEquals( "TEST STRING", retrievedEntity.c );

					assertEquals( "TEST TEXT", result[1] );
					assertEquals( "TEST TEXT", result[2] );
					assertEquals( 1d, result[3] );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-13021")
	public void testCharType(SessionFactoryScope scope) throws Exception {
		scope.inSession(
				session -> {
					Transaction t = session.beginTransaction();
					SearchEntity entity = new SearchEntity();
					entity.key = Integer.valueOf( 1 );
					entity.t = "TEST TEXT";
					entity.c = "TEST STRING";

					session.persist( entity );
					t.commit();

					session.beginTransaction();
					Query<Object[]> legacyQuery = session.createQuery(
							"select b, snippets(c), highlighted(c), score() from " + ENTITY_NAME
									+ " b where contains(b.c, 'string')",
							Object[].class
					);

					Object[] result = legacyQuery.getSingleResult();
					SearchEntity retrievedEntity = (SearchEntity) result[0];

					assertEquals( 4, result.length );

					assertEquals( Integer.valueOf( 1 ), retrievedEntity.key );
					assertEquals( "TEST TEXT", retrievedEntity.t );
					assertEquals( "TEST STRING", retrievedEntity.c );

					assertEquals( "TEST <b>STRING</b>", result[1] );
					assertEquals( "TEST <b>STRING</b>", result[2] );
					assertEquals( 0.75d, result[3] );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-13021")
	public void testCharTypeComplexQuery(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					Transaction t = session.beginTransaction();
					SearchEntity entity = new SearchEntity();
					entity.key = Integer.valueOf( 1 );
					entity.t = "TEST TEXT";
					entity.c = "TEST STRING";

					session.persist( entity );
					session.flush();
					t.commit();

					session.beginTransaction();
					Query<Object[]> legacyQuery = session.createQuery(
							"select b, snippets(c), highlighted(c), score() from " + ENTITY_NAME
									+ " b where contains(b.c, 'string') and key=1 and score() > 0.5",
							Object[].class );

					Object[] result = legacyQuery.getSingleResult();
					SearchEntity retrievedEntity = (SearchEntity) result[0];

					assertEquals( 4, result.length );

					assertEquals( Integer.valueOf( 1 ), retrievedEntity.key );
					assertEquals( "TEST TEXT", retrievedEntity.t );
					assertEquals( "TEST STRING", retrievedEntity.c );

					assertEquals( "TEST <b>STRING</b>", result[1] );
					assertEquals( "TEST <b>STRING</b>", result[2] );
					assertEquals( 0.75d, result[3] );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-13021")
	public void testFuzzy(SessionFactoryScope scope) {
		scope.inSession(
				session -> {
					Transaction t = session.beginTransaction();
					SearchEntity entity = new SearchEntity();
					entity.key = Integer.valueOf( 1 );
					entity.t = "TEST TEXT";
					entity.c = "TEST STRING";

					session.persist( entity );
					session.flush();
					t.commit();

					session.beginTransaction();

					Query<Object[]> legacyQuery = session.createQuery( "select b, snippets(c), highlighted(c), score() from " + ENTITY_NAME
																		+ " b where contains(b.c, 'string', FUZZY(0.7))", Object[].class );

					Object[] result = legacyQuery.getSingleResult();
					SearchEntity retrievedEntity = (SearchEntity) result[0];

					assertEquals( 4, result.length );

					assertEquals( Integer.valueOf( 1 ), retrievedEntity.key );
					assertEquals( "TEST TEXT", retrievedEntity.t );
					assertEquals( "TEST STRING", retrievedEntity.c );

					assertEquals( "TEST <b>STRING</b>", result[1] );
					assertEquals( "TEST <b>STRING</b>", result[2] );
					assertEquals( 0.75d, result[3] );
				}
		);
	}

	@Entity(name = ENTITY_NAME)
	public static class SearchEntity {

		@Id
		public Integer key;

		public String t;

		public String c;
	}
}
