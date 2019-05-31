/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.inheritance.joined;

import java.util.Arrays;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.envers.Audited;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Chris Cranford
 */
@Disabled("NYI - Joined Inheritance Support")
@TestForIssue(jiraKey = "HHH-11133")
public class DiscriminatorJoinedInheritanceTest extends EnversEntityManagerFactoryBasedFunctionalTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { ParentEntity.class, ChildEntity.class, ChildListHolder.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		ChildEntity childEntity = new ChildEntity( 1, "Child" );
		inTransactions(
				entityManager -> {
					entityManager.persist( childEntity );
				},

				entityManager -> {
					ChildListHolder holder = new ChildListHolder();
					holder.setId( 1 );
					holder.setChildren( Arrays.asList( childEntity ) );
					entityManager.persist( holder );
				}
		);
	}

	@DynamicTest
	public void testRevisionCounts() {
		assertThat( getAuditReader().getRevisions( ChildEntity.class, 1 ), contains( 1 ) );
		assertThat( getAuditReader().getRevisions( ChildListHolder.class, 1 ), contains( 2 ) );
	}

	@DynamicTest
	public void testConfiguredDiscriminatorValue() {
		ChildEntity entity = getAuditReader().find( ChildEntity.class, 1, 1 );
		assertThat( entity.getType(), equalTo( "ce" ) );
	}

	@DynamicTest
	public void testDiscriminatorValuesViaRelatedEntityQuery() {
		ChildListHolder holder = getAuditReader().find( ChildListHolder.class, 1, 2 );
		assertThat( holder.getChildren(), CollectionMatchers.hasSize( 1 ) );
		assertThat( holder.getChildren().get( 0 ).getType(), equalTo( "ce" ) );
	}

	@Entity(name = "ParentEntity")
	@Audited
	@Inheritance(strategy = InheritanceType.JOINED)
	@DiscriminatorValue("pe")
	@DiscriminatorColumn(name = "type", length = 255)
	public static abstract class ParentEntity {
		@Id
		private Integer id;

		@Column(insertable = false, updatable = false)
		private String type;

		ParentEntity() {

		}

		ParentEntity(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getType() {
			return type;
		}

		private void setType(String type) {
			this.type = type;
		}
	}

	@Entity(name = "ChildEntity")
	@Audited
	@DiscriminatorValue("ce")
	public static class ChildEntity extends ParentEntity {
		private String name;

		ChildEntity() {

		}

		ChildEntity(Integer id, String name) {
			super( id );
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "ChildListHolder")
	@Table(name = "CHILD_HOLDER")
	@Audited
	public static class ChildListHolder {
		@Id
		private Integer id;
		@OneToMany
		private List<ChildEntity> children;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public List<ChildEntity> getChildren() {
			return children;
		}

		public void setChildren(List<ChildEntity> children) {
			this.children = children;
		}
	}
}
