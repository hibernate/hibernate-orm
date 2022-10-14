package org.hibernate.orm.test.mapping.embeddable.elementcollection;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderColumn;


@DomainModel(
		annotatedClasses = {
				EmbeddedElementCollectionWithIdenticallyNamedAssociationTest.EntityA.class,
				EmbeddedElementCollectionWithIdenticallyNamedAssociationTest.EntityB.class,
				EmbeddedElementCollectionWithIdenticallyNamedAssociationTest.EmbeddableB.class
		}
)
@SessionFactory
@TestForIssue(jiraKey = "HHH-15604")
public class EmbeddedElementCollectionWithIdenticallyNamedAssociationTest {

	// This is just a smoke test: the failure used to happen when building the SessionFactory.
	@Test
	public void smokeTest(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			EntityA a1 = new EntityA();
			a1.setId( 1 );
			EntityA a2 = new EntityA();
			a2.setId( 2 );

			EntityB b = new EntityB();
			b.setId( 1 );
			b.setIdenticallyNamedAssociation( a1 );
			EmbeddableB embeddableB = new EmbeddableB();
			embeddableB.setIdenticallyNamedAssociation( a2 );
			b.getElementCollection().add( embeddableB );

			session.persist( a1 );
			session.persist( a2 );
			session.persist( b );
		} );
		scope.inTransaction( session -> {
			EntityA a1 = session.get( EntityA.class, 1 );
			EntityA a2 = session.get( EntityA.class, 2 );
			EntityB b = session.get( EntityB.class, 1 );
			assertThat( a1 ).isNotNull();
			assertThat( a2 ).isNotNull();
			assertThat( b ).isNotNull();

			assertThat( b.getIdenticallyNamedAssociation() )
					.isEqualTo( a1 );

			assertThat( b.getElementCollection() ).hasSize( 1 );
			assertThat( b.getElementCollection().get( 0 ).getIdenticallyNamedAssociation() )
					.isEqualTo( a2 );
		} );
	}

	@Entity(name = "entityA")
	public static class EntityA {
		@Id
		private Integer id;

		@OneToOne(mappedBy = "identicallyNamedAssociation", fetch = FetchType.LAZY)
		private EntityB b;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public EntityB getB() {
			return b;
		}

		public void setB(EntityB b) {
			this.b = b;
		}
	}

	@Entity(name = "entityB")
	public static class EntityB {
		@Id
		private Integer id;

		@OneToOne
		@JoinColumn(name = "entityA_id")
		private EntityA identicallyNamedAssociation;

		@ElementCollection
		@Embedded
		@OrderColumn(name = "idx")
		@CollectionTable(name = "elemcollect")
		private List<EmbeddableB> elementCollection = new ArrayList<>();

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public EntityA getIdenticallyNamedAssociation() {
			return identicallyNamedAssociation;
		}

		public void setIdenticallyNamedAssociation(EntityA a) {
			this.identicallyNamedAssociation = a;
		}

		public List<EmbeddableB> getElementCollection() {
			return elementCollection;
		}
	}

	@Embeddable
	public static class EmbeddableB {
		@OneToOne
		@JoinColumn(name = "emb_entityA_id")
		private EntityA identicallyNamedAssociation;

		public EntityA getIdenticallyNamedAssociation() {
			return identicallyNamedAssociation;
		}

		public void setIdenticallyNamedAssociation(EntityA a) {
			this.identicallyNamedAssociation = a;
		}
	}

}