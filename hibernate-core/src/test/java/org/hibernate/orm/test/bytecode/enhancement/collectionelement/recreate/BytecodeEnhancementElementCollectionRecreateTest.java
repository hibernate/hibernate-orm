/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.collectionelement.recreate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.orm.test.bytecode.enhancement.collectionelement.recreate.BytecodeEnhancementElementCollectionRecreateTest.MyEntity;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;

@DomainModel(
		annotatedClasses = {
				MyEntity.class,
		}
)
@SessionFactory(applyCollectionsInDefaultFetchGroup = false)
@BytecodeEnhanced
@EnhancementOptions(lazyLoading = true)
@JiraKey("HHH-14387")
public class BytecodeEnhancementElementCollectionRecreateTest {

	@BeforeEach
	public void check(SessionFactoryScope scope) {
		scope.inSession(
				session ->
						assertFalse( session.getSessionFactory().getSessionFactoryOptions()
								.isCollectionsInDefaultFetchGroupEnabled() )
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.createQuery( "delete from myentity" ).executeUpdate()
		);
	}

	@Test
	public void testRecreateCollection(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			MyEntity entity = new MyEntity();
			entity.setId( 1 );
			entity.setElements( Arrays.asList( "one", "two", "four" ) );
			session.persist( entity );
		} );

		scope.inTransaction( session -> {
			MyEntity entity = session.get( MyEntity.class, 1 );
			entity.setElements( Arrays.asList( "two", "three" ) );
			session.persist( entity );
		} );

		scope.inTransaction( session -> {
			MyEntity entity = session.get( MyEntity.class, 1 );
			assertThat( entity.getElements() )
					.containsExactlyInAnyOrder( "two", "three" );
		} );
	}

	@Test
	public void testRecreateCollectionFind(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			MyEntity entity = new MyEntity();
			entity.setId( 1 );
			entity.setElements( Arrays.asList( "one", "two", "four" ) );
			session.persist( entity );
		} );

		scope.inTransaction( session -> {
			MyEntity entity = session.find( MyEntity.class, 1 );
			entity.setElements( Arrays.asList( "two", "three" ) );
			session.persist( entity );
		} );

		scope.inTransaction( session -> {
			MyEntity entity = session.find( MyEntity.class, 1 );
			assertThat( entity.getElements() )
					.containsExactlyInAnyOrder( "two", "three" );
		} );
	}

	@Test
	public void testDeleteCollection(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			MyEntity entity = new MyEntity();
			entity.setId( 1 );
			entity.setElements( Arrays.asList( "one", "two", "four" ) );
			session.persist( entity );
		} );

		scope.inTransaction( session -> {
			MyEntity entity = session.get( MyEntity.class, 1 );
			entity.setElements( new ArrayList<>() );
		} );

		scope.inTransaction( session -> {
			MyEntity entity = session.get( MyEntity.class, 1 );
			assertThat( entity.getElements() )
					.isEmpty();
		} );
	}

	@Test
	public void testDeleteCollectionFind(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			MyEntity entity = new MyEntity();
			entity.setId( 1 );
			entity.setElements( Arrays.asList( "one", "two", "four" ) );
			session.persist( entity );
		} );

		scope.inTransaction( session -> {
			MyEntity entity = session.find( MyEntity.class, 1 );
			entity.setElements( new ArrayList<>() );
		} );

		scope.inTransaction( session -> {
			MyEntity entity = session.find( MyEntity.class, 1 );
			assertThat( entity.getElements() )
					.isEmpty();
		} );
	}

	@Test
	public void testLoadAndCommitTransactionDoesNotDeleteCollection(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			MyEntity entity = new MyEntity();
			entity.setId( 1 );
			entity.setElements( Arrays.asList( "one", "two", "four" ) );
			session.persist( entity );
		} );

		scope.inTransaction( session ->
				session.get( MyEntity.class, 1 )
		);

		scope.inTransaction( session -> {
			MyEntity entity = session.get( MyEntity.class, 1 );
			assertThat( entity.getElements() )
					.containsExactlyInAnyOrder( "one", "two", "four" );
		} );

	}

	@Test
	public void testLoadAndCommitTransactionDoesNotDeleteCollectionFind(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			MyEntity entity = new MyEntity();
			entity.setId( 1 );
			entity.setElements( Arrays.asList( "one", "two", "four" ) );
			session.persist( entity );
		} );

		scope.inTransaction( session ->
				session.find( MyEntity.class, 1 )
		);

		scope.inTransaction( session -> {
			MyEntity entity = session.find( MyEntity.class, 1 );
			assertThat( entity.getElements() )
					.containsExactlyInAnyOrder( "one", "two", "four" );
		} );

	}

	@Entity(name = "myentity")
	public static class MyEntity {
		@Id
		private Integer id;

		@ElementCollection
		@OrderColumn
		private List<String> elements;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public List<String> getElements() {
			return elements;
		}

		public void setElements(List<String> elements) {
			this.elements = elements;
		}

	}

}
