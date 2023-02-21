package org.hibernate.orm.test.mapping.embeddable;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@DomainModel(
		annotatedClasses = {
				EmbeddableWithIdenticallyNamedAssociationTest.EntityA.class,
				EmbeddableWithIdenticallyNamedAssociationTest.EntityB.class,
				EmbeddableWithIdenticallyNamedAssociationTest.EmbeddableB.class
		}
)
@SessionFactory
@TestForIssue(jiraKey = "TODO")
public class EmbeddableWithIdenticallyNamedAssociationTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		EntityA a1 = new EntityA();
		a1.setId( 1 );
		EntityB b1 = new EntityB();
		b1.setId( 1 );
		b1.setIdenticallyNamedAssociationFromB( a1 );
		a1.setIdenticallyNamedAssociationFromA( b1 );

		EntityA a2 = new EntityA();
		a2.setId( 2 );
		EntityB b2 = new EntityB();
		b2.setId( 2 );
		b2.setIdenticallyNamedAssociationFromB( a2 );
		a2.setIdenticallyNamedAssociationFromA( b2 );

		EmbeddableB embeddableB = new EmbeddableB();
		embeddableB.setIdenticallyNamedAssociationFromB( a2 );
		b1.setEmbeddableB( embeddableB );
		EmbeddableA embeddableA = new EmbeddableA();
		embeddableA.setIdenticallyNamedAssociationFromA( b1 );
		a2.setEmbeddableA( embeddableA );

		scope.inTransaction( session -> {
			session.persist( a1 );
			session.persist( a2 );
			session.persist( b1 );
			session.persist( b2 );

			assertEntityContent( a1, a2, b1, b2 );
		} );
	}

	private void assertEntityContent(EntityA a1, EntityA a2, EntityB b1, EntityB b2) {
		assertThat( a1 ).isNotNull();
		assertThat( a2 ).isNotNull();
		assertThat( b1 ).isNotNull();
		assertThat( b2 ).isNotNull();

		assertThat( b1.getIdenticallyNamedAssociationFromB() ).isEqualTo( a1 );
		assertThat( a1.getIdenticallyNamedAssociationFromA() ).isEqualTo( b1 );

		assertThat( b2.getIdenticallyNamedAssociationFromB() ).isEqualTo( a2 );
		assertThat( a2.getIdenticallyNamedAssociationFromA() ).isEqualTo( b2 );

		assertThat( b1.getEmbeddableB() ).isNotNull();
		assertThat( b1.getEmbeddableB().getIdenticallyNamedAssociationFromB() ).isEqualTo( a2 );
		assertThat( a2.getEmbeddableA() ).isNotNull();
		assertThat( a2.getEmbeddableA().getIdenticallyNamedAssociationFromA() ).isEqualTo( b1 );
	}

	@Test
	public void testGetEntities(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			EntityA a1 = session.get( EntityA.class, 1 );
			EntityA a2 = session.get( EntityA.class, 2 );
			EntityB b1 = session.get( EntityB.class, 1 );
			EntityB b2 = session.get( EntityB.class, 2 );

			// Run the *exact* same assertions we ran just after persisting.
			// Entity content should be identical, but the bug is: it's not.
			assertEntityContent(a1, a2, b1, b2);
		} );
	}

	@Entity(name = "entityA")
	public static class EntityA {
		@Id
		private Integer id;

		@OneToOne(mappedBy = "identicallyNamedAssociationFromB")
		private EntityB identicallyNamedAssociationFromA;

		@Embedded
		private EmbeddableA embeddableA;

		@Override
		public String toString() {
			return "EntityB{" +
					"id=" + id +
					", identicallyNamedAssociationFromA=" + identicallyNamedAssociationFromA.getId() +
					", embeddableA=" + embeddableA +
					'}';
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public EntityB getIdenticallyNamedAssociationFromA() {
			return identicallyNamedAssociationFromA;
		}

		public void setIdenticallyNamedAssociationFromA(EntityB identicallyNamedAssociationFromA) {
			this.identicallyNamedAssociationFromA = identicallyNamedAssociationFromA;
		}

		public EmbeddableA getEmbeddableA() {
			return embeddableA;
		}

		public void setEmbeddableA(EmbeddableA embeddableA) {
			this.embeddableA = embeddableA;
		}
	}

	@Embeddable
	public static class EmbeddableA {
		@OneToOne(mappedBy = "embeddableB.identicallyNamedAssociationFromB")
		private EntityB identicallyNamedAssociationFromA;

		@Override
		public String toString() {
			return "EmbeddableA{" +
					", identicallyNamedAssociationFromA=" + identicallyNamedAssociationFromA.getId() +
					'}';
		}

		public EntityB getIdenticallyNamedAssociationFromA() {
			return identicallyNamedAssociationFromA;
		}

		public void setIdenticallyNamedAssociationFromA(EntityB a) {
			this.identicallyNamedAssociationFromA = a;
		}
	}

	@Entity(name = "entityB")
	public static class EntityB {
		@Id
		private Integer id;

		@OneToOne
		@JoinColumn(name = "entityA_id")
		private EntityA identicallyNamedAssociationFromB;

		@Embedded
		private EmbeddableB embeddableB;

		@Override
		public String toString() {
			return "EntityB{" +
					"id=" + id +
					", identicallyNamedAssociationFromB=" + identicallyNamedAssociationFromB.getId() +
					", embeddableB=" + embeddableB +
					'}';
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public EntityA getIdenticallyNamedAssociationFromB() {
			return identicallyNamedAssociationFromB;
		}

		public void setIdenticallyNamedAssociationFromB(EntityA a) {
			this.identicallyNamedAssociationFromB = a;
		}

		public EmbeddableB getEmbeddableB() {
			return embeddableB;
		}

		public void setEmbeddableB(EmbeddableB embeddableB) {
			this.embeddableB = embeddableB;
		}
	}

	@Embeddable
	public static class EmbeddableB {
		@OneToOne
		@JoinColumn(name = "emb_entityA_id")
		private EntityA identicallyNamedAssociationFromB;

		@Override
		public String toString() {
			return "EmbeddableB{" +
					", identicallyNamedAssociationFromB=" + identicallyNamedAssociationFromB.getId() +
					'}';
		}

		public EntityA getIdenticallyNamedAssociationFromB() {
			return identicallyNamedAssociationFromB;
		}

		public void setIdenticallyNamedAssociationFromB(EntityA a) {
			this.identicallyNamedAssociationFromB = a;
		}
	}

}