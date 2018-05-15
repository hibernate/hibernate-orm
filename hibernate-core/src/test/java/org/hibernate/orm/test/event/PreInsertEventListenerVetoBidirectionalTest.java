/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.event;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import org.hibernate.action.internal.EntityActionVetoException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.orm.test.SessionFactoryBasedFunctionalTest;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.DialectFeatureChecks;
import org.hibernate.testing.junit5.ExpectedException;
import org.hibernate.testing.junit5.FailureExpected;
import org.hibernate.testing.junit5.RequiresDialectFeature;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.fail;

/**
 * @author Chris Cranford
 */
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsIdentityColumns.class)
@TestForIssue(jiraKey = "HHH-11721")
@FailureExpected( value= "fetching database snapshot not yet implemented" )
public class PreInsertEventListenerVetoBidirectionalTest extends SessionFactoryBasedFunctionalTest {
	@Override
	protected void applyMetadataSources(MetadataSources metadataSources) {
		super.applyMetadataSources( metadataSources );
		metadataSources.addAnnotatedClass( Child.class );
		metadataSources.addAnnotatedClass( Parent.class );
	}

	@Override
	protected boolean exportSchema() {
		return true;
	}


	@AfterAll
	protected void afterSessionFactoryBuilt() {
		EventListenerRegistry registry = sessionFactory().getServiceRegistry()
				.getService( EventListenerRegistry.class );
		registry.appendListeners(
				EventType.PRE_INSERT,
				(PreInsertEventListener) event -> event.getEntity() instanceof Parent
		);
	}

	@Test
	@ExpectedException(value = EntityActionVetoException.class  )
	public void testVeto() {
		sessionFactoryScope().inTransaction( session -> {
			Parent parent = new Parent();
			parent.setField1( "f1" );
			parent.setfield2( "f2" );

			Child child = new Child();
			parent.setChild( child );

			session.save( parent );
		} );

		fail( "Should have thrown EntityActionVetoException!" );
	}

	@Entity(name = "Child")
	public static class Child {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Integer id;

		@OneToOne
		private Parent parent;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}
	}

	@Entity(name = "Parent")
	public static class Parent {

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private Integer id;

		private String field1;

		private String field2;

		@OneToOne(cascade = CascadeType.ALL, mappedBy = "parent")
		private Child child;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getField1() {
			return field1;
		}

		public void setField1(String field1) {
			this.field1 = field1;
		}

		public String getField2() {
			return field2;
		}

		public void setfield2(String field2) {
			this.field2 = field2;
		}

		public Child getChild() {
			return child;
		}

		public void setChild(Child child) {
			this.child = child;
			child.setParent( this );
		}
	}
}
