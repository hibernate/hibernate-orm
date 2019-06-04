/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.bytecode.enhancement.lazy.proxy;

import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.core.Is.is;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-11147")
@RunWith(BytecodeEnhancerRunner.class)
//@EnhancementOptions(lazyLoading = true)
public class SimpleUpdateTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
		ssrb.applySetting( AvailableSettings.ALLOW_ENHANCEMENT_AS_PPROXY, "true" );
		ssrb.applySetting( AvailableSettings.FORMAT_SQL, "false" );
		ssrb.applySetting( AvailableSettings.USE_SECOND_LEVEL_CACHE, "false" );
		ssrb.applySetting( AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, "true" );

	}

	private static final int CHILDREN_SIZE = 10;
	private Long lastChildID;

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Parent.class, Child.class };
	}

	@Before
	public void prepare() {
		doInHibernate( this::sessionFactory, s -> {
			Parent parent = new Parent();
			for ( int i = 0; i < CHILDREN_SIZE; i++ ) {
				Child child = new Child();
				// Association management should kick in here
				child.parent = parent;
				s.persist( child );
				lastChildID = child.id;
			}
			s.persist( parent );
		} );
	}


	@After
	public void tearDown() {
		doInHibernate( this::sessionFactory, s -> {
			s.createQuery( "delete from Child" ).executeUpdate();
			s.createQuery( "delete from Parent" ).executeUpdate();
		} );
	}

	@Test
	public void updateSimpleField() {
		String updatedName = "Barrabas";
		doInHibernate( this::sessionFactory, s -> {
			Child loadedChild = s.load( Child.class, lastChildID );
			loadedChild.setName( updatedName );
		} );

		doInHibernate( this::sessionFactory, s -> {
			Child loadedChild = s.load( Child.class, lastChildID );
			assertThat( loadedChild.getName(), is( updatedName ) );
		} );
	}

	@Test
	public void testUpdateAssociation() {
		String updatedName = "Barrabas";
		doInHibernate( this::sessionFactory, s -> {
			Child loadedChild = s.load( Child.class, lastChildID );

			loadedChild.setName( updatedName );

			Parent parent = new Parent();
			loadedChild.parent = parent;
			s.save( parent );

		} );

		doInHibernate( this::sessionFactory, s -> {
			Child loadedChild = s.load( Child.class, lastChildID );
			assertThat( loadedChild.getName(), is( updatedName ) );
		} );
	}

	@Entity(name = "Parent")
	@Table(name = "PARENT")
	private static class Parent {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		Long id;

		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		List<Child> children;

		void setChildren(List<Child> children) {
			this.children = children;
		}
	}

	@Entity(name = "Child")
	@Table(name = "CHILD")
	private static class Child {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		Long id;

		@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		@LazyToOne(LazyToOneOption.NO_PROXY)
		Parent parent;

		String name;

		Child() {
		}

		Child(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
