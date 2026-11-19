package org.hibernate.orm.test.generator.batch;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that a {@link PrePersist} callback runs before the entity state is captured
 * and checked for nullability, and still sees the sequence-generated identifier,
 * even though identifier batch generation is enabled.
 */
@SessionFactory
@DomainModel(annotatedClasses = {
		PrePersistBatchGenerationTest.Parent.class,
		PrePersistBatchGenerationTest.Child.class,
})
@ServiceRegistry(settings = @Setting(name = AvailableSettings.CHECK_NULLABILITY, value = "true"))
@Jira("https://hibernate.atlassian.net/browse/HHH-20737")
class PrePersistBatchGenerationTest {

	@AfterEach
	void dropTestData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void testPrePersistInitializesNonNullableAttribute(SessionFactoryScope scope) {
		final Long parentId = scope.fromTransaction( session -> {
			final Parent parent = new Parent();
			for ( int i = 0; i < 3; i++ ) {
				parent.addChild( new Child() );
			}
			session.persist( parent );

			assertThat( parent.getCode() ).isEqualTo( "parent-" + parent.getId() );
			assertThat( parent.getIdSeenInCallback() ).isNotNull().isEqualTo( parent.getId() );
			for ( Child child : parent.getChildren() ) {
				assertThat( child.getIdSeenInCallback() ).isNotNull().isEqualTo( child.getId() );
				assertThat( child.getCode() ).isEqualTo( "child-" + child.getId() );
			}
			return parent.getId();
		} );

		scope.inTransaction( session -> {
			final Parent parent = session.find( Parent.class, parentId );
			assertThat( parent.getCode() ).isEqualTo( "parent-" + parentId );
			assertThat( parent.getChildren() ).hasSize( 3 );
			for ( Child child : parent.getChildren() ) {
				assertThat( child.getCode() ).isEqualTo( "child-" + child.getId() );
			}
		} );
	}

	@Entity(name = "PrePersistBatchParent")
	static class Parent {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pre_persist_parent_seq")
		@SequenceGenerator(name = "pre_persist_parent_seq", allocationSize = 5)
		private Long id;

		@Column(nullable = false)
		private String code;

		@OneToMany(mappedBy = "parent", cascade = CascadeType.PERSIST)
		private List<Child> children = new ArrayList<>();

		private transient Long idSeenInCallback;

		@PrePersist
		void prePersist() {
			idSeenInCallback = id;
			code = "parent-" + id;
		}

		Long getId() {
			return id;
		}

		String getCode() {
			return code;
		}

		Long getIdSeenInCallback() {
			return idSeenInCallback;
		}

		List<Child> getChildren() {
			return children;
		}

		void addChild(Child child) {
			child.parent = this;
			children.add( child );
		}
	}

	@Entity(name = "PrePersistBatchChild")
	static class Child {
		@Id
		@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pre_persist_child_seq")
		@SequenceGenerator(name = "pre_persist_child_seq", allocationSize = 5)
		private Long id;

		@Column(nullable = false)
		private String code;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "parent_id")
		private Parent parent;

		private transient Long idSeenInCallback;

		@PrePersist
		void prePersist() {
			idSeenInCallback = id;
			code = "child-" + id;
		}

		Long getId() {
			return id;
		}

		String getCode() {
			return code;
		}

		Long getIdSeenInCallback() {
			return idSeenInCallback;
		}
	}
}
