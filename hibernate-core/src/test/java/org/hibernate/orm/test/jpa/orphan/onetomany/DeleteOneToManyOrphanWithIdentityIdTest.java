package org.hibernate.orm.test.jpa.orphan.onetomany;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Jpa(
		annotatedClasses = {
			DeleteOneToManyOrphanWithIdentityIdTest.Parent.class,
			DeleteOneToManyOrphanWithIdentityIdTest.Child.class
		}
)
class DeleteOneToManyOrphanWithIdentityIdTest {

	@Test
	@TestForIssue(jiraKey = "HHH-15258")
	@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class)
	void testNoExceptionThrown(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent parent = new Parent();
					session.persist( parent );
					session.createQuery( "from Parent" ).getResultList();
					assertDoesNotThrow(
							() -> session.createQuery( "from Parent" ).getResultList(),
							"without fixing, an exception would be thrown for the second getResultList() invocation"
					);
				}
		);
	}

	@Entity(name = "Parent")
	static class Parent {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		Long id;

		@OneToMany(mappedBy = "parent", orphanRemoval = true)
		List<Child> children = new ArrayList<>();

	}

	@Entity(name = "Child")
	static class Child {

		@Id
		Long id;

		@ManyToOne
		Parent parent;

	}

}
