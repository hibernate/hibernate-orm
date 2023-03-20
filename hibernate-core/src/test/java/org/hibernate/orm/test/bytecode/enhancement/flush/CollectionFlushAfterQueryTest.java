package org.hibernate.orm.test.bytecode.enhancement.flush;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@RunWith(BytecodeEnhancerRunner.class)
@TestForIssue(jiraKey = "HHH-16337")
public class CollectionFlushAfterQueryTest extends BaseCoreFunctionalTestCase {

	private static final Long MY_ENTITY_ID = 1l;

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				CollectionFlushAfterQueryTest.MyEntity.class,
				CollectionFlushAfterQueryTest.MyOtherEntity.class,
				CollectionFlushAfterQueryTest.MyAnotherEntity.class
		};
	}

	@Test
	public void testAutoFlush() {
		inTransaction(
				session -> {
					MyEntity myEntity = new MyEntity( MY_ENTITY_ID, "my entity" );
					MyOtherEntity otherEntity = new MyOtherEntity( 2l, "my other entity" );
					myEntity.addOtherEntity( otherEntity );
					session.persist( otherEntity );
					session.persist( myEntity );
				}
		);

		inTransaction(
				session -> {
					MyEntity myEntity = session.find( MyEntity.class, MY_ENTITY_ID );

					MyOtherEntity otherEntity = new MyOtherEntity( 3l, "my new other entity" );
					Set<MyOtherEntity> set = new HashSet<>();
					set.add( otherEntity );
					myEntity.setOtherEntities( set );

					session.createQuery( "from MyAnotherEntity ", MyAnotherEntity.class ).getResultList();
				}
		);

		inTransaction(
				session -> {
					MyEntity myEntity = session.find( MyEntity.class, MY_ENTITY_ID );
					Set<MyOtherEntity> redirectUris = myEntity.getOtherEntities();
					assertThat( redirectUris.size() ).isEqualTo( 1 );

					Optional<MyOtherEntity> first = redirectUris.stream().findFirst();
					assertThat( first.get().getName() ).isEqualTo( "my new other entity" );
				}
		);

	}

	@Entity(name = "MyEntity")
	public static class MyEntity {
		@Id
		private Long id;

		private String name;

		@OneToMany(cascade = CascadeType.ALL)
		protected Set<MyOtherEntity> otherEntities = new HashSet<>();

		public MyEntity() {
		}

		public MyEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return this.id;
		}

		public Set<MyOtherEntity> getOtherEntities() {
			return otherEntities;
		}

		public void setOtherEntities(Set<MyOtherEntity> otherEntities) {
			this.otherEntities = otherEntities;
		}

		public void addOtherEntity(MyOtherEntity otherEntity) {
			this.otherEntities.add( otherEntity );
		}
	}

	@Entity(name = "MyOtherEntity")
	public static class MyOtherEntity {

		@Id
		private Long id;

		private String name;

		public MyOtherEntity() {
		}

		public MyOtherEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return this.id;
		}

		public String getName() {
			return name;
		}
	}

	@Entity(name = "MyAnotherEntity")
	public static class MyAnotherEntity {

		@Id
		private Long id;

		private String name;

		public MyAnotherEntity() {
		}

		public MyAnotherEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return this.id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}
}
