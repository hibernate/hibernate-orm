//$Id: SessionFactoryStub.java 8754 2005-12-05 23:36:59Z steveebersole $
package org.hibernate.jmx;

import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.sql.Connection;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.Interceptor;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Cache;
import org.hibernate.engine.FilterDefinition;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.UUIDHexGenerator;
import org.hibernate.impl.SessionFactoryObjectFactory;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metadata.CollectionMetadata;
import org.hibernate.stat.Statistics;

/**
 * A flyweight for <tt>SessionFactory</tt>. If the MBean itself does not
 * have classpath to the persistent classes, then a stub will be registered
 * with JNDI and the actual <tt>SessionFactoryImpl</tt> built upon first
 * access.
 * @author Gavin King
 */
public class SessionFactoryStub implements SessionFactory {

	private static final Logger log = LoggerFactory.getLogger(SessionFactoryStub.class);

	private static final IdentifierGenerator UUID_GENERATOR = new UUIDHexGenerator();

	private transient SessionFactory impl;
	private transient HibernateService service;
	private String uuid;
	private String name;

	SessionFactoryStub(HibernateService service) {
		this.service = service;
		this.name = service.getJndiName();
		try {
			uuid = (String) UUID_GENERATOR.generate(null, null);
		}
		catch (Exception e) {
			throw new AssertionFailure("Could not generate UUID");
		}

		SessionFactoryObjectFactory.addInstance( uuid, name, this, service.getProperties() );
	}

	public org.hibernate.classic.Session openSession(Connection connection, Interceptor interceptor) {
		return getImpl().openSession(connection, interceptor);
	}

	public org.hibernate.classic.Session openSession(Interceptor interceptor) throws HibernateException {
		return getImpl().openSession(interceptor);
	}

	public org.hibernate.classic.Session openSession() throws HibernateException {
		return getImpl().openSession();
	}
	
	public org.hibernate.classic.Session openSession(Connection conn) {
		return getImpl().openSession(conn);
	}

	public org.hibernate.classic.Session getCurrentSession() {
		return getImpl().getCurrentSession();
	}
	
	private synchronized SessionFactory getImpl() {
		if (impl==null) impl = service.buildSessionFactory();
		return impl;
	}

	//readResolveObject
	private Object readResolve() throws ObjectStreamException {
		// look for the instance by uuid
		Object result = SessionFactoryObjectFactory.getInstance(uuid);
		if (result==null) {
			// in case we were deserialized in a different JVM, look for an instance with the same name
			// (alternatively we could do an actual JNDI lookup here....)
			result = SessionFactoryObjectFactory.getNamedInstance(name);
			if (result==null) {
				throw new InvalidObjectException("Could not find a stub SessionFactory named: " + name);
			}
			else {
				log.debug("resolved stub SessionFactory by name");
			}
		}
		else {
			log.debug("resolved stub SessionFactory by uid");
		}
		return result;
	}

	/**
	 * @see javax.naming.Referenceable#getReference()
	 */
	public Reference getReference() throws NamingException {
		return new Reference(
			SessionFactoryStub.class.getName(),
			new StringRefAddr("uuid", uuid),
			SessionFactoryObjectFactory.class.getName(),
			null
		);
	}

	public ClassMetadata getClassMetadata(Class persistentClass) throws HibernateException {
		return getImpl().getClassMetadata(persistentClass);
	}

	public ClassMetadata getClassMetadata(String entityName)
	throws HibernateException {
		return getImpl().getClassMetadata(entityName);
	}

	public CollectionMetadata getCollectionMetadata(String roleName) throws HibernateException {
		return getImpl().getCollectionMetadata(roleName);
	}

	public Map getAllClassMetadata() throws HibernateException {
		return getImpl().getAllClassMetadata();
	}

	public Map getAllCollectionMetadata() throws HibernateException {
		return getImpl().getAllCollectionMetadata();
	}

	public void close() throws HibernateException {
	}
	
	public boolean isClosed() {
		return false;
	}

	public Cache getCache() {
		return getImpl().getCache();
	}

	public void evict(Class persistentClass, Serializable id)
		throws HibernateException {
		getImpl().evict(persistentClass, id);
	}

	public void evict(Class persistentClass) throws HibernateException {
		getImpl().evict(persistentClass);
	}

	public void evictEntity(String entityName, Serializable id)
	throws HibernateException {
		getImpl().evictEntity(entityName, id);
	}
	
	public void evictEntity(String entityName) throws HibernateException {
		getImpl().evictEntity(entityName);
	}

	public void evictCollection(String roleName, Serializable id)
		throws HibernateException {
		getImpl().evictCollection(roleName, id);
	}

	public void evictCollection(String roleName) throws HibernateException {
		getImpl().evictCollection(roleName);
	}

	public void evictQueries() throws HibernateException {
		getImpl().evictQueries();
	}

	public void evictQueries(String cacheRegion) throws HibernateException {
		getImpl().evictQueries(cacheRegion);
	}

	public Statistics getStatistics() {
		return getImpl().getStatistics();
	}

	public StatelessSession openStatelessSession() {
		return getImpl().openStatelessSession();
	}

	public StatelessSession openStatelessSession(Connection conn) {
		return getImpl().openStatelessSession(conn);
	}

	public Set getDefinedFilterNames() {
		return getImpl().getDefinedFilterNames();
	}

	public FilterDefinition getFilterDefinition(String filterName) throws HibernateException {
		return getImpl().getFilterDefinition( filterName );
	}

	public boolean containsFetchProfileDefinition(String name) {
		return getImpl().containsFetchProfileDefinition( name );
	}
}
