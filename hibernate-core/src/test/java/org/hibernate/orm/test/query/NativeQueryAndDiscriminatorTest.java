package org.hibernate.orm.test.query;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				NativeQueryAndDiscriminatorTest.BaseEntity.class,
				NativeQueryAndDiscriminatorTest.TestEntity.class
		}
)
@SessionFactory
@JiraKey("HHH-18515")
public class NativeQueryAndDiscriminatorTest {


	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity e = new TestEntity( 1l, "test", EntityDiscriminator.T );
					session.persist( e );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createMutationQuery( "delete TestEntity" ).executeUpdate();
				}
		);
	}

	@Test
	public void testNativeQuery(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					BaseEntity entity = session.createNativeQuery(
									"select * from BASE_ENTITY where id = :id",
									BaseEntity.class
							)
							.setParameter( "id", 1l )
							.getSingleResult();
					assertThat( entity ).isNotNull();
				}
		);
	}


	@Entity(name = "BaseEntity")
	@Table(name = "BASE_ENTITY")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(name = "discriminator", discriminatorType = DiscriminatorType.CHAR)
	@DiscriminatorValue("B")
	public static class BaseEntity {

		@Id
		private Long id;

		private String name;

		@Column(insertable = false, updatable = false)
		@Enumerated(EnumType.STRING)
		private EntityDiscriminator discriminator;

		public BaseEntity() {
		}

		public BaseEntity(Long id, String name, EntityDiscriminator discriminator) {
			this.id = id;
			this.name = name;
			this.discriminator = discriminator;
		}
	}

	@Entity(name = "TestEntity")
	@DiscriminatorValue("T")
	public static class TestEntity extends BaseEntity {

		public TestEntity() {
		}

		public TestEntity(Long id, String name, EntityDiscriminator discriminator) {
			super( id, name, discriminator );
		}
	}

	enum EntityDiscriminator {
		T;
	}
}
