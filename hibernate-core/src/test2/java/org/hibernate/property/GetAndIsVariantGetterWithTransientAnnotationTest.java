/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.property;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-11716")
public class GetAndIsVariantGetterWithTransientAnnotationTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {TestEntity.class, SecondTestEntity.class};
	}

	@Test
	public void testGetAndIsVariantCanHaveDifferentReturnValueWhenOneHasATransientAnnotation() {
		TransactionUtil.doInHibernate( this::sessionFactory, session1 -> {
			TestEntity entity = new TestEntity();
			entity.setId( 1L );
			entity.setChecked( true );
			session1.save( entity );
		} );

		TransactionUtil.doInHibernate( this::sessionFactory, session1 -> {
			final TestEntity entity = session1.find( TestEntity.class, 1L );
			assertThat( entity.isChecked(), is( true ) );
		} );

		TransactionUtil.doInHibernate( this::sessionFactory, session1 -> {
			final TestEntity entity = session1.find( TestEntity.class, 1L );
			entity.setChecked( null );
		} );

		TransactionUtil.doInHibernate( this::sessionFactory, session1 -> {
			final TestEntity entity = session1.find( TestEntity.class, 1L );
			assertThat( entity.isChecked(), is( nullValue() ) );
		} );
	}

	@Test
	public void testBothGetterAndIsVariantAreIgnoredWhenMarkedTransient() {
		TransactionUtil.doInHibernate( this::sessionFactory, session1 -> {
			SecondTestEntity entity = new SecondTestEntity();
			entity.setId( 1L );
			entity.setChecked( true );
			session1.save( entity );
		} );

		TransactionUtil.doInHibernate( this::sessionFactory, session1 -> {
			final SecondTestEntity entity = session1.find( SecondTestEntity.class, 1L );
			assertThat( entity.getChecked(), is( nullValue() ) );
		} );
	}

	@Entity(name = "TestEntity")
	@Table(name = "TEST_ENTITY")
	public static class TestEntity {
		private Long id;
		private Boolean checked;
		private String name;

		@Id
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public void setChecked(Boolean checked) {
			this.checked = checked;
		}

		@Transient
		public boolean getChecked() {
			return false;
		}

		public Boolean isChecked() {
			return this.checked;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Transient
		public boolean isName() {
			return name.length() > 0;
		}
	}

	@Entity(name = "SecondTestEntity")
	@Table(name = "TEST_ENTITY_2")
	public static class SecondTestEntity {
		private Long id;
		private Boolean checked;

		@Id
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public void setChecked(Boolean checked) {
			this.checked = checked;
		}

		@Transient
		public Boolean getChecked() {
			return this.checked;
		}

		@Transient
		public boolean isChecked() {
			return false;
		}
	}

}
