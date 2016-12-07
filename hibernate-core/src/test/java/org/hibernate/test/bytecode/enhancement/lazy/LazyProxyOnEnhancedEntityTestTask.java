/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.CascadingAction;
import org.hibernate.engine.spi.CascadingActions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.internal.DefaultFlushEventListener;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.LoadEvent;
import org.hibernate.event.spi.LoadEventListener;

import org.hibernate.test.bytecode.enhancement.AbstractEnhancerTestTask;

/**
 * @author Luis Barreiro
 */
public class LazyProxyOnEnhancedEntityTestTask extends AbstractEnhancerTestTask {

	private Long parentID;

	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {Parent.class, Child.class};
	}

	public void prepare() {
		super.prepare( new Configuration() );

		Session s = getFactory().openSession();
		s.getTransaction().begin();

		Child c = new Child();
		s.persist( c );

		Parent parent = new Parent();
		parent.setChild( c );
		s.persist( parent );
		parentID = parent.getId();

		s.getTransaction().commit();
		s.clear();
		s.close();

	}

	public void execute() {
		EventListenerRegistry registry = ( (SessionFactoryImplementor) getFactory() ).getServiceRegistry().getService( EventListenerRegistry.class );

		// The 5.2 version of this test uses JpaFlushEventListener and EntityManager to reproduce the issue.
		// EntityManager and JpaFlushEventListener is not available in hibernate-core 5.0/5.1.
		// This issue can be reproduced for 5.0/5.1 in hibernate-core by overriding DefaultFlushEventListener to use
		// CascadingActions.PERSIST_ON_FLUSH.
		registry.prependListeners(
				EventType.FLUSH,
				new DefaultFlushEventListener() {
					@Override
					protected CascadingAction getCascadingAction() {
						return CascadingActions.PERSIST_ON_FLUSH;}

				}
		);
		registry.prependListeners( EventType.LOAD, new ImmediateLoadTrap() );

		Session s = getFactory().openSession();
		s.getTransaction().begin();

		Parent p = s.get( Parent.class, parentID );
		s.flush(); // unwanted lazy load occurs here

		s.getTransaction().commit();
		s.close();
	}

	protected void cleanup() {
	}

	private static class ImmediateLoadTrap implements LoadEventListener {
		@Override
		public void onLoad(LoadEvent event, LoadEventListener.LoadType loadType) throws HibernateException {
			if ( loadType == IMMEDIATE_LOAD ) {
				String msg = loadType + ":" + event.getEntityClassName() + "#" + event.getEntityId();
				throw new RuntimeException(msg);
			}
		}
	}

	@Entity
	@Table(name = "LazyProxyTask_Parent")
	public static class Parent {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		@OneToOne(fetch = FetchType.LAZY
		)
		private Child child;

		public Long getId() {
			return id;
		}

		public Child getChild() {
			return child;
		}

		public void setChild(Child child) {
			this.child = child;
		}
	}

	@Entity
	@Table(name = "LazyProxyTask_Child")
	public static class Child {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		private String name;

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

}
