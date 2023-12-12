package org.hibernate.orm.test.embeddable;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.io.Serializable;
import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@DomainModel(
		annotatedClasses = {
				OneToManyMappedByInEmbeddedTest.EntityA.class,
				OneToManyMappedByInEmbeddedTest.EntityB.class,
				OneToManyMappedByInEmbeddedTest.EntityC.class,
		}
)
@SessionFactory
public class OneToManyMappedByInEmbeddedTest
{

	@BeforeAll
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {

					EntityA entityA = new EntityA( 1 );

					EmbeddedValueInA embeddedValueInA = new EmbeddedValueInA();

					EntityC entityC = new EntityC();
					entityC.setId(1);
					entityC.setName("testName");

					EntityC entityC1 = new EntityC();
					entityC1.setName("testName1");
					entityC1.setId(2);

					embeddedValueInA.setEntityCList( List.of(entityC, entityC1) );
					entityA.setEmbedded( embeddedValueInA );

					session.persist( entityA );

					EntityB entityB = new EntityB(1);
					final EmbeddedValueInBWorking embedded = new EmbeddedValueInBWorking();
					embedded.setTestString("hello");
					entityB.setEmbedded(embedded);

					session.persist(entityB);

				} );
	}

	@Test
	public void testEmbeddableWithOneToManyLoadBefore(SessionFactoryScope scope) {
		scope.inTransaction(
			session -> {

				final EntityA entityA = session.get(EntityA.class, 1);
				assertThat( entityA ).isNotNull();

				final Object entityA1 =
					session.createQuery("select a.embedded from EntityA a where a.id = 1").getSingleResult();
				assertThat( entityA ).isNotNull();

			}
		);
	}

	@Test
	public void testEmbeddableWithoutOneToMany(SessionFactoryScope scope) {
		scope.inTransaction(
			session -> {

				final Object entityB =
					session.createQuery("select b.embedded from EntityB b where b.id = 1").getSingleResult();
				assertThat( entityB ).isNotNull();
			});
	}

	@Test
	public void testEmbeddableWithOneToMany(SessionFactoryScope scope) {
		scope.inTransaction(
			session -> {

				final Object entityA =
					session.createQuery("select a.embedded from EntityA a where a.id = 1").getSingleResult();
				assertThat( entityA ).isNotNull();

			}
		);
	}


	@Entity(name = "EntityA")
	public static class EntityA {
		@Id
		private Integer id;

		@Embedded
		private EmbeddedValueInA embedded = new EmbeddedValueInA();

		public EntityA() {
		}

		private EntityA(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public EmbeddedValueInA getEmbedded() {
			return embedded;
		}

		public void setEmbedded(EmbeddedValueInA embedded) {
			this.embedded = embedded;
		}
	}


	@Embeddable
	public static class EmbeddedValueInA implements Serializable {

		private String testString;
		@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
		@JoinColumn(name = "entityA_id")
		@OrderBy("id")
		private List<EntityC> entityCList;

		public EmbeddedValueInA() {
		}

		public List<EntityC> getEntityCList() {
			return entityCList;
		}

		public void setEntityCList(
				List<EntityC> entityCList) {
			this.entityCList = entityCList;
		}

		public String getTestString()
		{
			return testString;
		}

		public void setTestString(String testString)
		{
			this.testString = testString;
		}
	}

	@Entity(name = "EntityB")
	public static class EntityB {

		@Id
		private Integer id;

		@Embedded
		private EmbeddedValueInBWorking
			embedded = new EmbeddedValueInBWorking();

		public EntityB() {
		}

		private EntityB(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public EmbeddedValueInBWorking getEmbedded() {
			return embedded;
		}

		public void setEmbedded(EmbeddedValueInBWorking embedded) {
			this.embedded = embedded;
		}
	}

	@Embeddable
	public static class EmbeddedValueInBWorking implements Serializable {

		private String testString;
		public EmbeddedValueInBWorking() {
		}

		public String getTestString()
		{
			return testString;
		}

		public void setTestString(String testString)
		{
			this.testString = testString;
		}
	}


	@Entity(name = "EntityC")
	@Table(name = "t_entity_c")
	public static class EntityC {

		@Id
		private Integer id;

		private String name;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
