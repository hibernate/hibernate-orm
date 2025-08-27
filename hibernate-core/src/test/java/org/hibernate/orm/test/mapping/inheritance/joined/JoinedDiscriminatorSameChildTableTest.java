/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.inheritance.joined;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {
		JoinedDiscriminatorSameChildTableTest.EntityParent.class,
		JoinedDiscriminatorSameChildTableTest.EntityChildOne.class,
		JoinedDiscriminatorSameChildTableTest.EntityChildTwo.class,
		JoinedDiscriminatorSameChildTableTest.EntityRelation.class,
})
@SessionFactory(useCollectingStatementInspector = true)
@Jira("https://hibernate.atlassian.net/browse/HHH-19457")
public class JoinedDiscriminatorSameChildTableTest {
	@Test
	public void testParents(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();

		scope.inSession( session -> {
			final EntityRelation relation = session.find( EntityRelation.class, "relation_1" );
			inspector.clear();
			assertThat( relation.getParents() ).hasSize( 2 );
		} );
		// no need to filter by discriminator column, as we're selecting all subtypes of EntityParent
		inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "disc_col", 1 );

		scope.inSession( session -> {
			final EntityRelation relation = session.createQuery(
					"from EntityRelation where id = 'relation_1'",
					EntityRelation.class
			).getSingleResult();
			inspector.clear();
			assertThat( relation.getParents() ).hasSize( 2 );
		} );
		inspector.assertNumberOfOccurrenceInQueryNoSpace( 0, "disc_col", 1 );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityRelation relation = new EntityRelation();
			relation.setId( "relation_1" );
			session.persist( relation );

			final EntityChildOne c1 = new EntityChildOne();
			c1.setId( "child_1" );
			c1.setIdRelation( "relation_1" );
			session.persist( c1 );

			final EntityChildTwo c2 = new EntityChildTwo();
			c2.setId( "child_2" );
			c2.setIdRelation( "relation_1" );
			session.persist( c2 );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Entity(name = "EntityParent")
	@Table(name = "parent_table")
	@Inheritance(strategy = InheritanceType.JOINED)
	@DiscriminatorColumn(name = "disc_col")
	static abstract class EntityParent {
		@Id
		@Column(name = "id")
		private String id;

		@Column(name = "id_relation")
		private String idRelation;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "id_relation", referencedColumnName = "id", insertable = false, updatable = false)
		private EntityRelation relation;

		public EntityRelation getRelation() {
			return relation;
		}

		public void setRelation(EntityRelation requisition) {
			this.relation = requisition;
		}

		public String getIdRelation() {
			return idRelation;
		}

		public void setIdRelation(String idRelation) {
			this.idRelation = idRelation;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}
	}

	@Entity(name = "EntityRelation")
	static class EntityRelation {
		@Id
		private String id;

		@OneToMany(fetch = FetchType.LAZY, mappedBy = "relation")
		private List<EntityParent> parents;

		public void setId(String id) {
			this.id = id;
		}

		public String getId() {
			return id;
		}

		public List<EntityParent> getParents() {
			return parents;
		}
	}

	@Entity(name = "EntityChildOne")
	@Table(name = "child_table")
	@DiscriminatorValue("child-one")
	static class EntityChildOne extends EntityParent {
		private String name;
	}

	@Entity(name = "EntityChildTwo")
	@Table(name = "child_table")
	@DiscriminatorValue("child-two")
	static class EntityChildTwo extends EntityParent {
		private Integer age;
	}
}
