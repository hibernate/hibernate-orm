/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated.delegate;

import java.util.Date;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.SourceType;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.generator.EventType;
import org.hibernate.generator.values.GeneratedValuesMutationDelegate;
import org.hibernate.id.insert.AbstractSelectingDelegate;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.MutationType;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link GeneratedValuesMutationDelegate efficient generated values retrieval}
 * with {@link InheritanceType#JOINED} inheritance structures.
 *
 * @author Marco Belladelli
 */
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsIdentityColumns.class )
@DomainModel( annotatedClasses = {
		MutationDelegateJoinedInheritanceTest.BaseEntity.class,
		MutationDelegateJoinedInheritanceTest.ChildEntity.class,
		MutationDelegateJoinedInheritanceTest.NonGeneratedParent.class,
		MutationDelegateJoinedInheritanceTest.GeneratedChild.class,
} )
@SessionFactory( useCollectingStatementInspector = true )
public class MutationDelegateJoinedInheritanceTest {
	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.createMutationQuery( "delete from BaseEntity" ).executeUpdate() );
	}

	@Test
	public void testInsertBaseEntity(SessionFactoryScope scope) {
		final GeneratedValuesMutationDelegate delegate = getDelegate(
				scope,
				BaseEntity.class,
				MutationType.INSERT
		);
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		scope.inTransaction( session -> {
			final BaseEntity entity = new BaseEntity();
			session.persist( entity );
			session.flush();

			assertThat( entity.getId() ).isNotNull();
			assertThat( entity.getName() ).isEqualTo( "default_name" );

			assertThat( inspector.getSqlQueries().get( 0 ) ).contains( "insert" );
			inspector.assertExecutedCount(
					delegate instanceof AbstractSelectingDelegate
							? 3
							: delegate != null && delegate.supportsArbitraryValues() ? 1 : 2
			);
		} );
	}

	@Test
	public void testInsertChildEntity(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		scope.inTransaction( session -> {
			final ChildEntity entity = new ChildEntity();
			session.persist( entity );
			session.flush();

			assertThat( entity.getId() ).isNotNull();
			assertThat( entity.getName() ).isEqualTo( "default_name" );
			assertThat( entity.getChildName() ).isEqualTo( "default_child_name" );

			final GeneratedValuesMutationDelegate delegate = getDelegate(
					scope,
					ChildEntity.class,
					MutationType.INSERT
			);

			if ( delegate instanceof AbstractSelectingDelegate ) {
				assertThat( inspector.getSqlQueries().get( 0 ) ).contains( "insert" );
				assertThat( inspector.getSqlQueries().get( 2 ) ).contains( "insert" );
				// Note: this is a current restriction, mutation delegates only retrieve generated values
				// on the "root" table, and we expect other values to be read through a subsequent select
				inspector.assertIsSelect( 3 );
				inspector.assertExecutedCount( 4 );
			}
			else {
				assertThat( inspector.getSqlQueries().get( 0 ) ).contains( "insert" );
				assertThat( inspector.getSqlQueries().get( 1 ) ).contains( "insert" );
				// Note: this is a current restriction, mutation delegates only retrieve generated values
				// on the "root" table, and we expect other values to be read through a subsequent select
				inspector.assertIsSelect( 2 );
				inspector.assertExecutedCount( 3 );
			}
		} );
	}

	@Test
	public void testUpdateBaseEntity(SessionFactoryScope scope) {
		final GeneratedValuesMutationDelegate delegate = getDelegate(
				scope,
				BaseEntity.class,
				MutationType.UPDATE
		);
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		final Long id = scope.fromTransaction( session -> {
			final BaseEntity entity = new BaseEntity();
			session.persist( entity );
			session.flush();
			return entity.getId();
		} );

		inspector.clear();

		scope.inTransaction( session -> {
			final BaseEntity entity = session.find( BaseEntity.class, id );
			entity.setData( "changed" );
			session.flush();

			assertThat( entity.getUpdateDate() ).isNotNull();

			inspector.assertIsSelect( 0 );
			assertThat( inspector.getSqlQueries().get( 1 ) ).contains( "update " );
			inspector.assertExecutedCount(
					delegate != null && delegate.supportsArbitraryValues() ? 2 : 3
			);
		} );
	}

	@Test
	public void testUpdateChildEntity(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		final Long id = scope.fromTransaction( session -> {
			final ChildEntity entity = new ChildEntity();
			session.persist( entity );
			session.flush();
			return entity.getId();
		} );

		inspector.clear();

		scope.inTransaction( session -> {
			final ChildEntity entity = session.find( ChildEntity.class, id );
			entity.setData( "changed" );
			session.flush();

			assertThat( entity.getUpdateDate() ).isNotNull();
			assertThat( entity.getChildUpdateDate() ).isNotNull();

			inspector.assertIsSelect( 0 );
			assertThat( inspector.getSqlQueries().get( 1 ) ).contains( "update " );
			// Note: this is a current restriction, mutation delegates only retrieve generated values
			// on the "root" table, and we expect other values to be read through a subsequent select
			inspector.assertIsSelect( 2 );
			inspector.assertExecutedCount( 3 );
		} );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-18259" )
	public void testGeneratedOnlyOnChild(SessionFactoryScope scope) {
		final GeneratedValuesMutationDelegate delegate = getDelegate(
				scope,
				NonGeneratedParent.class,
				MutationType.UPDATE
		);
		// Mutation delegates only support generated values on the "root" table
		assertThat( delegate ).isNull();

		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();

		scope.inTransaction( session -> {
			final GeneratedChild generatedChild = new GeneratedChild();
			generatedChild.setId( 1L );
			session.persist( generatedChild );

			session.flush();

			assertThat( generatedChild.getName() ).isEqualTo( "child_name" );
			inspector.assertExecutedCount( 3 );
			inspector.assertIsInsert( 0 );
			inspector.assertIsInsert( 1 );
			inspector.assertIsSelect( 2 );
		} );
	}

	private static GeneratedValuesMutationDelegate getDelegate(
			SessionFactoryScope scope,
			@SuppressWarnings( "SameParameterValue" ) Class<?> entityClass,
			MutationType mutationType) {
		final EntityPersister entityDescriptor = scope.getSessionFactory()
				.getMappingMetamodel()
				.findEntityDescriptor( entityClass );
		return entityDescriptor.getMutationDelegate( mutationType );
	}

	@Entity( name = "BaseEntity" )
	@Inheritance( strategy = InheritanceType.JOINED )
	@SuppressWarnings( "unused" )
	public static class BaseEntity {
		@Id
		@GeneratedValue( strategy = GenerationType.IDENTITY )
		private Long id;

		@Generated( event = EventType.INSERT )
		@ColumnDefault( "'default_name'" )
		private String name;

		@UpdateTimestamp( source = SourceType.DB )
		private Date updateDate;

		@SuppressWarnings( "FieldCanBeLocal" )
		private String data;

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Date getUpdateDate() {
			return updateDate;
		}

		public void setData(String data) {
			this.data = data;
		}
	}

	@Entity( name = "ChildEntity" )
	@SuppressWarnings( "unused" )
	public static class ChildEntity extends BaseEntity {
		@Generated( event = EventType.INSERT )
		@ColumnDefault( "'default_child_name'" )
		private String childName;

		@UpdateTimestamp( source = SourceType.DB )
		private Date childUpdateDate;

		public String getChildName() {
			return childName;
		}

		public Date getChildUpdateDate() {
			return childUpdateDate;
		}
	}

	@Entity( name = "NonGeneratedParent" )
	@Inheritance( strategy = InheritanceType.JOINED )
	@SuppressWarnings( "unused" )
	public static class NonGeneratedParent {
		@Id
		private Long id;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	@Entity( name = "GeneratedChild" )
	@SuppressWarnings( "unused" )
	public static class GeneratedChild extends NonGeneratedParent {
		@Generated( event = EventType.INSERT )
		@ColumnDefault( "'child_name'" )
		private String name;

		public String getName() {
			return name;
		}
	}
}
