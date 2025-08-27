/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import java.time.LocalDate;

import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.community.dialect.GaussDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaConflictClause;
import org.hibernate.query.criteria.JpaCriteriaInsertValues;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.contacts.Contact;
import org.hibernate.testing.orm.domain.contacts.Contact.Name;
import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ServiceRegistry
@DomainModel(standardModels = { StandardDomainModel.GAMBIT, StandardDomainModel.CONTACTS })
@SessionFactory
@JiraKey("HHH-17506")
public class InsertConflictTests {

	@BeforeEach
	public void prepareData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.persist( new Contact(
							1,
							new Name( "A", "B" ),
							Contact.Gender.FEMALE,
							LocalDate.of( 2000, 1, 1 )
					) );
					session.persist( new BasicEntity( 1, "data" ) );
				}
		);
	}

	@AfterEach
	public void cleanupData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testOnConflictDoNothing(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					int updated = session.createMutationQuery(
							"insert into BasicEntity (id, data) " +
									"values (1, 'John') " +
									"on conflict do nothing"
					).executeUpdate();
					if ( scope.getSessionFactory().getJdbcServices().getDialect() instanceof MySQLDialect ) {
						// Since JDBC set the MySQL CLIENT_FOUND_ROWS flag, the updated count is 1 even if values didn't change
						// Also see https://dev.mysql.com/doc/refman/8.0/en/insert-on-duplicate.html
						assertEquals( 1, updated );
					}
					else {
						assertEquals( 0, updated );
					}
					final BasicEntity basicEntity = session.find( BasicEntity.class, 1 );
					assertEquals( "data", basicEntity.getData() );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsUpsertOrMerge.class)
	public void testOnConflictDoUpdate(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					//tag::hql-insert-conflict-example[]
					int updated = session.createMutationQuery(
							"insert into BasicEntity (id, data) " +
									"values (1, 'John') " +
									"on conflict(id) do update " +
									"set data = excluded.data"
					).executeUpdate();
					//end::hql-insert-conflict-example[]
					if ( scope.getSessionFactory().getJdbcServices().getDialect() instanceof MySQLDialect ) {
						// Strange MySQL returns 2 if the conflict action updates a row
						// Also see https://dev.mysql.com/doc/refman/8.0/en/insert-on-duplicate.html
						assertEquals( 2, updated );
					}
					else {
						assertEquals( 1, updated );
					}
					final BasicEntity basicEntity = session.find( BasicEntity.class, 1 );
					assertEquals( "John", basicEntity.getData() );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsUpsertOrMerge.class)
	@SkipForDialect(dialectClass = InformixDialect.class, reason = "MATCHED does not support AND condition")
	public void testOnConflictDoUpdateWithWhere(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					int updated = session.createMutationQuery(
							"insert into BasicEntity (id, data) " +
									"values (1, 'John') " +
									"on conflict(id) do update " +
									"set data = excluded.data " +
									"where id > 1"
					).executeUpdate();
					if ( scope.getSessionFactory().getJdbcServices().getDialect() instanceof MySQLDialect ) {
						// Since JDBC set the MySQL CLIENT_FOUND_ROWS flag, the updated count is 1 even if values didn't change
						// Also see https://dev.mysql.com/doc/refman/8.0/en/insert-on-duplicate.html
						assertEquals( 1, updated );
					}
					else if ( scope.getSessionFactory().getJdbcServices().getDialect() instanceof SybaseASEDialect ) {
						// Sybase seems to report all matched rows as affected and ignores additional predicates
						assertEquals( 1, updated );
					}
					else if ( scope.getSessionFactory().getJdbcServices().getDialect() instanceof GaussDBDialect ) {
						// GaussDB seems to report all matched rows as affected and ignores additional predicates
						assertEquals( 1, updated );
					}
					else {
						assertEquals( 0, updated );
					}
					final BasicEntity basicEntity = session.find( BasicEntity.class, 1 );
					assertEquals( "data", basicEntity.getData() );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsUpsertOrMerge.class)
	@SkipForDialect(dialectClass = InformixDialect.class, reason = "MATCHED does not support AND condition")
	public void testOnConflictDoUpdateWithWhereCriteria(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					final JpaCriteriaInsertValues<BasicEntity> insert = cb.createCriteriaInsertValues( BasicEntity.class );
					insert.setInsertionTargetPaths(
							insert.getTarget().get( "id" ),
							insert.getTarget().get( "data" )
					);
					insert.values( cb.values( cb.value( 1 ), cb.value( "John" ) ) );
					final JpaConflictClause<BasicEntity> conflictClause = insert.onConflict();
					conflictClause.conflictOnConstraintPaths( insert.getTarget().get( "id" ) )
							.onConflictDoUpdate()
							.set( "data", conflictClause.getExcludedRoot().get( "data" ) )
							.where( cb.gt( insert.getTarget().get( "id" ), 1 ) );
					int updated = session.createMutationQuery( insert ).executeUpdate();
					if ( scope.getSessionFactory().getJdbcServices().getDialect() instanceof MySQLDialect ) {
						// Since JDBC set the MySQL CLIENT_FOUND_ROWS flag, the updated count is 1 even if values didn't change
						// Also see https://dev.mysql.com/doc/refman/8.0/en/insert-on-duplicate.html
						assertEquals( 1, updated );
					}
					else if ( scope.getSessionFactory().getJdbcServices().getDialect() instanceof SybaseASEDialect ) {
						// Sybase seems to report all matched rows as affected and ignores additional predicates
						assertEquals( 1, updated );
					}
					else if ( scope.getSessionFactory().getJdbcServices().getDialect() instanceof GaussDBDialect ) {
						// GaussDB seems to report all matched rows as affected and ignores additional predicates
						assertEquals( 1, updated );
					}
					else {
						assertEquals( 0, updated );
					}
					final BasicEntity basicEntity = session.find( BasicEntity.class, 1 );
					assertEquals( "data", basicEntity.getData() );
				}
		);
	}

	@Test
	public void testOnConflictDoNothingMultiTable(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					int updated = session.createMutationQuery(
							"insert into Contact (id, name) " +
									"values (1, ('John', 'Doe')) " +
									"on conflict do nothing"
					).executeUpdate();
					if ( scope.getSessionFactory().getJdbcServices().getDialect() instanceof MySQLDialect ) {
						// Since JDBC set the MySQL CLIENT_FOUND_ROWS flag, the updated count is 1 even if values didn't change
						// Also see https://dev.mysql.com/doc/refman/8.0/en/insert-on-duplicate.html
						assertEquals( 1, updated );
					}
					else {
						assertEquals( 0, updated );
					}
					final Contact contact = session.find( Contact.class, 1 );
					assertEquals( "A", contact.getName().getFirst() );
					assertEquals( "B", contact.getName().getLast() );
					assertEquals( Contact.Gender.FEMALE, contact.getGender() );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsUpsertOrMerge.class)
	@SkipForDialect(dialectClass = SybaseASEDialect.class, reason = "MERGE into a table that has a self-referential FK does not work")
	public void testOnConflictDoUpdateMultiTable(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					int updated = session.createMutationQuery(
							"insert into Contact (id, name, gender) " +
									"values (1, ('John', 'Doe'), MALE) " +
									"on conflict(id) do update " +
									"set name = excluded.name, gender = excluded.gender"
					).executeUpdate();
					if ( scope.getSessionFactory().getJdbcServices().getDialect() instanceof MySQLDialect ) {
						// Strange MySQL returns 2 if the conflict action updates a row
						// Also see https://dev.mysql.com/doc/refman/8.0/en/insert-on-duplicate.html
						assertEquals( 2, updated );
					}
					else {
						assertEquals( 1, updated );
					}
					final Contact contact = session.find( Contact.class, 1 );
					assertEquals( "John", contact.getName().getFirst() );
					assertEquals( "Doe", contact.getName().getLast() );
					assertEquals( Contact.Gender.MALE, contact.getGender() );
				}
		);
	}

	@Test
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsUpsertOrMerge.class)
	@SkipForDialect(dialectClass = SybaseASEDialect.class, reason = "MERGE into a table that has a self-referential FK does not work")
	@SkipForDialect(dialectClass = InformixDialect.class, reason = "MATCHED does not support AND condition")
	public void testOnConflictDoUpdateWithWhereMultiTable(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					int updated = session.createMutationQuery(
							"insert into Contact (id, name, gender) " +
									"values (1, ('John', 'Doe'), FEMALE) " +
									"on conflict(id) do update " +
									"set name = excluded.name, gender = excluded.gender " +
									"where id > 1"
					).executeUpdate();
					if ( scope.getSessionFactory().getJdbcServices().getDialect() instanceof MySQLDialect ) {
						// Since JDBC set the MySQL CLIENT_FOUND_ROWS flag, the updated count is 1 even if values didn't change
						// Also see https://dev.mysql.com/doc/refman/8.0/en/insert-on-duplicate.html
						assertEquals( 1, updated );
					}
					else if ( scope.getSessionFactory().getJdbcServices().getDialect() instanceof SybaseASEDialect ) {
						// Sybase seems to report all matched rows as affected and ignores additional predicates
						assertEquals( 1, updated );
					}
					else if ( scope.getSessionFactory().getJdbcServices().getDialect() instanceof GaussDBDialect ) {
						// GaussDB seems to report all matched rows as affected and ignores additional predicates
						assertEquals( 1, updated );
					}
					else {
						assertEquals( 0, updated );
					}
					final Contact contact = session.find( Contact.class, 1 );
					assertEquals( "A", contact.getName().getFirst() );
					assertEquals( "B", contact.getName().getLast() );
					assertEquals( Contact.Gender.FEMALE, contact.getGender() );
				}
		);
	}
}
