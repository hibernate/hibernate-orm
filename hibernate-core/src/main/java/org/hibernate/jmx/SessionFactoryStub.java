/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
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

import org.jboss.logging.Logger;

import org.hibernate.AssertionFailure;
import org.hibernate.Cache;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionBuilder;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.StatelessSessionBuilder;
import org.hibernate.TypeHelper;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.UUIDGenerator;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.SessionFactoryRegistry;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metadata.CollectionMetadata;
import org.hibernate.service.jndi.internal.JndiServiceImpl;
import org.hibernate.stat.Statistics;

/**
 * A flyweight for <tt>SessionFactory</tt>. If the MBean itself does not
 * have classpath to the persistent classes, then a stub will be registered
 * with JNDI and the actual <tt>SessionFactoryImpl</tt> built upon first
 * access.
 *
 * @author Gavin King
 *
 * @deprecated See <a href="http://opensource.atlassian.com/projects/hibernate/browse/HHH-6190">HHH-6190</a> for details
 */
@Deprecated
@SuppressWarnings( {"deprecation"})
public class SessionFactoryStub implements SessionFactory {
	private static final IdentifierGenerator UUID_GENERATOR = UUIDGenerator.buildSessionFactoryUniqueIdentifierGenerator();

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, SessionFactoryStub.class.getName());

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

		SessionFactoryRegistry.INSTANCE.addSessionFactory(
				uuid,
				name,
				ConfigurationHelper.getBoolean(
						AvailableSettings.SESSION_FACTORY_NAME_IS_JNDI,
						service.getProperties(),
						true
				),
				this,
				new JndiServiceImpl( service.getProperties() )
		);
	}

	@Override
	public SessionFactoryOptions getSessionFactoryOptions() {
		return impl.getSessionFactoryOptions();
	}

	@Override
	public SessionBuilder withOptions() {
		return getImpl().withOptions();
	}

	public Session openSession() throws HibernateException {
		return getImpl().openSession();
	}

	public Session getCurrentSession() {
		return getImpl().getCurrentSession();
	}

	private synchronized SessionFactory getImpl() {
		if (impl==null) impl = service.buildSessionFactory();
		return impl;
	}

	//readResolveObject
	private Object readResolve() throws ObjectStreamException {
		// look for the instance by uuid
		Object result = SessionFactoryRegistry.INSTANCE.getSessionFactory( uuid ) ;
		if ( result == null ) {
            // in case we were deserialized in a different JVM, look for an instance with the same name
			// (alternatively we could do an actual JNDI lookup here....)
			result = SessionFactoryRegistry.INSTANCE.getNamedSessionFactory( name );
			if ( result == null ) {
				throw new InvalidObjectException( "Could not find a SessionFactory [uuid=" + uuid + ",name=" + name + "]" );
			}
			LOG.debug("Resolved stub SessionFactory by name");
		}
		else {
			LOG.debug("Resolved stub SessionFactory by UUID");
		}
		return result;
	}

	/**
	 * @see javax.naming.Referenceable#getReference()
	 */
	@Override
	public Reference getReference() throws NamingException {
		return new Reference(
				SessionFactoryStub.class.getName(),
				new StringRefAddr("uuid", uuid),
				SessionFactoryRegistry.ObjectFactoryImpl.class.getName(),
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

	public Map<String,ClassMetadata> getAllClassMetadata() throws HibernateException {
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

	@Override
	public StatelessSessionBuilder withStatelessOptions() {
		return getImpl().withStatelessOptions();
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

	public TypeHelper getTypeHelper() {
		return getImpl().getTypeHelper();
	}
}
