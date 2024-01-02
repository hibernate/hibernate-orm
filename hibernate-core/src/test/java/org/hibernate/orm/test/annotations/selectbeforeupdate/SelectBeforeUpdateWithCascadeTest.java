package org.hibernate.orm.test.annotations.selectbeforeupdate;

import java.util.List;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.orm.test.legacy.Child;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hibernate.annotations.CascadeType.SAVE_UPDATE;

@DomainModel(
		annotatedClasses = {
				SelectBeforeUpdateWithCascadeTest.ChildEntity.class,
				SelectBeforeUpdateWithCascadeTest.MappedEntity.class,
				SelectBeforeUpdateWithCascadeTest.ParentEntity.class
		}
)
@SessionFactory
@JiraKey("HHH-17560")
public class SelectBeforeUpdateWithCascadeTest {

	@Test
	public void testPersist(SessionFactoryScope scope) {

		ChildEntity childEntity = new ChildEntity();
		scope.inTransaction(
				session -> {
					childEntity.setAProperty( "property" );
					childEntity.setNonInsertableProperty( "nonInsertable" );
					childEntity.setNonUpdatableProperty( "nonUpdatable" );

					session.persist( childEntity );
				}
		);

		scope.inTransaction(
				session -> {
					ParentEntity parent1 = new ParentEntity();
					parent1.setChildEntity( childEntity );

					session.persist( parent1 );
				}
		);

		scope.inTransaction(
				session -> {
					ParentEntity parent2 = new ParentEntity();
					parent2.setChildEntity( childEntity );

					session.persist( parent2 );
				}
		);

		scope.inTransaction(
				session -> {
					List<ChildEntity> children = session.createQuery( "select c from ChildEntity c" ).list();
					assertThat(children.size()).isEqualTo( 1 );

					List<ParentEntity> parents = session.createQuery( "select p from ParentEntity p" ).list();
					assertThat(parents.size()).isEqualTo( 2 );

				}
		);
	}

	@Entity(name = "ChildEntity")
	@SelectBeforeUpdate
	public static class ChildEntity {
		@Id
		@GeneratedValue
		private Long id;

		private String aProperty;

		@Column(updatable = false)
		private String nonUpdatableProperty;

		@Column(insertable = false)
		private String nonInsertableProperty;

		@OneToOne(mappedBy = "childEntity")
		private MappedEntity mappedEntity;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getAProperty() {
			return aProperty;
		}

		public void setAProperty(String property) {
			this.aProperty = property;
		}

		public MappedEntity getMappedEntity() {
			return mappedEntity;
		}

		public void setMappedEntity(MappedEntity mappedEntity) {
			this.mappedEntity = mappedEntity;
		}

		public String getNonUpdatableProperty() {
			return nonUpdatableProperty;
		}

		public void setNonUpdatableProperty(String nonUpdatableProperty) {
			this.nonUpdatableProperty = nonUpdatableProperty;
		}

		public String getNonInsertableProperty() {
			return nonInsertableProperty;
		}

		public void setNonInsertableProperty(String nonInsertableProperty) {
			this.nonInsertableProperty = nonInsertableProperty;
		}
	}

	@Entity(name = "MappedEntity")
	public static class MappedEntity {
		@Id
		@GeneratedValue
		private Long id;

		@OneToOne
		@JoinColumn(name = "child_entity_id", updatable = false)
		private ChildEntity childEntity;

		private String property;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public ChildEntity getChildEntity() {
			return childEntity;
		}

		public void setChildEntity(ChildEntity childEntity) {
			this.childEntity = childEntity;
		}

		public String getProperty() {
			return property;
		}

		public void setProperty(String property) {
			this.property = property;
		}
	}

	@Entity(name = "ParentEntity")
	public static class ParentEntity {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne
		@Cascade(SAVE_UPDATE)
		@JoinColumn(name = "child_entity_id")
		private ChildEntity childEntity;



		public void setId(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public ChildEntity getChildEntity() {
			return childEntity;
		}

		public void setChildEntity(ChildEntity childEntity) {
			this.childEntity = childEntity;
		}
	}
}
