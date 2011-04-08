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
package org.hibernate.metamodel.source;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.DuplicateMappingException;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.annotations.common.reflection.MetadataProvider;
import org.hibernate.annotations.common.reflection.MetadataProviderInjector;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.annotations.common.reflection.java.JavaReflectionManager;
import org.hibernate.cfg.EJB3NamingStrategy;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.cfg.annotations.reflection.JPAMetadataProvider;
import org.hibernate.mapping.FetchProfile;
import org.hibernate.mapping.MetadataSource;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.PluralAttributeBinding;
import org.hibernate.metamodel.relational.Database;
import org.hibernate.metamodel.source.hbm.HibernateXmlBinder;

import org.jboss.logging.Logger;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class Metadata implements Serializable {

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, Metadata.class.getName());

	private final HibernateXmlBinder hibernateXmlBinder = new HibernateXmlBinder( this );
	private final ExtendsQueue extendsQueue = new ExtendsQueue( this );
	private final MetadataSourceQueue metadataSourceQueue = new MetadataSourceQueue( this );

	private transient ReflectionManager reflectionManager = createReflectionManager();

	public HibernateXmlBinder getHibernateXmlBinder() {
		return hibernateXmlBinder;
	}

	public ExtendsQueue getExtendsQueue() {
		return extendsQueue;
	}

	public MetadataSourceQueue getMetadataSourceQueue() {
		return metadataSourceQueue;
	}

	public ReflectionManager getReflectionManager() {
		return reflectionManager;
	}

	public void setReflectionManager(ReflectionManager reflectionManager) {
		this.reflectionManager = reflectionManager;
	}

	private ReflectionManager createReflectionManager() {
		return createReflectionManager( new JPAMetadataProvider() );
	}

	private ReflectionManager createReflectionManager(MetadataProvider metadataProvider) {
		ReflectionManager reflectionManager = new JavaReflectionManager();
		( (MetadataProviderInjector) reflectionManager ).setMetadataProvider( metadataProvider );
		return reflectionManager;
	}

	private final Database database = new Database();

	public Database getDatabase() {
		return database;
	}

	private NamingStrategy namingStrategy = EJB3NamingStrategy.INSTANCE;

	public NamingStrategy getNamingStrategy() {
		return namingStrategy;
	}

	public void setNamingStrategy(NamingStrategy namingStrategy) {
		this.namingStrategy = namingStrategy;
	}

	private Map<String,EntityBinding> entityBindingMap = new HashMap<String, EntityBinding>();

	public EntityBinding getEntityBinding(String entityName) {
		return entityBindingMap.get( entityName );
	}

	public Iterable<EntityBinding> getEntityBindings() {
		return entityBindingMap.values();
	}

	public void addEntity(EntityBinding entityBinding) {
		final String entityName = entityBinding.getEntity().getName();
		if ( entityBindingMap.containsKey( entityName ) ) {
			throw new DuplicateMappingException( DuplicateMappingException.Type.ENTITY, entityName );
		}
		entityBindingMap.put( entityName, entityBinding );
	}

	private Map<String,PluralAttributeBinding> collectionBindingMap = new HashMap<String, PluralAttributeBinding>();

	public PluralAttributeBinding getCollection(String collectionRole) {
		return collectionBindingMap.get( collectionRole );
	}

	public Iterable<PluralAttributeBinding> getCollections() {
		return collectionBindingMap.values();
	}

	public void addCollection(PluralAttributeBinding pluralAttributeBinding) {
		final String owningEntityName = pluralAttributeBinding.getEntityBinding().getEntity().getName();
		final String attributeName = pluralAttributeBinding.getAttribute().getName();
		final String collectionRole = owningEntityName + '.' + attributeName;
		if ( collectionBindingMap.containsKey( collectionRole ) ) {
			throw new DuplicateMappingException( DuplicateMappingException.Type.ENTITY, collectionRole );
		}
		collectionBindingMap.put( collectionRole, pluralAttributeBinding );
	}

	private Map<String,String> imports;

	public void addImport(String importName, String entityName) {
		if ( imports == null ) {
			imports = new HashMap<String, String>();
		}
		LOG.tracef( "Import: %s -> %s", importName, entityName );
		String old = imports.put( importName, entityName );
		if ( old != null ) {
			LOG.debugf( "import name [%s] overrode previous [%s]", importName, old  );
		}
	}

	private Map<String,FetchProfile> fetchProfiles = new HashMap<String, FetchProfile>();

	public Iterable<FetchProfile> getFetchProfiles() {
		return fetchProfiles.values();
	}

	public FetchProfile findOrCreateFetchProfile(String profileName, MetadataSource source) {
		FetchProfile profile = fetchProfiles.get( profileName );
		if ( profile == null ) {
			profile = new FetchProfile( profileName, source );
			fetchProfiles.put( profileName, profile );
		}
		return profile;
	}

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		//we need  reflectionManager before reading the other components (MetadataSourceQueue in particular)
		final MetadataProvider metadataProvider = (MetadataProvider) ois.readObject();
		this.reflectionManager = createReflectionManager( metadataProvider );
		ois.defaultReadObject();
	}

	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		//We write MetadataProvider first as we need  reflectionManager before reading the other components
		final MetadataProvider metadataProvider = ( ( MetadataProviderInjector ) reflectionManager ).getMetadataProvider();
		out.writeObject( metadataProvider );
		out.defaultWriteObject();
	}
}
