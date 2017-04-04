/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.LoadEvent;
import org.hibernate.event.spi.LoadEventListener;
import org.hibernate.jpa.event.internal.core.JpaFlushEventListener;

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

		EntityManager em = getFactory().createEntityManager();
		em.getTransaction().begin();

		Child c = new Child();
		em.persist( c );

		Parent parent = new Parent();
		parent.setChild( c );
		em.persist( parent );
		parentID = parent.getId();

		em.getTransaction().commit();
		em.clear();
		em.close();

	}

	public void execute() {
		EventListenerRegistry registry = getFactory().unwrap( SessionFactoryImplementor.class ).getServiceRegistry().getService( EventListenerRegistry.class );
		registry.prependListeners( EventType.FLUSH, new JpaFlushEventListener() );
		registry.prependListeners( EventType.LOAD, new ImmediateLoadTrap() );

		EntityManager em = getFactory().createEntityManager();
		em.getTransaction().begin();

		Parent p = em.find(Parent.class, parentID);
		em.flush(); // unwanted lazy load occurs here

		em.getTransaction().commit();
		em.close();
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
