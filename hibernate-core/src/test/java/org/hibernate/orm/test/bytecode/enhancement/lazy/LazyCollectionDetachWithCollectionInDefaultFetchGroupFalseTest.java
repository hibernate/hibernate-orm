/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.Hibernate.isPropertyInitialized;
import static org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils.checkDirtyTracking;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.boot.internal.SessionFactoryBuilderImpl;
import org.hibernate.boot.internal.SessionFactoryOptionsBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.SessionFactoryBuilderService;
import org.hibernate.orm.test.bytecode.enhancement.dirty.DirtyTrackingCollectionInDefaultFetchGroupTest;
import org.hibernate.proxy.HibernateProxy;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * Same as {@link LazyCollectionDetachTest},
 * but with {@code collectionInDefaultFetchGroup} set to {@code false} explicitly.
 * <p>
 * Kept here for <a href="https://github.com/hibernate/hibernate-orm/pull/5252#pullrequestreview-1095843220">historical reasons</a>.
 */
@TestForIssue(jiraKey = "HHH-12260")
@RunWith(BytecodeEnhancerRunner.class)
public class LazyCollectionDetachWithCollectionInDefaultFetchGroupFalseTest extends BaseCoreFunctionalTestCase {

	private static final int CHILDREN_SIZE = 10;
	private Long parentID;

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ Parent.class, Child.class };
	}

	@Override
	protected void prepareBasicRegistryBuilder(StandardServiceRegistryBuilder serviceRegistryBuilder) {
		serviceRegistryBuilder.addService(
				SessionFactoryBuilderService.class,
				(SessionFactoryBuilderService) (metadata, bootstrapContext) -> {
					SessionFactoryOptionsBuilder optionsBuilder = new SessionFactoryOptionsBuilder(
							metadata.getMetadataBuildingOptions().getServiceRegistry(),
							bootstrapContext
					);
					// We want to test with this setting set to false explicitly,
					// because another test already takes care of the default.
					optionsBuilder.enableCollectionInDefaultFetchGroup( false );
					return new SessionFactoryBuilderImpl( metadata, optionsBuilder );
				}
		);
	}

	@Before
	public void prepare() {
		doInHibernate( this::sessionFactory, s -> {
			Parent parent = new Parent();
			parent.setChildren( new ArrayList<>() );
			for ( int i = 0; i < CHILDREN_SIZE; i++ ) {
				Child child = new Child();
				child.parent = parent;
				s.persist( child );
			}
			s.persist( parent );
			parentID = parent.id;
		} );
	}

	@Test
	public void testDetach() {
		doInHibernate( this::sessionFactory, s -> {
			Parent parent = s.find( Parent.class, parentID );

			assertThat( parent, notNullValue() );
			assertThat( parent, not( instanceOf( HibernateProxy.class ) ) );
			assertFalse( isPropertyInitialized( parent, "children" ) );
			checkDirtyTracking( parent );

			s.detach( parent );

			s.flush();
		} );
	}

	@Test
	public void testDetachProxy() {
		doInHibernate( this::sessionFactory, s -> {
			Parent parent = s.getReference( Parent.class, parentID );

			checkDirtyTracking( parent );

			s.detach( parent );

			s.flush();
		} );
	}

	@Test
	public void testRefresh() {
		doInHibernate( this::sessionFactory, s -> {
			Parent parent = s.find( Parent.class, parentID );

			assertThat( parent, notNullValue() );
			assertThat( parent, not( instanceOf( HibernateProxy.class ) ) );
			assertFalse( isPropertyInitialized( parent, "children" ) );
			checkDirtyTracking( parent );

			s.refresh( parent );

			s.flush();
		} );
	}


	@Entity(name = "Parent")
	@Table(name = "PARENT")
	private static class Parent {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		Long id;

		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
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
		Parent parent;

		Child() {
		}
	}
}
