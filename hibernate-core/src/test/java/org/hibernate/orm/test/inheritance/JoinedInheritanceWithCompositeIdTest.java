package org.hibernate.orm.test.inheritance;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.PrimaryKeyJoinColumns;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

/**
 * Test for HHH-20884: Verifies that JOINED inheritance with composite IDs
 * works correctly when using @PrimaryKeyJoinColumns.
 */
@JiraKey("HHH-20884")
@DomainModel(
		annotatedClasses = {
				JoinedInheritanceWithCompositeIdTest.Cid.class,
				JoinedInheritanceWithCompositeIdTest.ParentA.class,
				JoinedInheritanceWithCompositeIdTest.EntityA.class,
				JoinedInheritanceWithCompositeIdTest.EntityB.class,
				JoinedInheritanceWithCompositeIdTest.EntityC.class
		}
)
@SessionFactory
public class JoinedInheritanceWithCompositeIdTest {

	@Test
	public void testBootstrap(SessionFactoryScope scope) {
		// If we get here, the SessionFactory was created successfully,
		// which means the fix for HHH-20884 is working
		scope.inTransaction( session -> {
			// Just verify we can create instances
			EntityA entityA = new EntityA();
			entityA.setCid( new Cid( 1, 1 ) );
			entityA.setValueA( "Test Value" );

			EntityB entityB = new EntityB();
			entityB.setId( new Cid( 1, 2 ) );

			session.persist( entityA );
			session.persist( entityB );
		} );
	}

	@Embeddable
	public static class Cid implements Serializable {
		private Integer project;
		private Integer id;

		public Cid() {
		}

		public Cid(Integer project, Integer id) {
			this.project = project;
			this.id = id;
		}

		public Integer getProject() {
			return project;
		}

		public void setProject(Integer project) {
			this.project = project;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "ParentA")
	@Inheritance(strategy = InheritanceType.JOINED)
	@DiscriminatorColumn(name = "def_type_id", discriminatorType = DiscriminatorType.INTEGER)
	public static abstract class ParentA {
		@EmbeddedId
		private Cid cid;

		public Cid getCid() {
			return cid;
		}

		public void setCid(Cid cid) {
			this.cid = cid;
		}
	}

	@Entity(name = "EntityA")
	@DiscriminatorValue("1")
	@PrimaryKeyJoinColumns({
			@PrimaryKeyJoinColumn(name = "project", referencedColumnName = "project"),
			@PrimaryKeyJoinColumn(name = "id", referencedColumnName = "id")
	})
	public static class EntityA extends ParentA {
		private String valueA;

		public String getValueA() {
			return valueA;
		}

		public void setValueA(String valueA) {
			this.valueA = valueA;
		}
	}

	@Entity(name = "EntityB")
	public static class EntityB {
		@EmbeddedId
		private Cid id;

		public Cid getId() {
			return id;
		}

		public void setId(Cid id) {
			this.id = id;
		}
	}

	@IdClass(EntityC.PKey.class)
	@Entity(name = "EntityC")
	public static class EntityC implements Serializable {
		@Id
		@ManyToOne
		private EntityA entityA;

		@Id
		@ManyToOne
		private EntityB entityB;

		public EntityA getEntityA() {
			return entityA;
		}

		public void setEntityA(EntityA entityA) {
			this.entityA = entityA;
		}

		public EntityB getEntityB() {
			return entityB;
		}

		public void setEntityB(EntityB entityB) {
			this.entityB = entityB;
		}

		public static class PKey implements Serializable {
			private Cid entityA;
			private Cid entityB;

			public Cid getEntityA() {
				return entityA;
			}

			public void setEntityA(Cid entityA) {
				this.entityA = entityA;
			}

			public Cid getEntityB() {
				return entityB;
			}

			public void setEntityB(Cid entityB) {
				this.entityB = entityB;
			}
		}
	}
}
