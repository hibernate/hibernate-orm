/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.AnnotationException;
import org.hibernate.boot.internal.ClassmateContext;
import org.hibernate.boot.jaxb.mapping.spi.JaxbConverterImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityListenerContainerImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityListenerImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbManagedType;
import org.hibernate.boot.jaxb.mapping.spi.JaxbMappedSuperclassImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPersistenceUnitDefaultsImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPersistenceUnitMetadataImpl;
import org.hibernate.boot.model.convert.internal.ClassBasedConverterDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.ClassLoaderAccess;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;

import jakarta.persistence.AccessType;
import jakarta.persistence.AttributeConverter;

/**
 * A helper for consuming orm.xml mappings.
 *
 * @author Emmanuel Bernard
 * @author Brett Meyer
 */
public class XMLContext implements Serializable {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( XMLContext.class );

	private final ClassLoaderAccess classLoaderAccess;
	private final ClassmateContext classmateContext;

	private Default globalDefaults;
	private final Map<String, JaxbManagedType> managedTypeOverride = new HashMap<>();
	private final Map<String, JaxbEntityListenerImpl> entityListenerOverride = new HashMap<>();
	private final Map<String, Default> defaultsOverride = new HashMap<>();
	private final List<JaxbEntityMappingsImpl> defaultElements = new ArrayList<>();
	private final List<String> defaultEntityListeners = new ArrayList<>();
	private boolean hasContext = false;

	public XMLContext(BootstrapContext bootstrapContext) {
		this( bootstrapContext.getClassLoaderAccess(), bootstrapContext.getClassmateContext() );
	}

	public XMLContext(ClassLoaderAccess classLoaderAccess, ClassmateContext classmateContext) {
		this.classLoaderAccess = classLoaderAccess;
		this.classmateContext = classmateContext;
	}

	/**
	 * Add the JAXB binding to this context and return the list of added class names.
	 */
	public List<String> addDocument(JaxbEntityMappingsImpl entityMappings) {
		hasContext = true;

		final List<String> addedClasses = new ArrayList<>();

		//global defaults
		final JaxbPersistenceUnitMetadataImpl metadata = entityMappings.getPersistenceUnitMetadata();
		if ( metadata != null ) {
			if ( globalDefaults == null ) {
				globalDefaults = new Default();
				globalDefaults.setMetadataComplete(
						metadata.getXmlMappingMetadataComplete() != null
								? Boolean.TRUE
								: null
				);

				final JaxbPersistenceUnitDefaultsImpl defaultElement = metadata.getPersistenceUnitDefaults();
				if ( defaultElement != null ) {
					globalDefaults.setSchema( defaultElement.getSchema() );
					globalDefaults.setCatalog( defaultElement.getCatalog() );
					globalDefaults.setAccess( defaultElement.getAccess() );
					globalDefaults.setCascadePersist( defaultElement.getCascadePersist() != null ? Boolean.TRUE : null );
					globalDefaults.setDelimitedIdentifiers( defaultElement.getDelimitedIdentifiers() != null ? Boolean.TRUE : null );
					defaultEntityListeners.addAll( addEntityListenerClasses( defaultElement.getEntityListenerContainer(), null, addedClasses ) );
				}
			}
			else {
				LOG.duplicateMetadata();
			}
		}

		//entity mapping default
		Default entityMappingDefault = new Default();
		String packageName = entityMappings.getPackage();
		entityMappingDefault.setPackageName( packageName );
		entityMappingDefault.setSchema( entityMappings.getSchema() );
		entityMappingDefault.setCatalog( entityMappings.getCatalog() );
		entityMappingDefault.setAccess( entityMappings.getAccess() );
		defaultElements.add( entityMappings );

		setLocalAttributeConverterDefinitions( entityMappings.getConverters(), packageName );

		addClass( entityMappings.getEntities(), packageName, entityMappingDefault, addedClasses );

		addClass( entityMappings.getMappedSuperclasses(), packageName, entityMappingDefault, addedClasses );

		addClass( entityMappings.getEmbeddables(), packageName, entityMappingDefault, addedClasses );

		return addedClasses;
	}

	private void addClass(List<? extends JaxbManagedType> managedTypes, String packageName, Default defaults, List<String> addedClasses) {
		for ( JaxbManagedType element : managedTypes) {
			String className = buildSafeClassName( element.getClazz(), packageName );
			if ( managedTypeOverride.containsKey( className ) ) {
				//maybe switch it to warn?
				throw new IllegalStateException( "Duplicate XML entry for " + className );
			}
			addedClasses.add( className );
			managedTypeOverride.put( className, element );
			Default mergedDefaults = new Default();
			// Apply entity mapping defaults
			mergedDefaults.overrideWithCatalogAndSchema( defaults );
			// ... then apply entity settings
			Default fileDefaults = new Default();
			fileDefaults.setMetadataComplete( element.isMetadataComplete() );
			fileDefaults.setAccess( element.getAccess() );
			mergedDefaults.overrideWithCatalogAndSchema( fileDefaults );
			// ... and we get the merged defaults for that entity
			defaultsOverride.put( className, mergedDefaults );

			LOG.debugf( "Adding XML overriding information for %s", className );
			if ( element instanceof JaxbEntityImpl ) {
				addEntityListenerClasses( ( (JaxbEntityImpl) element ).getEntityListenerContainer(), packageName, addedClasses );
			}
			else if ( element instanceof JaxbMappedSuperclassImpl ) {
				addEntityListenerClasses( ( (JaxbMappedSuperclassImpl) element ).getEntityListenerContainer(), packageName, addedClasses );
			}
		}
	}

	private List<String> addEntityListenerClasses(JaxbEntityListenerContainerImpl listeners, String packageName, List<String> addedClasses) {
		List<String> localAddedClasses = new ArrayList<>();
		if ( listeners != null ) {
			List<JaxbEntityListenerImpl> elements = listeners.getEntityListeners();
			for ( JaxbEntityListenerImpl listener : elements ) {
				String listenerClassName = buildSafeClassName( listener.getClazz(), packageName );
				if ( entityListenerOverride.containsKey( listenerClassName ) ) {
					LOG.duplicateListener( listenerClassName );
					continue;
				}
				localAddedClasses.add( listenerClassName );
				entityListenerOverride.put( listenerClassName, listener );
			}
		}
		LOG.debugf( "Adding XML overriding information for listeners: %s", localAddedClasses );
		addedClasses.addAll( localAddedClasses );
		return localAddedClasses;
	}

	private void setLocalAttributeConverterDefinitions(List<JaxbConverterImpl> converterElements, String packageName) {
		for ( JaxbConverterImpl converterElement : converterElements ) {
			final String className = converterElement.getClazz();
			final boolean autoApply = Boolean.TRUE.equals( converterElement.isAutoApply() );

			try {
				final Class<? extends AttributeConverter<?,?>> attributeConverterClass = classLoaderAccess.classForName(
						buildSafeClassName( className, packageName )
				);
				converterDescriptors.add(
						new ClassBasedConverterDescriptor( attributeConverterClass, autoApply, classmateContext )
				);
			}
			catch (ClassLoadingException e) {
				throw new AnnotationException( "Unable to locate specified AttributeConverter implementation class : " + className, e );
			}
			catch (Exception e) {
				throw new AnnotationException( "Unable to instantiate specified AttributeConverter implementation class : " + className, e );
			}
		}
	}

	public static String buildSafeClassName(String className, String defaultPackageName) {
		if ( className.indexOf( '.' ) < 0 && StringHelper.isNotEmpty( defaultPackageName ) ) {
			className = StringHelper.qualify( defaultPackageName, className );
		}
		return className;
	}

	public static String buildSafeClassName(String className, Default defaults) {
		return buildSafeClassName( className, defaults.getPackageName() );
	}

	public Default getDefaultWithoutGlobalCatalogAndSchema(String className) {
		Default xmlDefault = new Default();
		xmlDefault.overrideWithoutCatalogAndSchema( globalDefaults );
		if ( className != null ) {
			Default entityMappingOverriding = defaultsOverride.get( className );
			xmlDefault.overrideWithCatalogAndSchema( entityMappingOverriding );
		}
		return xmlDefault;
	}

	public Default getDefaultWithGlobalCatalogAndSchema() {
		Default xmlDefault = new Default();
		xmlDefault.overrideWithCatalogAndSchema( globalDefaults );
		return xmlDefault;
	}

	public JaxbManagedType getManagedTypeOverride(String className) {
		return managedTypeOverride.get( className );
	}

	public JaxbEntityListenerImpl getEntityListenerOverride(String className) {
		return entityListenerOverride.get( className );
	}

	public List<JaxbEntityMappingsImpl> getAllDocuments() {
		return defaultElements;
	}

	public boolean hasContext() {
		return hasContext;
	}

	private final List<ConverterDescriptor> converterDescriptors = new ArrayList<>();

	public void applyDiscoveredAttributeConverters(ConverterRegistry converterRegistry) {
		for ( ConverterDescriptor descriptor : converterDescriptors ) {
			converterRegistry.addAttributeConverter( descriptor );
		}
		converterDescriptors.clear();
	}

	public static class Default implements Serializable {
		private AccessType access;
		private String packageName;
		private String schema;
		private String catalog;
		private Boolean metadataComplete;
		private Boolean cascadePersist;
		private Boolean delimitedIdentifier;

		public AccessType getAccess() {
			return access;
		}

		protected void setAccess(AccessType access) {
			this.access = access;
		}

		public String getCatalog() {
			return catalog;
		}

		protected void setCatalog(String catalog) {
			this.catalog = catalog;
		}

		public String getPackageName() {
			return packageName;
		}

		protected void setPackageName(String packageName) {
			this.packageName = packageName;
		}

		public String getSchema() {
			return schema;
		}

		protected void setSchema(String schema) {
			this.schema = schema;
		}

		public Boolean getMetadataComplete() {
			return metadataComplete;
		}

		public boolean canUseJavaAnnotations() {
			return metadataComplete == null || !metadataComplete;
		}

		protected void setMetadataComplete(Boolean metadataComplete) {
			this.metadataComplete = metadataComplete;
		}

		public Boolean getCascadePersist() {
			return cascadePersist;
		}

		void setCascadePersist(Boolean cascadePersist) {
			this.cascadePersist = cascadePersist;
		}

		public void overrideWithCatalogAndSchema(Default override) {
			overrideWithoutCatalogAndSchema( override );
			if ( override != null ) {
				if ( override.getSchema() != null ) {
					schema = override.getSchema();
				}
				if ( override.getCatalog() != null ) {
					catalog = override.getCatalog();
				}
			}
		}

		public void overrideWithoutCatalogAndSchema(Default globalDefault) {
			if ( globalDefault != null ) {
				if ( globalDefault.getAccess() != null ) {
					access = globalDefault.getAccess();
				}
				if ( globalDefault.getPackageName() != null ) {
					packageName = globalDefault.getPackageName();
				}
				if ( globalDefault.getDelimitedIdentifier() != null ) {
					delimitedIdentifier = globalDefault.getDelimitedIdentifier();
				}
				if ( globalDefault.getMetadataComplete() != null ) {
					metadataComplete = globalDefault.getMetadataComplete();
				}
				if ( globalDefault.getCascadePersist() != null ) {
					cascadePersist = globalDefault.getCascadePersist();
				}
			}
		}

		public void setDelimitedIdentifiers(Boolean delimitedIdentifier) {
			this.delimitedIdentifier = delimitedIdentifier;
		}

		public Boolean getDelimitedIdentifier() {
			return delimitedIdentifier;
		}
	}

	public List<String> getDefaultEntityListeners() {
		return defaultEntityListeners;
	}
}
