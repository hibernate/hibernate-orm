/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.collectionelement.recreate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.boot.SessionFactoryBuilder;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(BytecodeEnhancerRunner.class)
@EnhancementOptions(lazyLoading = true)
@TestForIssue(jiraKey = "HHH-14387")
public class BytecodeEnhancementElementCollectionRecreateCollectionsInDefaultGroupTest
		extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				MyEntity.class
		};
	}

	@Override
	protected void configureSessionFactoryBuilder(SessionFactoryBuilder sfb) {
		super.configureSessionFactoryBuilder( sfb );
		sfb.applyCollectionsInDefaultFetchGroup( true );
	}

	@Before
	public void check() {
		inSession(
				session ->
						assertTrue( session.getSessionFactory().getSessionFactoryOptions()
											.isCollectionsInDefaultFetchGroupEnabled() )
		);
	}


	@After
	public void tearDown() {
		inTransaction(
				session ->
						session.createQuery( "delete from myentity" ).executeUpdate()
		);
	}

	@Test
	public void testRecreateCollection() {
		inTransaction( session -> {
			MyEntity entity = new MyEntity();
			entity.setId( 1 );
			entity.setElements( Arrays.asList( "one", "two", "four" ) );
			session.persist( entity );
		} );

		inTransaction( session -> {
			MyEntity entity = session.get( MyEntity.class, 1 );
			entity.setElements( Arrays.asList( "two", "three" ) );
			session.persist( entity );
		} );

		inTransaction( session -> {
			MyEntity entity = session.get( MyEntity.class, 1 );
			assertThat( entity.getElements() )
					.containsExactlyInAnyOrder( "two", "three" );
		} );
	}

	@Test
	public void testRecreateCollectionFind() {
		inTransaction( session -> {
			MyEntity entity = new MyEntity();
			entity.setId( 1 );
			entity.setElements( Arrays.asList( "one", "two", "four" ) );
			session.persist( entity );
		} );

		inTransaction( session -> {
			MyEntity entity = session.find( MyEntity.class, 1 );
			entity.setElements( Arrays.asList( "two", "three" ) );
			session.persist( entity );
		} );

		inTransaction( session -> {
			MyEntity entity = session.find( MyEntity.class, 1 );
			assertThat( entity.getElements() )
					.containsExactlyInAnyOrder( "two", "three" );
		} );
	}

	@Test
	public void testDeleteCollection() {
		inTransaction( session -> {
			MyEntity entity = new MyEntity();
			entity.setId( 1 );
			entity.setElements( Arrays.asList( "one", "two", "four" ) );
			session.persist( entity );
		} );

		inTransaction( session -> {
			MyEntity entity = session.get( MyEntity.class, 1 );
			entity.setElements( new ArrayList<>() );
		} );

		inTransaction( session -> {
			MyEntity entity = session.get( MyEntity.class, 1 );
			assertThat( entity.getElements() )
					.isEmpty();
		} );
	}

	@Test
	public void testDeleteCollectionFind() {
		inTransaction( session -> {
			MyEntity entity = new MyEntity();
			entity.setId( 1 );
			entity.setElements( Arrays.asList( "one", "two", "four" ) );
			session.persist( entity );
		} );

		inTransaction( session -> {
			MyEntity entity = session.find( MyEntity.class, 1 );
			entity.setElements( new ArrayList<>() );
		} );

		inTransaction( session -> {
			MyEntity entity = session.find( MyEntity.class, 1 );
			assertThat( entity.getElements() )
					.isEmpty();
		} );
	}

	@Test
	public void testLoadAndCommitTransactionDoesNotDeleteCollection() {
		inTransaction( session -> {
			MyEntity entity = new MyEntity();
			entity.setId( 1 );
			entity.setElements( Arrays.asList( "one", "two", "four" ) );
			session.persist( entity );
		} );

		inTransaction( session ->
							   session.get( MyEntity.class, 1 )
		);

		inTransaction( session -> {
			MyEntity entity = session.get( MyEntity.class, 1 );
			assertThat( entity.getElements() )
					.containsExactlyInAnyOrder( "one", "two", "four" );
		} );

	}

	@Test
	public void testLoadAndCommitTransactionDoesNotDeleteCollectionFind() {
		inTransaction( session -> {
			MyEntity entity = new MyEntity();
			entity.setId( 1 );
			entity.setElements( Arrays.asList( "one", "two", "four" ) );
			session.persist( entity );
		} );

		inTransaction( session ->
							   session.find( MyEntity.class, 1 )
		);

		inTransaction( session -> {
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
