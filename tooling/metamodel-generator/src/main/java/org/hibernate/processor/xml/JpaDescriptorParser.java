/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

import org.hibernate.MappingException;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.configuration.spi.JaxbPersistenceImpl;
import org.hibernate.boot.jaxb.internal.ConfigurationBinder;
import org.hibernate.boot.jaxb.internal.MappingBinder;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEmbeddableImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbMappedSuperclassImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPersistenceUnitDefaultsImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbPersistenceUnitMetadataImpl;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.jaxb.spi.JaxbBindableMappingDescriptor;
import org.hibernate.processor.Context;
import org.hibernate.processor.util.AccessTypeInformation;
import org.hibernate.processor.util.FileTimeStampChecker;
import org.hibernate.processor.util.TypeUtils;
import org.hibernate.processor.util.xml.XmlParserHelper;

import jakarta.persistence.AccessType;
import org.jspecify.annotations.Nullable;

import static org.hibernate.processor.util.StringUtil.determineFullyQualifiedClassName;
import static org.hibernate.processor.util.StringUtil.packageNameFromFullyQualifiedName;

/**
 * Parser for JPA XML descriptors (persistence.xml and referenced mapping files).
 *
 * @author Hardy Ferentschik
 */
public class JpaDescriptorParser {
	private static final String DEFAULT_ORM_XML_LOCATION = "/META-INF/orm.xml";
	private static final String SERIALIZATION_FILE_NAME = "Hibernate-Static-Metamodel-Generator.tmp";

	private final Context context;
	private final List<JaxbEntityMappingsImpl> entityMappings;
	private final XmlParserHelper xmlParserHelper;
	private final ConfigurationBinder configurationBinder;
	private final MappingBinder mappingBinder;

	public JpaDescriptorParser(Context context) {
		this.context = context;
		this.entityMappings = new ArrayList<>();
		this.xmlParserHelper = new XmlParserHelper( context );

		final ResourceStreamLocatorImpl resourceStreamLocator = new ResourceStreamLocatorImpl( context );
		this.configurationBinder = new ConfigurationBinder( resourceStreamLocator );
		this.mappingBinder = new MappingBinder( resourceStreamLocator, (Function<String,Object>) null );
	}

	public void parseMappingXml() {
		Collection<String> mappingFileNames = determineMappingFileNames();

		if ( context.doLazyXmlParsing() && mappingFilesUnchanged( mappingFileNames ) ) {
			return;
		}

		loadEntityMappings( mappingFileNames );
		determineDefaultAccessTypeAndMetaCompleteness();
		determineXmlAccessTypes();
		if ( !context.isFullyXmlConfigured() ) {
			// need to take annotations into consideration, since they can override xml settings
			// we have to at least determine whether any of the xml configured entities is influenced by annotations
			determineAnnotationAccessTypes();
		}

		for ( JaxbEntityMappingsImpl mappings : entityMappings ) {
			String defaultPackageName = mappings.getPackage();
			parseEntities( mappings.getEntities(), defaultPackageName );
			parseEmbeddable( mappings.getEmbeddables(), defaultPackageName );
			parseMappedSuperClass( mappings.getMappedSuperclasses(), defaultPackageName );
		}
	}

	private Collection<String> determineMappingFileNames() {
		Collection<String> mappingFileNames = new ArrayList<>();

		JaxbPersistenceImpl persistence = getPersistence();
		if ( persistence != null ) {
			// get mapping file names from persistence.xml
			List<JaxbPersistenceImpl.JaxbPersistenceUnitImpl> persistenceUnits = persistence.getPersistenceUnit();
			for ( JaxbPersistenceImpl.JaxbPersistenceUnitImpl unit : persistenceUnits ) {
				mappingFileNames.addAll( unit.getMappingFiles() );
			}
		}

		// /META-INF/orm.xml is implicit
		mappingFileNames.add( DEFAULT_ORM_XML_LOCATION );

		// not really part of the official spec, but the processor allows to specify mapping files directly as
		// command line options
		mappingFileNames.addAll( context.getOrmXmlFiles() );
		return mappingFileNames;
	}

	private @Nullable JaxbPersistenceImpl getPersistence() {
		JaxbPersistenceImpl persistence = null;

		final String persistenceXmlLocation = context.getPersistenceXmlLocation();
		final InputStream stream = xmlParserHelper.getInputStreamForResource( persistenceXmlLocation );
		if ( stream == null ) {
			return null;
		}

		try {
			final Binding<JaxbPersistenceImpl> binding = configurationBinder.bind(
					stream,
					new Origin( SourceType.RESOURCE, persistenceXmlLocation )
			);
			persistence = binding.getRoot();
		}
		catch (MappingException e) {
			throw e;
		}
		catch (Exception e) {
			context.logMessage(
					Diagnostic.Kind.WARNING, "Unable to parse persistence.xml: " + e.getMessage()
			);
		}
		finally {
			try {
				stream.close();
			}
			catch (IOException e) {
				// eat it
			}
		}

		return persistence;
	}

	private void loadEntityMappings(Collection<String> mappingFileNames) {
		final ResourceStreamLocatorImpl resourceStreamLocator = new ResourceStreamLocatorImpl( context );

		for ( String mappingFile : mappingFileNames ) {
			final InputStream inputStream = resourceStreamLocator.locateResourceStream( mappingFile );
			if ( inputStream == null ) {
				// todo (jpa32) : log
				continue;
			}
			try {
				final Binding<JaxbBindableMappingDescriptor> binding = mappingBinder.bind(
						inputStream,
						new Origin( SourceType.RESOURCE, mappingFile )
				);
				if ( binding != null ) {
					entityMappings.add( (JaxbEntityMappingsImpl) binding.getRoot() );
				}
			}
			catch (Exception e) {
				context.logMessage(
						Diagnostic.Kind.WARNING, "Unable to parse " + mappingFile + ": " + e.getMessage()
				);
			}
			finally {
				try {
					inputStream.close();
				}
				catch (IOException e) {
					// eat it
				}
			}
		}
	}

	private boolean mappingFilesUnchanged(Collection<String> mappingFileNames) {
		boolean mappingFilesUnchanged = false;
		FileTimeStampChecker fileStampCheck = new FileTimeStampChecker();
		for ( String mappingFile : mappingFileNames ) {
			try {
				URL url = this.getClass().getResource( mappingFile );
				if ( url == null ) {
					continue;
				}
				File file = new File( url.toURI() );
				context.logMessage( Diagnostic.Kind.OTHER, "Check file  " + mappingFile );
				if ( file.exists() ) {
					fileStampCheck.add( mappingFile, file.lastModified() );
				}
			}
			catch (URISyntaxException e) {
				// in doubt return false
				return false;
			}
		}

		FileTimeStampChecker serializedTimeStampCheck = loadTimeStampCache();
		if ( serializedTimeStampCheck.equals( fileStampCheck ) ) {
			context.logMessage( Diagnostic.Kind.OTHER, "XML parsing will be skipped due to unchanged xml files" );
			mappingFilesUnchanged = true;
		}
		else {
			saveTimeStampCache( fileStampCheck );
		}

		return mappingFilesUnchanged;
	}

	private void saveTimeStampCache(FileTimeStampChecker fileStampCheck) {
		final File file = getSerializationTmpFile();
		try ( final ObjectOutput out = new ObjectOutputStream( new FileOutputStream( file ) ) ) {
			out.writeObject( fileStampCheck );
			context.logMessage(
					Diagnostic.Kind.OTHER, "Serialized " + fileStampCheck + " into " + file.getAbsolutePath()
			);
		}
		catch (IOException e) {
			// ignore - if the serialization failed we just have to keep parsing the xml
			context.logMessage( Diagnostic.Kind.OTHER, "Error serializing  " + fileStampCheck );
		}
	}

	private File getSerializationTmpFile() {
		File tmpDir = new File( System.getProperty( "java.io.tmpdir" ) );
		return new File( tmpDir, SERIALIZATION_FILE_NAME );
	}

	private FileTimeStampChecker loadTimeStampCache() {
		final File file = getSerializationTmpFile();
		if ( file.exists() ) {
			try {
				try ( FileInputStream fileInputStream = new FileInputStream( file ) ) {
					try ( ObjectInputStream in = new ObjectInputStream( fileInputStream ) ) {
						return (FileTimeStampChecker) in.readObject();
					}
				}
			}
			catch (IOException|ClassNotFoundException e) {
				//handled in the outer scope
			}
		}
		// ignore - if the de-serialization failed we just have to keep parsing the xml
		context.logMessage( Diagnostic.Kind.OTHER, "Error de-serializing  " + file );
		return new FileTimeStampChecker();
	}

	private void parseEntities(List<JaxbEntityImpl> entities, String defaultPackageName) {
		for ( JaxbEntityImpl entity : entities ) {
			String fqcn = determineFullyQualifiedClassName( defaultPackageName, entity.getClazz() );

			if ( !xmlMappedTypeExists( fqcn ) ) {
				context.logMessage(
						Diagnostic.Kind.WARNING,
						fqcn + " is mapped in xml, but class does not exist. Skipping meta model generation."
				);
				continue;
			}

			XmlMetaEntity metaEntity = XmlMetaEntity.create(
					entity, defaultPackageName, getXmlMappedType( fqcn ), context
			);
			if ( context.containsMetaEntity( fqcn ) ) {
				context.logMessage(
						Diagnostic.Kind.WARNING,
						fqcn + " was already processed once. Skipping second occurrence."
				);
			}
			context.addMetaEntity( fqcn, metaEntity );
		}
	}

	private void parseEmbeddable(
			List<JaxbEmbeddableImpl> embeddables,
			String defaultPackageName) {
		for ( JaxbEmbeddableImpl embeddable : embeddables ) {
			String fqcn = determineFullyQualifiedClassName( defaultPackageName, embeddable.getClazz() );
			// we have to extract the package name from the fqcn. Maybe the entity was setting a fqcn directly
			String pkg = packageNameFromFullyQualifiedName( fqcn );

			if ( !xmlMappedTypeExists( fqcn ) ) {
				context.logMessage(
						Diagnostic.Kind.WARNING,
						fqcn + " is mapped in xml, but class does not exist. Skipping meta model generation."
				);
				continue;
			}

			XmlMetaEntity metaEntity = new XmlMetaEntity( embeddable, pkg, getXmlMappedType( fqcn ), context );
			if ( context.containsMetaEmbeddable( fqcn ) ) {
				context.logMessage(
						Diagnostic.Kind.WARNING,
						fqcn + " was already processed once. Skipping second occurrence."
				);
			}
			context.addMetaEmbeddable( fqcn, metaEntity );
		}
	}

	private void parseMappedSuperClass(
			List<JaxbMappedSuperclassImpl> mappedSuperClasses,
			String defaultPackageName) {
		for ( JaxbMappedSuperclassImpl mappedSuperClass : mappedSuperClasses ) {
			String fqcn = determineFullyQualifiedClassName(
					defaultPackageName, mappedSuperClass.getClazz()
			);
			// we have to extract the package name from the fqcn. Maybe the entity was setting a fqcn directly
			String pkg = packageNameFromFullyQualifiedName( fqcn );

			if ( !xmlMappedTypeExists( fqcn ) ) {
				context.logMessage(
						Diagnostic.Kind.WARNING,
						fqcn + " is mapped in xml, but class does not exist. Skipping meta model generation."
				);
				continue;
			}

			XmlMetaEntity metaEntity = new XmlMetaEntity( mappedSuperClass, pkg, getXmlMappedType( fqcn ), context
			);

			if ( context.containsMetaEntity( fqcn ) ) {
				context.logMessage(
						Diagnostic.Kind.WARNING,
						fqcn + " was already processed once. Skipping second occurrence."
				);
			}
			context.addMetaEntity( fqcn, metaEntity );
		}
	}

	private boolean xmlMappedTypeExists(String fullyQualifiedClassName) {
		Elements utils = context.getElementUtils();
		return utils.getTypeElement( fullyQualifiedClassName ) != null;
	}

	private TypeElement getXmlMappedType(String fullyQualifiedClassName) {
		Elements utils = context.getElementUtils();
		return utils.getTypeElement( fullyQualifiedClassName );
	}

	private AccessType determineEntityAccessType(JaxbEntityMappingsImpl mappings) {
		final AccessType contextAccessType = context.getPersistenceUnitDefaultAccessType();
		final AccessType mappingsAccess = mappings.getAccess();
		if ( mappingsAccess != null ) {
			return mappingsAccess;
		}
		return contextAccessType;
	}

	private void determineXmlAccessTypes() {
		for ( JaxbEntityMappingsImpl mappings : entityMappings ) {
			String fqcn;
			String packageName = mappings.getPackage();
			AccessType defaultAccessType = determineEntityAccessType( mappings );

			for ( JaxbEntityImpl entity : mappings.getEntities() ) {
				final String name = entity.getClazz();
				fqcn = determineFullyQualifiedClassName( packageName, name );
				final AccessType explicitAccessType = entity.getAccess();
				final AccessTypeInformation accessInfo = new AccessTypeInformation( fqcn, explicitAccessType, defaultAccessType );
				context.addAccessTypeInformation( fqcn, accessInfo );
			}

			for ( JaxbMappedSuperclassImpl mappedSuperClass : mappings.getMappedSuperclasses() ) {
				final String name = mappedSuperClass.getClazz();
				fqcn = determineFullyQualifiedClassName( packageName, name );
				final AccessType explicitAccessType = mappedSuperClass.getAccess();
				final AccessTypeInformation accessInfo = new AccessTypeInformation( fqcn, explicitAccessType, defaultAccessType );
				context.addAccessTypeInformation( fqcn, accessInfo );
			}

			for ( JaxbEmbeddableImpl embeddable : mappings.getEmbeddables() ) {
				final String name = embeddable.getClazz();
				fqcn = determineFullyQualifiedClassName( packageName, name );
				final AccessType explicitAccessType = embeddable.getAccess();
				final AccessTypeInformation accessInfo = new AccessTypeInformation( fqcn, explicitAccessType, defaultAccessType );
				context.addAccessTypeInformation( fqcn, accessInfo );
			}
		}
	}

	private void determineAnnotationAccessTypes() {
		for ( JaxbEntityMappingsImpl mappings : entityMappings ) {
			String fqcn;
			String packageName = mappings.getPackage();

			for ( JaxbEntityImpl entity : mappings.getEntities() ) {
				String name = entity.getClazz();
				fqcn = determineFullyQualifiedClassName( packageName, name );
				TypeElement element = context.getTypeElementForFullyQualifiedName( fqcn );
				if ( element != null ) {
					TypeUtils.determineAccessTypeForHierarchy( element, context );
				}
			}

			for ( JaxbMappedSuperclassImpl mappedSuperClass : mappings.getMappedSuperclasses() ) {
				String name = mappedSuperClass.getClazz();
				fqcn = determineFullyQualifiedClassName( packageName, name );
				TypeElement element = context.getTypeElementForFullyQualifiedName( fqcn );
				if ( element != null ) {
					TypeUtils.determineAccessTypeForHierarchy( element, context );
				}
			}
		}
	}

	/**
	 * Determines the default access type as specified in the <i>persistence-unit-defaults</i> as well as whether the
	 * xml configuration is complete and annotations should be ignored.
	 * <p>
	 * Note, the spec says:
	 * <ul>
	 * <li>The persistence-unit-metadata element contains metadata for the entire persistence unit. It is
	 * undefined if this element occurs in multiple mapping files within the same persistence unit.</li>
	 * <li>If the xml-mapping-metadata-complete subelement is specified, the complete set of mapping
	 * metadata for the persistence unit is contained in the XML mapping files for the persistence unit, and any
	 * persistence annotations on the classes are ignored.</li>
	 * <li>When the xml-mapping-metadata-complete element is specified, any metadata-complete attributes specified
	 * within the entity, mapped-superclass, and embeddable elements are ignored.<li>
	 * </ul>
	 */
	private void determineDefaultAccessTypeAndMetaCompleteness() {
		for ( JaxbEntityMappingsImpl mappings : entityMappings ) {
			JaxbPersistenceUnitMetadataImpl meta = mappings.getPersistenceUnitMetadata();
			if ( meta != null ) {
				context.mappingDocumentFullyXmlConfigured( meta.getXmlMappingMetadataComplete() != null );

				JaxbPersistenceUnitDefaultsImpl persistenceUnitDefaults = meta.getPersistenceUnitDefaults();
				if ( persistenceUnitDefaults != null ) {
					final jakarta.persistence.AccessType xmlJpaAccessType = persistenceUnitDefaults.getAccess();
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
