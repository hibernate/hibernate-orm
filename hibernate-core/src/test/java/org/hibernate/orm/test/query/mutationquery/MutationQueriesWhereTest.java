/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.mutationquery;

import java.util.List;

import org.hibernate.annotations.SQLRestriction;
import org.hibernate.query.MutationQuery;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

import static java.util.Arrays.stream;

/**
 * Same as {@link MutationQueriesFilterTest} and {@link MutationQueriesWhereAndFilterTest},
 * but using only {@link SQLRestriction @SQLRestriction}
 *
 * @author Marco Belladelli
 */
@SessionFactory( useCollectingStatementInspector = true )
@DomainModel( annotatedClasses = {
		MutationQueriesWhereTest.UserEntity.class,
		MutationQueriesWhereTest.DiscriminatorBase.class,
		MutationQueriesWhereTest.DiscriminatorUser.class,
		MutationQueriesWhereTest.TablePerClassBase.class,
		MutationQueriesWhereTest.TablePerClassUser.class,
		MutationQueriesWhereTest.RoleEntity.class
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16392" )
public class MutationQueriesWhereTest {
	@Test
	public void testDelete(SessionFactoryScope scope) {
		delete( scope, "UserEntity", 1L, 1, false, 0, 1 );
	}

	@Test
	public void testDeleteAll(SessionFactoryScope scope) {
		delete( scope, "UserEntity", null, 1, false, 0, 1 );
	}

	@Test
	public void testDeleteDiscriminator(SessionFactoryScope scope) {
		delete( scope, "DiscriminatorUser", 1L, 1, true, 0, 1 );
	}

	@Test
	public void testDeleteAllDiscriminator(SessionFactoryScope scope) {
		delete( scope, "DiscriminatorUser", null, 1, true, 0, 1 );
	}

	@Test
	public void testDeleteTablePerClass(SessionFactoryScope scope) {
		delete( scope, "TablePerClassBase", 1L, 2, false, 0 );
	}

	@Test
	public void testDeleteAllTablePerClass(SessionFactoryScope scope) {
		delete( scope, "TablePerClassBase", null, 2, false, 0 );
	}

	private void delete(
			SessionFactoryScope scope,
			String tableName,
			Long idParam,
			int deleteCount,
			boolean discriminator,
			int... queryIndexes) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction( session -> {
			final MutationQuery mutationQuery = session.createMutationQuery( String.format(
					"delete from %s %s",
					tableName,
					idParam != null ? "where id = :id" : ""
			) );
			if ( idParam != null ) {
				mutationQuery.setParameter( "id", 1L );
			}
			mutationQuery.executeUpdate();
			stream( queryIndexes ).forEach( queryIndex -> statementInspector.assertNumberOfOccurrenceInQueryNoSpace(
					queryIndex,
					"deleted",
					deleteCount
			) );
			if ( discriminator ) {
				stream( queryIndexes ).forEach( queryIndex -> statementInspector.assertNumberOfOccurrenceInQueryNoSpace(
						queryIndex,
						"disc_col",
						1
				) );
			}
		} );
	}

	@Test
	public void testUpdate(SessionFactoryScope scope) {
		update( scope, "UserEntity", 1L, false );
	}

	@Test
	public void testUpdateAll(SessionFactoryScope scope) {
		update( scope, "UserEntity", null, false );
	}

	@Test
	public void testUpdateDiscriminator(SessionFactoryScope scope) {
		update( scope, "DiscriminatorUser", 1L, true );
	}

	@Test
	public void testUpdateAllDiscriminator(SessionFactoryScope scope) {
		update( scope, "DiscriminatorUser", null, true );
	}

	@Test
	public void testUpdateTablePerClass(SessionFactoryScope scope) {
		update( scope, "TablePerClassUser", 1L, false );
	}

	@Test
	public void testUpdateAllTablePerClass(SessionFactoryScope scope) {
		update( scope, "TablePerClassUser", null, false );
	}

	private void update(SessionFactoryScope scope, String tableName, Long idParam, boolean discriminator) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		scope.inTransaction( session -> {
			final MutationQuery mutationQuery = session.createMutationQuery( String.format(
					"update %s set name = 'Marco' %s",
					tableName,
					idParam != null ? "where id = :id" : ""
			) );
			if ( idParam != null ) {
				mutationQuery.setParameter( "id", 1L );
			}
			mutationQuery.executeUpdate();
			statementInspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "deleted", 1 );
			if ( discriminator ) {
				statementInspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "disc_col", 1 );
			}
		} );
	}

	@Entity( name = "UserEntity" )
	@SQLRestriction( "deleted = false" )
	public static class UserEntity {
		@Id
		private Long id;
		@Column
		private boolean deleted;
		private String name;
		@ManyToMany
		@JoinTable(
				name = "users_roles",
				joinColumns = @JoinColumn( name = "user_id" ),
				inverseJoinColumns = @JoinColumn( name = "role_id" )
		)
		private List<RoleEntity> roles;
	}

	@Entity( name = "DiscriminatorBase" )
	@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
	@DiscriminatorColumn( name = "disc_col" )
	@SQLRestriction( "deleted = false" )
	public static class DiscriminatorBase {
		@Id
		private Long id;
		@Column
		private boolean deleted;
	}

	@Entity( name = "DiscriminatorUser" )
	@DiscriminatorValue( "user" )
	public static class DiscriminatorUser extends DiscriminatorBase {
		private String name;
		@ManyToMany
		@JoinTable(
				name = "users_roles",
				joinColumns = @JoinColumn( name = "user_id" ),
				inverseJoinColumns = @JoinColumn( name = "role_id" )
		)
		private List<RoleEntity> roles;
	}

	@Entity( name = "TablePerClassBase" )
	@Inheritance( strategy = InheritanceType.TABLE_PER_CLASS )
	@SQLRestriction( "deleted = false" )
	public static abstract class TablePerClassBase {
		@Id
		private Long id;
		@Column
		private boolean deleted;
		@ManyToMany
		@JoinTable(
				name = "users_roles",
				joinColumns = @JoinColumn( name = "user_id" ),
				inverseJoinColumns = @JoinColumn( name = "role_id" )
		)
		private List<RoleEntity> roles;
	}

	@Entity( name = "TablePerClassUser" )
	public static class TablePerClassUser extends TablePerClassBase {
		private String name;
	}

	@Entity( name = "RoleEntity" )
	public static class RoleEntity {
		@Id
		@GeneratedValue
		private Long id;
		private String name;
	}
}
