package org.hibernate.orm.test.merge;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				SimpleMergeTest.MyEntity.class
		}
)
@SessionFactory
@JiraKey( "HHH-17634" )
public class SimpleMergeTest {

	@Test
	public void testMergeNewEntity(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					MyEntity newEntity = new MyEntity();

					assertThat( newEntity.getId() ).isNull();

					MyEntity mergedEntity = session.merge( newEntity );
					assertThat( mergedEntity ).isNotSameAs( newEntity );
					assertThat( mergedEntity.getId() ).isNotNull();
					assertThat( newEntity.getId() ).isNull();
				}
		);
	}


	@Entity(name = "MyEntity")
	public static class MyEntity {

		@Id
		@GeneratedValue
		private Integer id;

		private String name;

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

}


