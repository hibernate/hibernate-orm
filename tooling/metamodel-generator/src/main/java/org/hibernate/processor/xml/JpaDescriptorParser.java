/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import org.hibernate.MappingException;
import org.hibernate.boot.ResourceStreamLocator;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl;
import org.hibernate.boot.jaxb.internal.ConfigurationBinder;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbMappedSuperclassImpl;
import org.hibernate.processor.Context;
import org.hibernate.processor.util.AccessTypeInformation;
import org.hibernate.processor.util.FileTimeStampChecker;
import org.hibernate.processor.util.xml.XmlParserHelper;

import jakarta.persistence.AccessType;
import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.processor.util.StringUtil.determineFullyQualifiedClassName;
import static org.hibernate.processor.util.StringUtil.packageNameFromFullyQualifiedName;
import static org.hibernate.processor.util.TypeUtils.determineAccessTypeForHierarchy;

/**
 * Parser for JPA XML descriptors (persistence.xml and referenced mapping files).
 *
 * @author Hardy Ferentschik
 */
public final class JpaDescriptorParser {
	private static final String DEFAULT_ORM_XML_LOCATION = "/META-INF/orm.xml";
	private static final String SERIALIZATION_FILE_NAME = "Hibernate-Static-Metamodel-Generator.tmp";

	private final Context context;
	private final List<JaxbEntityMappingsImpl> entityMappings;
	private final XmlParserHelper xmlParserHelper;
	private final ConfigurationBinder configurationBinder;
	private final MappingBinder mappingBinder;
	private final ResourceStreamLocator resourceStreamLocator;

	public JpaDescriptorParser(Context context) {
		this.context = context;
		entityMappings = new ArrayList<>();
		xmlParserHelper = new XmlParserHelper( context );
		resourceStreamLocator = new ResourceStreamLocatorImpl( context );
		configurationBinder = new ConfigurationBinder( resourceStreamLocator );
		mappingBinder = new MappingBinder( resourceStreamLocator, (Function<String, Object>) null );
	}

	public void parseMappingXml() {
		final var mappingFileNames = determineMappingFileNames();
		if ( !context.doLazyXmlParsing() || !mappingFilesUnchanged( mappingFileNames ) ) {
			loadEntityMappings( mappingFileNames );
			determineDefaultAccessTypeAndMetaCompleteness();
			determineXmlAccessTypes();
			if ( !context.isFullyXmlConfigured() ) {
				// Need to take annotations into consideration, since they can override XML settings.
				// We have to at least determine whether any of the XML-configured entities are influenced by annotations.
				determineAnnotationAccessTypes();
			}

			for ( var mappings : entityMappings ) {
				final String defaultPackageName = mappings.getPackage();
				parseEntities( mappings.getEntities(), defaultPackageName );
				parseEmbeddable( mappings.getEmbeddables(), defaultPackageName );
				parseMappedSuperClass( mappings.getMappedSuperclasses(), defaultPackageName );
			}
		}
	}

	private Collection<String> determineMappingFileNames() {
		final Collection<String> mappingFileNames = new ArrayList<>();
		final var persistence = getPersistence();
		if ( persistence != null ) {
			// get mapping file names from persistence.xml
			for ( var unit : persistence.getPersistenceUnit() ) {
				mappingFileNames.addAll( unit.getMappingFiles() );
			}
		}
		// /META-INF/orm.xml is implicit
		mappingFileNames.add( DEFAULT_ORM_XML_LOCATION );
		// not really part of the official spec, but the processor allows
		// specifying mapping files directly as command line options
		mappingFileNames.addAll( context.getOrmXmlFiles() );
		return mappingFileNames;
	}

	private @Nullable JaxbPersistenceImpl getPersistence() {
		final String persistenceXmlLocation = context.getPersistenceXmlLocation();
		final var stream = xmlParserHelper.getInputStreamForResource( persistenceXmlLocation );
		if ( stream == null ) {
			return null;
		}
		else {
			try (stream) {
				final var binding =
						configurationBinder.bind( stream,
								new Origin( SourceType.RESOURCE, persistenceXmlLocation ) );
				return binding.getRoot();
			}
			catch (MappingException e) {
				throw e;
			}
			catch (Exception e) {
				context.logMessage( Diagnostic.Kind.WARNING,
						"Unable to parse persistence.xml: " + e.getMessage() );
				return null;
			}
		}
	}

	private void loadEntityMappings(Collection<String> mappingFileNames) {
		for ( String mappingFile : mappingFileNames ) {
			final var inputStream = resourceStreamLocator.locateResourceStream( mappingFile );
			if ( inputStream != null ) {
				try ( inputStream ) {
					final var binding =
							mappingBinder.bind( inputStream,
									new Origin( SourceType.RESOURCE, mappingFile ) );
					if ( binding != null ) {
						entityMappings.add( (JaxbEntityMappingsImpl) binding.getRoot() );
					}
				}
				catch (Exception e) {
					context.logMessage(
							Diagnostic.Kind.WARNING,
							"Unable to parse " + mappingFile + ": " + e.getMessage()
					);
				}
			}
			// else todo (jpa32) : log
		}
	}

	private boolean mappingFilesUnchanged(Collection<String> mappingFileNames) {
		final var fileStampCheck = new FileTimeStampChecker();
		for ( String mappingFile : mappingFileNames ) {
			try {
				final var url = JpaDescriptorParser.class.getResource( mappingFile );
				if ( url != null ) {
					context.logMessage( Diagnostic.Kind.OTHER, "Check file " + mappingFile );
					final var file = new File( url.toURI() );
					if ( file.exists() ) {
						fileStampCheck.add( mappingFile, file.lastModified() );
					}
				}
			}
			catch (URISyntaxException e) {
				// in doubt return false
				return false;
			}
		}

		if ( loadTimeStampCache().equals( fileStampCheck ) ) {
			context.logMessage( Diagnostic.Kind.OTHER,
					"XML parsing will be skipped due to unchanged XML files" );
			return true;
		}
		else {
			saveTimeStampCache( fileStampCheck );
			return false;
		}
	}

	private void saveTimeStampCache(FileTimeStampChecker fileStampCheck) {
		final var file = getSerializationTmpFile();
		try ( final var out = new ObjectOutputStream( new FileOutputStream( file ) ) ) {
			out.writeObject( fileStampCheck );
			context.logMessage( Diagnostic.Kind.OTHER,
					"Serialized " + fileStampCheck + " into " + file.getAbsolutePath() );
		}
		catch (IOException e) {
			// ignore - if the serialization failed, we just have to keep parsing the XML
			context.logMessage( Diagnostic.Kind.OTHER, "Error serializing " + fileStampCheck );
		}
	}

	private File getSerializationTmpFile() {
		final var tmpDir = new File( System.getProperty( "java.io.tmpdir" ) );
		return new File( tmpDir, SERIALIZATION_FILE_NAME );
	}

	private FileTimeStampChecker loadTimeStampCache() {
		final var file = getSerializationTmpFile();
		if ( file.exists() ) {
			try ( var in = new ObjectInputStream( new FileInputStream( file ) ) ) {
				return (FileTimeStampChecker) in.readObject();
			}
			catch (IOException | ClassNotFoundException e) {
				//handled in the outer scope
			}
		}
		// ignore - if the deserialization failed, we just have to keep parsing the XML
		context.logMessage( Diagnostic.Kind.OTHER, "Error deserializing " + file );
		return new FileTimeStampChecker();
	}

	private void parseEntities(List<JaxbEntityImpl> entities, String defaultPackageName) {
		for ( var entity : entities ) {
			final String entityClassName =
					determineFullyQualifiedClassName( defaultPackageName, entity.getClazz() );
			if ( !xmlMappedTypeExists( entityClassName ) ) {
				context.logMessage( Diagnostic.Kind.WARNING,
						entityClassName
						+ " is mapped in XML, but class does not exist. Skipping meta model generation." );
			}
			else {
				final var metaEntity =
						XmlMetaEntity.create( entity, defaultPackageName, getXmlMappedType( entityClassName ), context );
				if ( context.containsMetaEntity( entityClassName ) ) {
					context.logMessage( Diagnostic.Kind.WARNING,
							entityClassName
							+ " was already processed once. Skipping second occurrence." );
				}
				context.addMetaEntity( entityClassName, metaEntity );
			}
		}
	}

	private void parseEmbeddable(
			List<JaxbEmbeddableImpl> embeddables,
			String defaultPackageName) {
		for ( var embeddable : embeddables ) {
			final String embeddableClassName =
					determineFullyQualifiedClassName( defaultPackageName, embeddable.getClazz() );
			// We have to extract the package name from the FQCN
			// Maybe the entity was setting a FQCN directly
			if ( !xmlMappedTypeExists( embeddableClassName ) ) {
				context.logMessage( Diagnostic.Kind.WARNING,
						embeddableClassName
						+ " is mapped in XML, but class does not exist. Skipping meta model generation." );
			}
			else {
				final String pkg = packageNameFromFullyQualifiedName( embeddableClassName );
				final var metaEntity =
						new XmlMetaEntity( embeddable, pkg, getXmlMappedType( embeddableClassName ), context );
				if ( context.containsMetaEmbeddable( embeddableClassName ) ) {
					context.logMessage( Diagnostic.Kind.WARNING,
							embeddableClassName
							+ " was already processed once. Skipping second occurrence." );
				}
				context.addMetaEmbeddable( embeddableClassName, metaEntity );
			}
		}
	}

	private void parseMappedSuperClass(
			List<JaxbMappedSuperclassImpl> mappedSuperClasses,
			String defaultPackageName) {
		for ( var mappedSuperClass : mappedSuperClasses ) {
			final String mappedSuperClassName =
					determineFullyQualifiedClassName( defaultPackageName, mappedSuperClass.getClazz() );
			// We have to extract the package name from the FQCN
			// Maybe the entity was setting a FQCN directly
			if ( !xmlMappedTypeExists( mappedSuperClassName ) ) {
				context.logMessage( Diagnostic.Kind.WARNING,
						mappedSuperClassName
						+ " is mapped in XML, but class does not exist. Skipping meta model generation." );
			}
			else {
				final String pkg = packageNameFromFullyQualifiedName( mappedSuperClassName );
				final var metaEntity =
						new XmlMetaEntity( mappedSuperClass, pkg, getXmlMappedType( mappedSuperClassName ), context );
				if ( context.containsMetaEntity( mappedSuperClassName ) ) {
					context.logMessage( Diagnostic.Kind.WARNING,
							mappedSuperClassName
							+ " was already processed once. Skipping second occurrence." );
				}
				context.addMetaEntity( mappedSuperClassName, metaEntity );
			}
		}
	}

	private boolean xmlMappedTypeExists(String fullyQualifiedClassName) {
		return context.getElementUtils().getTypeElement( fullyQualifiedClassName ) != null;
	}

	private TypeElement getXmlMappedType(String fullyQualifiedClassName) {
		return context.getElementUtils().getTypeElement( fullyQualifiedClassName );
	}

	private AccessType determineEntityAccessType(JaxbEntityMappingsImpl mappings) {
		final var mappingsAccess = mappings.getAccess();
		return mappingsAccess != null ? mappingsAccess : context.getPersistenceUnitDefaultAccessType();
	}

	private void determineXmlAccessTypes() {
		for ( var mappings : entityMappings ) {
			final String packageName = mappings.getPackage();
			final var defaultAccessType = determineEntityAccessType( mappings );
			for ( var entity : mappings.getEntities() ) {
				addAccessTypeInfo( packageName, entity.getClazz(), entity.getAccess(), defaultAccessType );
			}
			for ( var mappedSuperClass : mappings.getMappedSuperclasses() ) {
				addAccessTypeInfo( packageName, mappedSuperClass.getClazz(), mappedSuperClass.getAccess(), defaultAccessType );
			}
			for ( var embeddable : mappings.getEmbeddables() ) {
				addAccessTypeInfo( packageName, embeddable.getClazz(), embeddable.getAccess(), defaultAccessType );
			}
		}
	}

	private void addAccessTypeInfo(
			String packageName, String simpleName,
			AccessType accessType, AccessType defaultAccessType) {
		final String className = determineFullyQualifiedClassName( packageName, simpleName );
		context.addAccessTypeInformation( className,
				new AccessTypeInformation( className, accessType, defaultAccessType ) );
	}

	private void determineAnnotationAccessTypes() {
		for ( var mappings : entityMappings ) {
			final String packageName = mappings.getPackage();
			for ( var entity : mappings.getEntities() ) {
				determineHierarchyAccessType( packageName, entity.getClazz() );
			}
			for ( var mappedSuperClass : mappings.getMappedSuperclasses() ) {
				determineHierarchyAccessType( packageName, mappedSuperClass.getClazz() );
			}
		}
	}

	private void determineHierarchyAccessType(String packageName, String simpleName) {
		final var element =
				context.getTypeElementForFullyQualifiedName(
						determineFullyQualifiedClassName( packageName, simpleName ) );
		if ( element != null ) {
			determineAccessTypeForHierarchy( element, context );
		}
	}

	/**
	 * Determines the default access type as specified in the <i>persistence-unit-defaults</i>
	 * as well as whether the XML configuration is complete and annotations should be ignored.
	 * <p>
	 * Note, the spec says:
	 * <ul>
	 * <li>The persistence-unit-metadata element contains metadata for the entire persistence unit.
	 *     It is undefined if this element occurs in multiple mapping files within the same persistence
	 *     unit.</li>
	 * <li>If the xml-mapping-metadata-complete sub-element is specified, the complete set of mapping
	 *     metadata for the persistence unit is contained in the XML mapping files for the persistence
	 *     unit, and any persistence annotations on the classes are ignored.</li>
	 * <li>When the xml-mapping-metadata-complete element is specified, any metadata-complete attributes
	 *     specified within the entity, mapped-superclass, and embeddable elements are ignored.<li>
	 * </ul>
	 */
	private void determineDefaultAccessTypeAndMetaCompleteness() {
		for ( var mappings : entityMappings ) {
			final var meta = mappings.getPersistenceUnitMetadata();
			if ( meta != null ) {
				context.mappingDocumentFullyXmlConfigured( meta.getXmlMappingMetadataComplete() != null );
				final var persistenceUnitDefaults = meta.getPersistenceUnitDefaults();
				if ( persistenceUnitDefaults != null ) {
					final var xmlJpaAccessType = persistenceUnitDefaults.getAccess();
					if ( xmlJpaAccessType != null ) {
						context.setPersistenceUnitDefaultAccessType( xmlJpaAccessType );
					}
				}
			}
			else {
				context.mappingDocumentFullyXmlConfigured( false );
			}
		}
	}
}
