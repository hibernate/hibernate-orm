/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.xml;

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
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.xml.validation.Schema;

import org.hibernate.jpamodelgen.Context;
import org.hibernate.jpamodelgen.util.AccessType;
import org.hibernate.jpamodelgen.util.AccessTypeInformation;
import org.hibernate.jpamodelgen.util.FileTimeStampChecker;
import org.hibernate.jpamodelgen.util.StringUtil;
import org.hibernate.jpamodelgen.util.TypeUtils;
import org.hibernate.jpamodelgen.util.xml.XmlParserHelper;
import org.hibernate.jpamodelgen.util.xml.XmlParsingException;
import org.hibernate.jpamodelgen.xml.jaxb.Entity;
import org.hibernate.jpamodelgen.xml.jaxb.EntityMappings;
import org.hibernate.jpamodelgen.xml.jaxb.Persistence;
import org.hibernate.jpamodelgen.xml.jaxb.PersistenceUnitDefaults;
import org.hibernate.jpamodelgen.xml.jaxb.PersistenceUnitMetadata;

/**
 * Parser for JPA XML descriptors (persistence.xml and referenced mapping files).
 *
 * @author Hardy Ferentschik
 */
public class JpaDescriptorParser {
	private static final String DEFAULT_ORM_XML_LOCATION = "/META-INF/orm.xml";
	private static final String SERIALIZATION_FILE_NAME = "Hibernate-Static-Metamodel-Generator.tmp";

	private static final String PERSISTENCE_SCHEMA = "persistence_2_1.xsd";
	private static final String ORM_SCHEMA = "orm_2_1.xsd";

	private final Context context;
	private final List<EntityMappings> entityMappings;
	private final XmlParserHelper xmlParserHelper;

	public JpaDescriptorParser(Context context) {
		this.context = context;
		this.entityMappings = new ArrayList<EntityMappings>();
		this.xmlParserHelper = new XmlParserHelper( context );
	}

	public void parseXml() {
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

		for ( EntityMappings mappings : entityMappings ) {
			String defaultPackageName = mappings.getPackage();
			parseEntities( mappings.getEntity(), defaultPackageName );
			parseEmbeddable( mappings.getEmbeddable(), defaultPackageName );
			parseMappedSuperClass( mappings.getMappedSuperclass(), defaultPackageName );
		}
	}

	private Collection<String> determineMappingFileNames() {
		Collection<String> mappingFileNames = new ArrayList<String>();

		Persistence persistence = getPersistence();
		if ( persistence != null ) {
			// get mapping file names from persistence.xml
			List<Persistence.PersistenceUnit> persistenceUnits = persistence.getPersistenceUnit();
			for ( Persistence.PersistenceUnit unit : persistenceUnits ) {
				mappingFileNames.addAll( unit.getMappingFile() );
			}
		}

		// /META-INF/orm.xml is implicit
		mappingFileNames.add( DEFAULT_ORM_XML_LOCATION );

		// not really part of the official spec, but the processor allows to specify mapping files directly as
		// command line options
		mappingFileNames.addAll( context.getOrmXmlFiles() );
		return mappingFileNames;
	}

	private Persistence getPersistence() {
		Persistence persistence = null;
		String persistenceXmlLocation = context.getPersistenceXmlLocation();
		InputStream stream = xmlParserHelper.getInputStreamForResource( persistenceXmlLocation );
		if ( stream == null ) {
			return null;
		}

		try {
			Schema schema = xmlParserHelper.getSchema( PERSISTENCE_SCHEMA );
			persistence = xmlParserHelper.getJaxbRoot( stream, Persistence.class, schema );
		}
		catch (XmlParsingException e) {
			context.logMessage(
					Diagnostic.Kind.WARNING, "Unable to parse persistence.xml: " + e.getMessage()
			);
		}

		try {
			stream.close();
		}
		catch (IOException e) {
			// eat it
		}

		return persistence;
	}

	private void loadEntityMappings(Collection<String> mappingFileNames) {
		for ( String mappingFile : mappingFileNames ) {
			InputStream stream = xmlParserHelper.getInputStreamForResource( mappingFile );
			if ( stream == null ) {
				continue;
			}
			EntityMappings mapping = null;
			try {
				Schema schema = xmlParserHelper.getSchema( ORM_SCHEMA );
				mapping = xmlParserHelper.getJaxbRoot( stream, EntityMappings.class, schema );
			}
			catch (XmlParsingException e) {
				context.logMessage(
						Diagnostic.Kind.WARNING, "Unable to parse " + mappingFile + ": " + e.getMessage()
				);
			}
			if ( mapping != null ) {
				entityMappings.add( mapping );
			}

			try {
				stream.close();
			}
			catch (IOException e) {
				// eat it
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
		try {
			File file = getSerializationTmpFile();
			ObjectOutput out = new ObjectOutputStream( new FileOutputStream( file ) );
			out.writeObject( fileStampCheck );
			out.close();
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
		FileTimeStampChecker serializedTimeStampCheck = new FileTimeStampChecker();
		File file = null;
		try {
			file = getSerializationTmpFile();
			if ( file.exists() ) {
				ObjectInputStream in = new ObjectInputStream( new FileInputStream( file ) );
				serializedTimeStampCheck = (FileTimeStampChecker) in.readObject();
				in.close();
			}
		}
		catch (Exception e) {
			// ignore - if the de-serialization failed we just have to keep parsing the xml
			context.logMessage( Diagnostic.Kind.OTHER, "Error de-serializing  " + file );
		}
		return serializedTimeStampCheck;
	}

	private void parseEntities(Collection<Entity> entities, String defaultPackageName) {
		for ( Entity entity : entities ) {
			String fqcn = StringUtil.determineFullyQualifiedClassName( defaultPackageName, entity.getClazz() );

			if ( !xmlMappedTypeExists( fqcn ) ) {
				context.logMessage(
						Diagnostic.Kind.WARNING,
						fqcn + " is mapped in xml, but class does not exist. Skipping meta model generation."
				);
				continue;
			}

			XmlMetaEntity metaEntity = new XmlMetaEntity(
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
			Collection<org.hibernate.jpamodelgen.xml.jaxb.Embeddable> embeddables,
			String defaultPackageName) {
		for ( org.hibernate.jpamodelgen.xml.jaxb.Embeddable embeddable : embeddables ) {
			String fqcn = StringUtil.determineFullyQualifiedClassName( defaultPackageName, embeddable.getClazz() );
			// we have to extract the package name from the fqcn. Maybe the entity was setting a fqcn directly
			String pkg = StringUtil.packageNameFromFqcn( fqcn );

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
			Collection<org.hibernate.jpamodelgen.xml.jaxb.MappedSuperclass> mappedSuperClasses,
			String defaultPackageName) {
		for ( org.hibernate.jpamodelgen.xml.jaxb.MappedSuperclass mappedSuperClass : mappedSuperClasses ) {
			String fqcn = StringUtil.determineFullyQualifiedClassName(
					defaultPackageName, mappedSuperClass.getClazz()
			);
			// we have to extract the package name from the fqcn. Maybe the entity was setting a fqcn directly
			String pkg = StringUtil.packageNameFromFqcn( fqcn );

			if ( !xmlMappedTypeExists( fqcn ) ) {
				context.logMessage(
						Diagnostic.Kind.WARNING,
						fqcn + " is mapped in xml, but class does not exist. Skipping meta model generation."
				);
				continue;
			}

			XmlMetaEntity metaEntity = new XmlMetaEntity(
					mappedSuperClass, pkg, getXmlMappedType( fqcn ), context
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

	private AccessType determineEntityAccessType(EntityMappings mappings) {
		AccessType accessType = context.getPersistenceUnitDefaultAccessType();
		if ( mappings.getAccess() != null ) {
			accessType = mapXmlAccessTypeToJpaAccessType( mappings.getAccess() );
		}
		return accessType;
	}

	private void determineXmlAccessTypes() {
		for ( EntityMappings mappings : entityMappings ) {
			String fqcn;
			String packageName = mappings.getPackage();
			AccessType defaultAccessType = determineEntityAccessType( mappings );

			for ( Entity entity : mappings.getEntity() ) {
				String name = entity.getClazz();
				fqcn = StringUtil.determineFullyQualifiedClassName( packageName, name );
				AccessType explicitAccessType = null;
				org.hibernate.jpamodelgen.xml.jaxb.AccessType type = entity.getAccess();
				if ( type != null ) {
					explicitAccessType = mapXmlAccessTypeToJpaAccessType( type );
				}
				AccessTypeInformation accessInfo = new AccessTypeInformation(
						fqcn, explicitAccessType, defaultAccessType
				);
				context.addAccessTypeInformation( fqcn, accessInfo );
			}

			for ( org.hibernate.jpamodelgen.xml.jaxb.MappedSuperclass mappedSuperClass : mappings.getMappedSuperclass() ) {
				String name = mappedSuperClass.getClazz();
				fqcn = StringUtil.determineFullyQualifiedClassName( packageName, name );
				AccessType explicitAccessType = null;
				org.hibernate.jpamodelgen.xml.jaxb.AccessType type = mappedSuperClass.getAccess();
				if ( type != null ) {
					explicitAccessType = mapXmlAccessTypeToJpaAccessType( type );
				}
				AccessTypeInformation accessInfo = new AccessTypeInformation(
						fqcn, explicitAccessType, defaultAccessType
				);
				context.addAccessTypeInformation( fqcn, accessInfo );
			}

			for ( org.hibernate.jpamodelgen.xml.jaxb.Embeddable embeddable : mappings.getEmbeddable() ) {
				String name = embeddable.getClazz();
				fqcn = StringUtil.determineFullyQualifiedClassName( packageName, name );
				AccessType explicitAccessType = null;
				org.hibernate.jpamodelgen.xml.jaxb.AccessType type = embeddable.getAccess();
				if ( type != null ) {
					explicitAccessType = mapXmlAccessTypeToJpaAccessType( type );
				}
				AccessTypeInformation accessInfo = new AccessTypeInformation(
						fqcn, explicitAccessType, defaultAccessType
				);
				context.addAccessTypeInformation( fqcn, accessInfo );
			}
		}
	}

	private void determineAnnotationAccessTypes() {
		for ( EntityMappings mappings : entityMappings ) {
			String fqcn;
			String packageName = mappings.getPackage();

			for ( Entity entity : mappings.getEntity() ) {
				String name = entity.getClazz();
				fqcn = StringUtil.determineFullyQualifiedClassName( packageName, name );
				TypeElement element = context.getTypeElementForFullyQualifiedName( fqcn );
				if ( element != null ) {
					TypeUtils.determineAccessTypeForHierarchy( element, context );
				}
			}

			for ( org.hibernate.jpamodelgen.xml.jaxb.MappedSuperclass mappedSuperClass : mappings.getMappedSuperclass() ) {
				String name = mappedSuperClass.getClazz();
				fqcn = StringUtil.determineFullyQualifiedClassName( packageName, name );
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
	 * <p/>
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
		for ( EntityMappings mappings : entityMappings ) {
			PersistenceUnitMetadata meta = mappings.getPersistenceUnitMetadata();
			if ( meta != null ) {
				if ( meta.getXmlMappingMetadataComplete() != null ) {
					context.mappingDocumentFullyXmlConfigured( true );
				}
				else {
					context.mappingDocumentFullyXmlConfigured( false );
				}

				PersistenceUnitDefaults persistenceUnitDefaults = meta.getPersistenceUnitDefaults();
				if ( persistenceUnitDefaults != null ) {
					org.hibernate.jpamodelgen.xml.jaxb.AccessType xmlAccessType = persistenceUnitDefaults.getAccess();
					if ( xmlAccessType != null ) {
						context.setPersistenceUnitDefaultAccessType( mapXmlAccessTypeToJpaAccessType( xmlAccessType ) );
					}
				}
			}
			else {
				context.mappingDocumentFullyXmlConfigured( false );
			}
		}
	}

	private AccessType mapXmlAccessTypeToJpaAccessType(org.hibernate.jpamodelgen.xml.jaxb.AccessType xmlAccessType) {
		switch ( xmlAccessType ) {
			case FIELD: {
				return AccessType.FIELD;
			}
			case PROPERTY: {
				return AccessType.PROPERTY;
			}
			default: {
			}
		}
		return null;
	}
}
