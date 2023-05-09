package org.hibernate.orm.test.embeddable;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;

@DomainModel(annotatedClasses = {
		EmbeddableAsElementCollectionWithUpdateableFalseTest.MyEntity.class,
		EmbeddableAsElementCollectionWithUpdateableFalseTest.MyEmbeddable.class
})
@SessionFactory
@JiraKey("HHH-16573")
public class EmbeddableAsElementCollectionWithUpdateableFalseTest {

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
			assertThat( entity.getMyEmbeddables() ).hasSize(1);
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
