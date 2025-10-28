/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.group;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.annotations.LazyGroup;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.proxy.HibernateProxy;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@JiraKey( "HHH-11155" )
@DomainModel(
		annotatedClasses = {
				LazyGroupTest.Child.class, LazyGroupTest.Parent.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false" ),
				@Setting( name = AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, value = "true" ),
		}
)
@SessionFactory
@BytecodeEnhanced
public class LazyGroupTest {

	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			Child c1 = new Child( "steve", "Hibernater" );
			Child c2 = new Child( "sally", "Joe Mama" );

			Parent p1 = new Parent( "Hibernate" );
			Parent p2 = new Parent( "Swimming" );

			c1.parent = p1;
			p1.children.add( c1 );

			c1.alternateParent = p2;
			p2.alternateChildren.add( c1 );

			c2.parent = p2;
			p2.children.add( c2 );

			c2.alternateParent = p1;
			p1.alternateChildren.add( c2 );

			s.persist( p1 );
			s.persist( p2 );
		} );
	}

	@Test
	@JiraKey( "HHH-10267" )
	public void testAccess(SessionFactoryScope scope) {
		scope.inTransaction(
				(s) -> {
					SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getSessionFactory()
							.getSessionFactoryOptions()
							.getStatementInspector();
					statementInspector.clear();

					final Child c1 = s.createQuery( "from Child c where c.name = :name", Child.class )
							.setParameter( "name", "steve" )
							.uniqueResult();

					statementInspector.assertExecutedCount( 1 );

					assertTrue( Hibernate.isPropertyInitialized( c1, "name" ) );

					assertFalse( Hibernate.isPropertyInitialized( c1, "nickName" ) );

					// parent should be an uninitialized enhanced-proxy
					assertTrue( Hibernate.isPropertyInitialized( c1, "parent" ) );
					assertThat( c1.getParent() ).isNotInstanceOf( HibernateProxy.class );
					assertFalse( Hibernate.isInitialized( c1.getParent() ) );

					// alternateParent should be an uninitialized enhanced-proxy
					assertTrue( Hibernate.isPropertyInitialized( c1, "alternateParent" ) );
					assertThat( c1.getAlternateParent() ).isNotInstanceOf( HibernateProxy.class );
					assertFalse( Hibernate.isInitialized( c1.getAlternateParent() ) );

					// Now lets access nickName which ought to initialize nickName
					c1.getNickName();
					statementInspector.assertExecutedCount( 2 );

					assertTrue( Hibernate.isPropertyInitialized( c1, "nickName" ) );

					// parent should be an uninitialized enhanced-proxy
					assertTrue( Hibernate.isPropertyInitialized( c1, "parent" ) );
					assertThat( c1.getParent() ).isNotInstanceOf( HibernateProxy.class );
					assertFalse( Hibernate.isInitialized( c1.getParent() ) );

					// alternateParent should be an uninitialized enhanced-proxy
					assertTrue( Hibernate.isPropertyInitialized( c1, "alternateParent" ) );
					assertThat( c1.getAlternateParent() ).isNotInstanceOf( HibernateProxy.class );
					assertFalse( Hibernate.isInitialized( c1.getAlternateParent() ) );
				}
		);
	}

	@Test
	@JiraKey( "HHH-11155" )
	public void testUpdate(SessionFactoryScope scope) {
		Parent p1New = new Parent();
		p1New.nombre = "p1New";

		scope.inTransaction(
				(s) -> {
					SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getSessionFactory()
							.getSessionFactoryOptions()
							.getStatementInspector();
					statementInspector.clear();

					final Child c1 = s.createQuery( "from Child c where c.name = :name", Child.class )
							.setParameter( "name", "steve" )
							.uniqueResult();
					statementInspector.assertExecutedCount( 1 );

					assertTrue( Hibernate.isPropertyInitialized( c1, "name" ) );

					assertFalse( Hibernate.isPropertyInitialized( c1, "nickName" ) );

					// parent should be an uninitialized enhanced-proxy
					assertTrue( Hibernate.isPropertyInitialized( c1, "parent" ) );
					assertThat( c1.getParent() ).isNotInstanceOf( HibernateProxy.class );
					assertFalse( Hibernate.isInitialized( c1.getParent() ) );

					// alternateParent should be an uninitialized enhanced-proxy
					assertTrue( Hibernate.isPropertyInitialized( c1, "alternateParent" ) );
					assertThat( c1.getAlternateParent() ).isNotInstanceOf( HibernateProxy.class );
					assertFalse( Hibernate.isInitialized( c1.getAlternateParent() ) );

					// Now lets update nickName
					c1.nickName = "new nickName";

					statementInspector.assertExecutedCount( 1 );

					assertTrue( Hibernate.isPropertyInitialized( c1, "name" ) );

					assertTrue( Hibernate.isPropertyInitialized( c1, "nickName" ) );

					// parent should be an uninitialized enhanced-proxy
					assertTrue( Hibernate.isPropertyInitialized( c1, "parent" ) );
					assertThat( c1.getParent() ).isNotInstanceOf( HibernateProxy.class );
					assertFalse( Hibernate.isInitialized( c1.getParent() ) );

					// alternateParent should be an uninitialized enhanced-proxy
					assertTrue( Hibernate.isPropertyInitialized( c1, "alternateParent" ) );
					assertThat( c1.getAlternateParent() ).isNotInstanceOf( HibernateProxy.class );
					assertFalse( Hibernate.isInitialized( c1.getAlternateParent() ) );

					// Now update c1.parent
					c1.parent.children.remove( c1 );
					c1.parent = p1New;
					p1New.children.add( c1 );
				}
		);

		// verify updates
		scope.inTransaction(
				(s) -> {
					final Child c1 = s.createQuery( "from Child c where c.name = :name", Child.class )
							.setParameter( "name", "steve" )
							.uniqueResult();

					assertThat( c1.getNickName() ).isEqualTo( "new nickName" );
					assertThat( c1.parent.nombre ).isEqualTo( "p1New" );
				}
		);
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	// --- //

	// --- //

	@Entity( name = "Parent" )
	@Table( name = "PARENT" )
	static class Parent {
		@Id
		@GeneratedValue( strategy = GenerationType.AUTO )
		Long id;

		String nombre;

		@OneToMany( mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY )
		List<Child> children = new ArrayList<>();

		@OneToMany( mappedBy = "alternateParent", cascade = CascadeType.ALL, fetch = FetchType.LAZY )
		List<Child> alternateChildren = new ArrayList<>();

		Parent() {
		}

		Parent(String nombre) {
			this.nombre = nombre;
		}
	}

	@Entity( name = "Child" )
	@Table( name = "CHILD" )
	static class Child {

		@Id
		@GeneratedValue( strategy = GenerationType.AUTO )
		Long id;

		String name;

		@Basic( fetch = FetchType.LAZY )
		String nickName;

		@ManyToOne( cascade = CascadeType.ALL, fetch = FetchType.LAZY )
		Parent parent;

		@ManyToOne( cascade = CascadeType.ALL, fetch = FetchType.LAZY )
		@LazyGroup( "SECONDARY" )
		Parent alternateParent;

		Child() {
		}

		Child(String name, String nickName) {
			this.name = name;
			this.nickName = nickName;
		}

		public Parent getParent() {
			return parent;
		}

		Parent getAlternateParent() {
			return alternateParent;
		}

		String getNickName() {
			return nickName;
		}
	}
}
