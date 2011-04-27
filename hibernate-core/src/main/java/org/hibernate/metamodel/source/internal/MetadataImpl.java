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
package org.hibernate.metamodel.source.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.jboss.logging.Logger;

import org.hibernate.DuplicateMappingException;
import org.hibernate.HibernateException;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.mapping.MetadataSource;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.FetchProfile;
import org.hibernate.metamodel.binding.PluralAttributeBinding;
import org.hibernate.metamodel.relational.Database;
import org.hibernate.metamodel.source.Metadata;
import org.hibernate.metamodel.source.MetadataSources;
import org.hibernate.metamodel.source.annotation.xml.XMLEntityMappings;
import org.hibernate.metamodel.source.annotations.AnnotationBinder;
import org.hibernate.metamodel.source.annotations.xml.OrmXmlParser;
import org.hibernate.metamodel.source.hbm.HibernateXmlBinder;
import org.hibernate.metamodel.source.hbm.xml.mapping.XMLHibernateMapping;
import org.hibernate.metamodel.source.spi.MetadataImplementor;
import org.hibernate.service.BasicServiceRegistry;
import org.hibernate.service.classloading.spi.ClassLoaderService;

/**
 * Container for configuration data while building and binding the metamodel
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public class MetadataImpl implements Metadata, MetadataImplementor, Serializable {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class, MetadataImpl.class.getName()
	);

	private final BasicServiceRegistry serviceRegistry;
	private final NamingStrategy namingStrategy;
	private final Database database = new Database();

	private Map<String, EntityBinding> entityBindingMap = new HashMap<String, EntityBinding>();
	private Map<String, PluralAttributeBinding> collectionBindingMap = new HashMap<String, PluralAttributeBinding>();
	private Map<String, FetchProfile> fetchProfiles = new HashMap<String, FetchProfile>();
	private Map<String, String> imports;

	public MetadataImpl(MetadataSources metadataSources, ProcessingOrder preferredProcessingOrder) {
		this.serviceRegistry = metadataSources.getServiceRegistry();
		this.namingStrategy = metadataSources.getNamingStrategy();

		final ArrayList<String> processedEntityNames = new ArrayList<String>();
		if ( preferredProcessingOrder == ProcessingOrder.HBM_FIRST ) {
			applyHibernateMappings( metadataSources, processedEntityNames );
			applyAnnotationMappings( metadataSources, processedEntityNames );
		}
		else {
			applyAnnotationMappings( metadataSources, processedEntityNames );
			applyHibernateMappings( metadataSources, processedEntityNames );
		}
	}

	private void applyHibernateMappings(MetadataSources metadataSources, List<String> processedEntityNames) {
		final HibernateXmlBinder hibernateXmlBinder = new HibernateXmlBinder( this );
		for ( JaxbRoot jaxbRoot : metadataSources.getJaxbRootList() ) {
			// filter to just hbm-based roots
			if ( jaxbRoot.getRoot() instanceof XMLHibernateMapping ) {
				hibernateXmlBinder.bindRoot( jaxbRoot );
			}
		}
	}

	private void applyAnnotationMappings(MetadataSources metadataSources, List<String> processedEntityNames) {
		// create a jandex index from the annotated classes
		Indexer indexer = new Indexer();
		for ( Class<?> clazz : metadataSources.getAnnotatedClasses() ) {
			indexClass( indexer, clazz.getName().replace( '.', '/' ) + ".class" );
		}

		// add package-info from the configured packages
		for ( String packageName : metadataSources.getAnnotatedPackages() ) {
			indexClass( indexer, packageName.replace( '.', '/' ) + "/package-info.class" );
		}
		Index index = indexer.complete();

		// process the xml configuration
		final OrmXmlParser ormParser = new OrmXmlParser( this );
		List<JaxbRoot<XMLEntityMappings>> mappings = new ArrayList<JaxbRoot<XMLEntityMappings>>();
		for ( JaxbRoot<?> root : metadataSources.getJaxbRootList() ) {
			if ( root.getRoot() instanceof XMLEntityMappings ) {
				mappings.add( (JaxbRoot<XMLEntityMappings>) root );
			}
		}
		index = ormParser.parseAndUpdateIndex( mappings, index );

		// create the annotation binder and pass it the final annotation index
		final AnnotationBinder annotationBinder = new AnnotationBinder( this );
		annotationBinder.bindGlobalAnnotations( index );
		annotationBinder.bindMappedClasses( index );
	}

	/**
	 * Adds a single class to the jandex index
	 *
	 * @param indexer the jandex indexer
	 * @param className the fully qualified name of the class
	 */
	private void indexClass(Indexer indexer, String className) {
		ClassLoaderService classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
		InputStream stream = classLoaderService.locateResourceStream( className );
		try {
			indexer.index( stream );
		}
		catch ( IOException e ) {
			throw new HibernateException( "Unable to open input stream for class " + className, e );
		}
	}

	public BasicServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	public Database getDatabase() {
		return database;
	}

	public NamingStrategy getNamingStrategy() {
		return namingStrategy;
	}

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

	public void addImport(String importName, String entityName) {
		if ( imports == null ) {
			imports = new HashMap<String, String>();
		}
		LOG.trace( "Import: " + importName + " -> " + entityName );
		String old = imports.put( importName, entityName );
		if ( old != null ) {
			LOG.debug( "import name [" + importName + "] overrode previous [{" + old + "}]" );
		}
	}

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
}
