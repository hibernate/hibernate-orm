package org.hibernate.test.jpa;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.event.def.DefaultPersistEventListener;
import org.hibernate.event.def.DefaultAutoFlushEventListener;
import org.hibernate.event.def.DefaultFlushEventListener;
import org.hibernate.event.def.DefaultFlushEntityEventListener;
import org.hibernate.event.AutoFlushEventListener;
import org.hibernate.event.FlushEventListener;
import org.hibernate.event.PersistEventListener;
import org.hibernate.event.FlushEntityEventListener;
import org.hibernate.engine.CascadingAction;
import org.hibernate.util.IdentityMap;
import org.hibernate.junit.functional.FunctionalTestCase;

import java.io.Serializable;

/**
 * An abstract test for all JPA spec related tests.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractJPATest extends FunctionalTestCase {
	public AbstractJPATest(String name) {
		super( name );
	}

	public String[] getMappings() {
		return new String[] { "jpa/Part.hbm.xml", "jpa/Item.hbm.xml", "jpa/MyEntity.hbm.xml" };
	}

	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.JPAQL_STRICT_COMPLIANCE, "true" );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "false" );
		cfg.setEntityNotFoundDelegate( new JPAEntityNotFoundDelegate() );
		cfg.getEventListeners().setPersistEventListeners( buildPersistEventListeners() );
		cfg.getEventListeners().setPersistOnFlushEventListeners( buildPersisOnFlushEventListeners() );
		cfg.getEventListeners().setAutoFlushEventListeners( buildAutoFlushEventListeners() );
		cfg.getEventListeners().setFlushEventListeners( buildFlushEventListeners() );
		cfg.getEventListeners().setFlushEntityEventListeners( buildFlushEntityEventListeners() );
	}

	public String getCacheConcurrencyStrategy() {
		// no second level caching
		return null;
	}


	// mimic specific exception aspects of the JPA environment ~~~~~~~~~~~~~~~~

	private static class JPAEntityNotFoundDelegate implements EntityNotFoundDelegate {
		public void handleEntityNotFound(String entityName, Serializable id) {
			throw new EntityNotFoundException( entityName, id );
		}
	}

	/**
	 * Mimic the JPA EntityNotFoundException.
	 */
	public static class EntityNotFoundException extends RuntimeException {
		private final String entityName;
		private final Serializable id;

		public EntityNotFoundException(String entityName, Serializable id) {
			this( "unable to locate specified entity", entityName, id );
		}

		public EntityNotFoundException(String message, String entityName, Serializable id) {
			super( message );
			this.entityName = entityName;
			this.id = id;
		}

		public String getEntityName() {
			return entityName;
		}

		public Serializable getId() {
			return id;
		}
	}


	// mimic specific event aspects of the JPA environment ~~~~~~~~~~~~~~~~~~~~

	protected PersistEventListener[] buildPersistEventListeners() {
		return new PersistEventListener[] { new JPAPersistEventListener() };
	}

	protected PersistEventListener[] buildPersisOnFlushEventListeners() {
		return new PersistEventListener[] { new JPAPersistOnFlushEventListener() };
	}

	protected AutoFlushEventListener[] buildAutoFlushEventListeners() {
		return new AutoFlushEventListener[] { JPAAutoFlushEventListener.INSTANCE };
	}

	protected FlushEventListener[] buildFlushEventListeners() {
		return new FlushEventListener[] { JPAFlushEventListener.INSTANCE };
	}

	protected FlushEntityEventListener[] buildFlushEntityEventListeners() {
		return new FlushEntityEventListener[] { new JPAFlushEntityEventListener() };
	}

	public static class JPAPersistEventListener extends DefaultPersistEventListener {
		// overridden in JPA impl for entity callbacks...
	}

	public static class JPAPersistOnFlushEventListener extends JPAPersistEventListener {
		protected CascadingAction getCascadeAction() {
			return CascadingAction.PERSIST_ON_FLUSH;
		}
	}

	public static class JPAAutoFlushEventListener extends DefaultAutoFlushEventListener {
		// not sure why EM code has this ...
		public static final AutoFlushEventListener INSTANCE = new JPAAutoFlushEventListener();

		protected CascadingAction getCascadingAction() {
			return CascadingAction.PERSIST_ON_FLUSH;
		}

		protected Object getAnything() {
			return IdentityMap.instantiate( 10 );
		}
	}

	public static class JPAFlushEventListener extends DefaultFlushEventListener {
		// not sure why EM code has this ...
		public static final FlushEventListener INSTANCE = new JPAFlushEventListener();

		protected CascadingAction getCascadingAction() {
			return CascadingAction.PERSIST_ON_FLUSH;
		}

		protected Object getAnything() {
			return IdentityMap.instantiate( 10 );
		}
	}

	public static class JPAFlushEntityEventListener extends DefaultFlushEntityEventListener {
		// in JPA, used mainly for preUpdate callbacks...
	}
}
