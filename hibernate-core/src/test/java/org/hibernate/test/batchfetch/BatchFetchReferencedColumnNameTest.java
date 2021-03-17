/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.batchfetch;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

import java.time.ZonedDateTime;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;

public class BatchFetchReferencedColumnNameTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ Child.class, Parent.class };
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );

		configuration.setProperty( AvailableSettings.SHOW_SQL, Boolean.TRUE.toString() );
		configuration.setProperty( AvailableSettings.FORMAT_SQL, Boolean.TRUE.toString() );

		configuration.setProperty( AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, "64" );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-13059")
	public void test() throws Exception {
		doInHibernate( this::sessionFactory, session -> {
			Parent p = new Parent();
			p.setId( 1L );
			session.save( p );

			Child c1 = new Child();
			c1.setCreatedOn( ZonedDateTime.now() );
			c1.setParentId( 1L );
			c1.setId( 10L );
			session.save( c1 );

			Child c2 = new Child();
			c2.setCreatedOn( ZonedDateTime.now() );
			c2.setParentId( 1L );
			c2.setId( 11L );
			session.save( c2 );
		} );

		doInHibernate( this::sessionFactory, session -> {
			Parent p = session.get( Parent.class, 1L );
			Assert.assertNotNull( p );

			Assert.assertEquals( 2, p.getChildren().size() );
		} );
	}

	@Entity
	@Table(name = "CHILD")
	public static class Child {

		@Id
		@Column(name = "CHILD_ID")
		private Long id;

		@Column(name = "PARENT_ID")
		private Long parentId;

		@Column(name = "CREATED_ON")
		private ZonedDateTime createdOn;

		public ZonedDateTime getCreatedOn() {
			return createdOn;
		}

		public void setCreatedOn(ZonedDateTime createdOn) {
			this.createdOn = createdOn;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Long getParentId() {
			return parentId;
		}

		public void setParentId(Long parentId) {
			this.parentId = parentId;
		}
	}

	@Entity
	@Table(name = "PARENT")
	public static class Parent {

		@Id
		@Column(name = "PARENT_ID")
		private Long id;

		@OneToMany(fetch = FetchType.EAGER, cascade = { CascadeType.ALL })
		@JoinColumn(name = "PARENT_ID", referencedColumnName = "PARENT_ID")
		@OrderBy("createdOn desc")
		private List<Child> children;

		public List<Child> getChildren() {
			return children;
		}

		public void setChildren(List<Child> children) {
			this.children = children;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

	}
}
