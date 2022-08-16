package org.hibernate.orm.test.batchfetch;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.query.spi.QueryImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = { StreamTest.Child.class, StreamTest.Parent.class }
)
@SessionFactory
@ServiceRegistry(settings = @Setting(name = AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, value = "2"))
@JiraKey("HHH-15449")
public class StreamTest {

	public static final String FIELD_VALUE = "afield";

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent parent = new Parent( FIELD_VALUE );
					session.persist( parent );
					session.persist( new Child( parent ) );
				}
		);
	}

	@Test
	public void testGetResultStream(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					QueryImplementor<Child> query = session
							.createQuery( "select c from Child as c where c.parent.someField=:someField", Child.class )
							.setParameter( "someField", FIELD_VALUE );
					Stream<Child> resultStream = query.getResultStream();
					List<Child> children = resultStream.collect( Collectors.toList() );
					assertThat( children.size() ).isEqualTo( 1 );

					assertThat( children.get( 0 ).getParent() ).isNotNull();
				}
		);
	}

	@Entity(name = "Child")
	@Table(name = "CHILD_TABLE")
	public static class Child {

		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne
		@JoinColumn(name = "parent_id", nullable = false, updatable = false)
		private Parent parent;

		public Child() {
		}

		public Child(Parent parent) {
			this.parent = parent;
		}

		public Parent getParent() {
			return parent;
		}

		public Long getId() {
			return id;
		}
	}

	@Entity(name = "Parent")
	@Table(name = "PARENT_TABLE")
	public static class Parent {
		@Id
		@GeneratedValue
		private Long id;

		private String someField;

		public Parent() {
		}

		public Parent(String someField) {
			this.someField = someField;
		}

		public String getSomeField() {
			return someField;
		}

		public Long getId() {
			return id;
		}

	}


}
