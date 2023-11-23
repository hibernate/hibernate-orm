package org.hibernate.orm.test.embeddable;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {
		EmbeddableAsOrderedElementCollectionWithUpdatableFalseTest.MyEntity.class,
		EmbeddableAsOrderedElementCollectionWithUpdatableFalseTest.MyEmbeddable.class
})
@SessionFactory
@JiraKey("HHH-16573")
@Disabled("We now assert that collection columns have the same insertable / updatable attributes, see HHH-17334")
public class EmbeddableAsOrderedElementCollectionWithUpdatableFalseTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createMutationQuery( "delete from MyEntity" ).executeUpdate();
				}
		);
	}

	@Test
	public void test(SessionFactoryScope scope) {
		var id = scope.fromTransaction( session -> {
			var entity = new MyEntity();
			session.persist( entity );
			return entity.getId();
		} );

		scope.inTransaction( session -> {
			MyEntity entity = session.find( MyEntity.class, id );
			assertThat( entity ).isNotNull();
			assertThat( entity.getMyEmbeddables() ).isEmpty();
			entity.getMyEmbeddables().add( new MyEmbeddable( "first" ) );
		} );

		scope.inTransaction( session -> {
			MyEntity entity = session.find( MyEntity.class, id );
			assertThat( entity ).isNotNull();
			List<MyEmbeddable> myEmbeddables = entity.getMyEmbeddables();
			assertThat( myEmbeddables ).hasSize( 1 );
		} );
	}

	@Test
	public void insertTest(SessionFactoryScope scope) {
		var id = scope.fromTransaction( session -> {
			var entity = new MyEntity();
			entity.getMyEmbeddables().add( new MyEmbeddable( "first" ) );
			entity.getMyEmbeddables().add( new MyEmbeddable( "third" ) );
			session.persist( entity );
			return entity.getId();
		} );

		scope.inTransaction( session -> {
			MyEntity entity = session.find( MyEntity.class, id );
			assertThat( entity ).isNotNull();
			assertThat( entity.getMyEmbeddables() ).hasSize( 2 );
			entity.getMyEmbeddables().add( 1, new MyEmbeddable( "second" ) );
		} );

		scope.inTransaction( session -> {
			MyEntity entity = session.find( MyEntity.class, id );
			assertThat( entity ).isNotNull();
			List<MyEmbeddable> myEmbeddables = entity.getMyEmbeddables();
			assertThat( myEmbeddables ).hasSize( 3 );
			assertThat( myEmbeddables.get( 0 ).getEmbeddedProperty() ).isEqualTo( "first" );
			assertThat( myEmbeddables.get( 1 ).getEmbeddedProperty() ).isEqualTo( "second" );
			assertThat( myEmbeddables.get( 2 ).getEmbeddedProperty() ).isEqualTo( "third" );
		} );
	}

	@Entity(name = "MyEntity")
	static class MyEntity {
		@Id
		@GeneratedValue
		private Integer id;

		@ElementCollection(fetch = FetchType.LAZY)
		@OrderColumn
		private List<MyEmbeddable> myEmbeddables = new ArrayList<>();

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public List<MyEmbeddable> getMyEmbeddables() {
			return myEmbeddables;
		}
	}

	@Embeddable
	public static class MyEmbeddable {
		@Column(updatable = false)
		private String embeddedProperty;

		public MyEmbeddable() {
		}

		public MyEmbeddable(String embeddedProperty) {
			this.embeddedProperty = embeddedProperty;
		}

		public String getEmbeddedProperty() {
			return embeddedProperty;
		}

		public void setEmbeddedProperty(String embeddedProperty) {
			this.embeddedProperty = embeddedProperty;
		}
	}
}
