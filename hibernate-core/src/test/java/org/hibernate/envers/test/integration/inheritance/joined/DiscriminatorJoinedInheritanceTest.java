/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.inheritance.joined;

import java.util.Arrays;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;

import org.hibernate.envers.Audited;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-11133")
public class DiscriminatorJoinedInheritanceTest extends BaseEnversJPAFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { ParentEntity.class, ChildEntity.class, ChildListHolder.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager entityManager = getEntityManager();
		try {
			ChildEntity childEntity = new ChildEntity( 1, "Child" );
			entityManager.getTransaction().begin();
			entityManager.persist( childEntity );
			entityManager.getTransaction().commit();

			ChildListHolder holder = new ChildListHolder();
			holder.setId( 1 );
			holder.setChildren( Arrays.asList( childEntity ) );
			entityManager.getTransaction().begin();
			entityManager.persist( holder );
			entityManager.getTransaction().commit();

		}
		catch ( Exception e ) {
			if ( entityManager.getTransaction().isActive() ) {
				entityManager.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			entityManager.close();
		}
	}

	@Test
	public void testRevisionCounts() {
		assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( ChildEntity.class, 1 ) );
		assertEquals( Arrays.asList( 2 ), getAuditReader().getRevisions( ChildListHolder.class, 1 ) );
	}

	@Test
	public void testConfiguredDiscriminatorValue() {
		ChildEntity entity = getAuditReader().find( ChildEntity.class, 1, 1 );
		assertEquals( "ce", entity.getType() );
	}

	@Test
	public void testDiscriminatorValuesViaRelatedEntityQuery() {
		ChildListHolder holder = getAuditReader().find( ChildListHolder.class, 1, 2 );
		assertEquals( 1, holder.getChildren().size() );
		assertEquals( "ce", holder.getChildren().get( 0 ).getType() );
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
