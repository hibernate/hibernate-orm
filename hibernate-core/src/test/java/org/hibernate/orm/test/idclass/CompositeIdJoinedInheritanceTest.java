package org.hibernate.orm.test.idclass;

import java.io.Serializable;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DomainModel(
		annotatedClasses = {
				CompositeIdJoinedInheritanceTest.BaseEntity.class,
				CompositeIdJoinedInheritanceTest.SubEntity.class
		}
)
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-20961")
public class CompositeIdJoinedInheritanceTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createMutationQuery("delete from SubEntity").executeUpdate();
					session.createMutationQuery("delete from BaseEntity").executeUpdate();
				}
		);
	}

	@Test
	public void testJoinedInheritanceWithCompositeId(SessionFactoryScope scope) {
		Long id1 = 1L;
		Long id2 = 2L;

		scope.inTransaction(
				session -> {
					SubEntity entity = new SubEntity();
					entity.key1 = id1;
					entity.key2 = id2;
					entity.baseName = "Base Data";
					entity.subName = "Sub Data";
					session.persist(entity);
				}
		);

		scope.inTransaction(
				session -> {
					CompositeKey key = new CompositeKey();
					key.key1 = id1;
					key.key2 = id2;
					SubEntity entity = session.find(SubEntity.class, key);
					assertNotNull(entity);
					assertEquals("Base Data", entity.baseName);
					assertEquals("Sub Data", entity.subName);
				}
		);
	}

	public static class CompositeKey implements Serializable {
		public Long key1;
		public Long key2;
	}

	@Entity(name = "BaseEntity")
	@Inheritance(strategy = InheritanceType.JOINED)
	@IdClass(CompositeKey.class)
	public static class BaseEntity {
		@Id
		public Long key1;
		@Id
		public Long key2;
		public String baseName;
	}

	@Entity(name = "SubEntity")
	public static class SubEntity extends BaseEntity {
		public String subName;
	}
}
