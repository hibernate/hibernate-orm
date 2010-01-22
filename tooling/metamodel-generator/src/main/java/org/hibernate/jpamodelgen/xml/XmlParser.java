// $Id:$
/*
* JBoss, Home of Professional Open Source
* Copyright 2009, Red Hat, Inc. and/or its affiliates, and individual contributors
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.hibernate.jpamodelgen.xml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.persistence.AccessType;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;

import org.hibernate.jpamodelgen.Context;
import org.hibernate.jpamodelgen.xml.jaxb.Entity;
import org.hibernate.jpamodelgen.xml.jaxb.EntityMappings;
import org.hibernate.jpamodelgen.xml.jaxb.ObjectFactory;
import org.hibernate.jpamodelgen.xml.jaxb.Persistence;
import org.hibernate.jpamodelgen.xml.jaxb.PersistenceUnitDefaults;
import org.hibernate.jpamodelgen.xml.jaxb.PersistenceUnitMetadata;

/**
 * @author Hardy Ferentschik
 */
public class XmlParser {

	private static final String PERSISTENCE_XML = "/META-INF/persistence.xml";
	private static final String ORM_XML = "/META-INF/orm.xml";
	private static final String PERSISTENCE_XML_XSD = "persistence_2_0.xsd";
	private static final String ORM_XSD = "orm_2_0.xsd";
	private static final String PATH_SEPARATOR = "/";
	private static final AccessType DEFAULT_XML_ACCESS_TYPE = AccessType.PROPERTY;

	private Context context;

	public XmlParser(Context context) {
		this.context = context;
	}

	public void parsePersistenceXml() {
		Persistence persistence = parseXml( PERSISTENCE_XML, Persistence.class, PERSISTENCE_XML_XSD );
		if ( persistence != null ) {
			List<Persistence.PersistenceUnit> persistenceUnits = persistence.getPersistenceUnit();
			for ( Persistence.PersistenceUnit unit : persistenceUnits ) {
				List<String> mappingFiles = unit.getMappingFile();
				for ( String mappingFile : mappingFiles ) {
					parsingOrmXml( mappingFile );
				}
			}
		}
		parsingOrmXml( ORM_XML ); // /META-INF/orm.xml is implicit
	}

	private void parsingOrmXml(String resource) {
		EntityMappings mappings = parseXml( resource, EntityMappings.class, ORM_XSD );
		if ( mappings == null ) {
			return;
		}

		AccessType accessType = determineGlobalAccessType( mappings );

		parseEntities( mappings, accessType );
		parseEmbeddable( mappings, accessType );
		parseMappedSuperClass( mappings, accessType );
	}

	private void parseEntities(EntityMappings mappings, AccessType accessType) {
		String packageName = mappings.getPackage();
		Collection<Entity> entities = mappings.getEntity();
		for ( Entity entity : entities ) {
			String fullyQualifiedClassName = packageName + "." + entity.getClazz();

			if ( !xmlMappedTypeExists( fullyQualifiedClassName ) ) {
				context.logMessage(
						Diagnostic.Kind.WARNING,
						fullyQualifiedClassName + " is mapped in xml, but class does not exists. Skipping meta model generation."
				);
				continue;
			}

			XmlMetaEntity metaEntity = new XmlMetaEntity(
					entity, packageName, getXmlMappedType( fullyQualifiedClassName ),
					context
			);

			if ( context.getMetaEntitiesToProcess().containsKey( fullyQualifiedClassName ) ) {
				context.logMessage(
						Diagnostic.Kind.WARNING,
						fullyQualifiedClassName + " was already processed once. Skipping second occurance."
				);
			}
			context.getMetaEntitiesToProcess().put( fullyQualifiedClassName, metaEntity );
		}
	}

	private void parseEmbeddable(EntityMappings mappings, AccessType accessType) {
		String packageName = mappings.getPackage();
		Collection<org.hibernate.jpamodelgen.xml.jaxb.Embeddable> embeddables = mappings.getEmbeddable();
		for ( org.hibernate.jpamodelgen.xml.jaxb.Embeddable embeddable : embeddables ) {
			String fullyQualifiedClassName = packageName + "." + embeddable.getClazz();

			if ( !xmlMappedTypeExists( fullyQualifiedClassName ) ) {
				context.logMessage(
						Diagnostic.Kind.WARNING,
						fullyQualifiedClassName + " is mapped in xml, but class does not exists. Skipping meta model generation."
				);
				continue;
			}

			XmlMetaEntity metaEntity = new XmlMetaEntity(
					embeddable, packageName, getXmlMappedType( fullyQualifiedClassName ),
					context
			);

			if ( context.getMetaSuperclassAndEmbeddableToProcess().containsKey( fullyQualifiedClassName ) ) {
				context.logMessage(
						Diagnostic.Kind.WARNING,
						fullyQualifiedClassName + " was already processed once. Skipping second occurance."
				);
			}
			context.getMetaSuperclassAndEmbeddableToProcess().put( fullyQualifiedClassName, metaEntity );
		}
	}


	private void parseMappedSuperClass(EntityMappings mappings, AccessType accessType) {
		String packageName = mappings.getPackage();
		Collection<org.hibernate.jpamodelgen.xml.jaxb.MappedSuperclass> mappedSuperClasses = mappings.getMappedSuperclass();
		for ( org.hibernate.jpamodelgen.xml.jaxb.MappedSuperclass mappedSuperClass : mappedSuperClasses ) {
			String fullyQualifiedClassName = packageName + "." + mappedSuperClass.getClazz();

			if ( !xmlMappedTypeExists( fullyQualifiedClassName ) ) {
				context.logMessage(
						Diagnostic.Kind.WARNING,
						fullyQualifiedClassName + " is mapped in xml, but class does not exists. Skipping meta model generation."
				);
				continue;
			}

			XmlMetaEntity metaEntity = new XmlMetaEntity(
					mappedSuperClass, packageName, getXmlMappedType( fullyQualifiedClassName ),
					context
			);

			if ( context.getMetaSuperclassAndEmbeddableToProcess().containsKey( fullyQualifiedClassName ) ) {
				context.logMessage(
						Diagnostic.Kind.WARNING,
						fullyQualifiedClassName + " was already processed once. Skipping second occurance."
				);
			}
			context.getMetaSuperclassAndEmbeddableToProcess().put( fullyQualifiedClassName, metaEntity );
		}
	}

	/**
	 * Tries to open the specified xml file and return an instance of the specified class using JAXB.
	 *
	 * @param resource the xml file name
	 * @param clazz The type of jaxb node to return
	 * @param schemaName The schema to validate against (can be {@code null});
	 *
	 * @return The top level jaxb instance contained in the xml file or {@code null} in case the file could not be found.
	 */
	private <T> T parseXml(String resource, Class<T> clazz, String schemaName) {

		InputStream stream = getInputStreamForResource( resource );

		if ( stream == null ) {
			context.logMessage( Diagnostic.Kind.OTHER, resource + " not found." );
			return null;
		}
		try {
			JAXBContext jc = JAXBContext.newInstance( ObjectFactory.class );
			Unmarshaller unmarshaller = jc.createUnmarshaller();
			if ( schemaName != null ) {
				unmarshaller.setSchema( getSchema( schemaName ) );
			}
			return clazz.cast( unmarshaller.unmarshal( stream ) );
		}
		catch ( JAXBException e ) {
			String message = "Error unmarshalling " + resource + " with exception :\n " + e;
			context.logMessage( Diagnostic.Kind.WARNING, message );
			return null;
		}
		catch ( Exception e ) {
			String message = "Error reading " + resource + " with exception :\n " + e;
			context.logMessage( Diagnostic.Kind.WARNING, message );
			return null;
		}
	}

	private Schema getSchema(String schemaName) {
		Schema schema = null;
		URL schemaUrl = this.getClass().getClassLoader().getResource( schemaName );
		if ( schemaUrl == null ) {
			return schema;
		}

		SchemaFactory sf = SchemaFactory.newInstance( javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI );
		try {
			schema = sf.newSchema( schemaUrl );
		}
		catch ( SAXException e ) {
			context.logMessage(
					Diagnostic.Kind.WARNING, "Unable to create schema for " + schemaName + ": " + e.getMessage()
			);
		}
		return schema;
	}

	private InputStream getInputStreamForResource(String resource) {
		String pkg = getPackage( resource );
		String name = getRelativeName( resource );
		context.logMessage( Diagnostic.Kind.OTHER, "Reading resource " + resource );
		InputStream ormStream;
		try {
			FileObject fileObject = context.getProcessingEnvironment()
					.getFiler()
					.getResource( StandardLocation.CLASS_OUTPUT, pkg, name );
			ormStream = fileObject.openInputStream();
		}
		catch ( IOException e1 ) {
			// TODO
			// unfortunately, the Filer.getResource API seems not to be able to load from /META-INF. One gets a
			// FilerException with the message with "Illegal name /META-INF". This means that we have to revert to
			// using the classpath. This might mean that we find a persistence.xml which is 'part of another jar.
			// Not sure what else we can do here
			ormStream = this.getClass().getResourceAsStream( resource );
		}
		return ormStream;
	}

	private String getPackage(String resourceName) {
		if ( !resourceName.contains( PATH_SEPARATOR ) ) {
			return "";
		}
		else {
			return resourceName.substring( 0, resourceName.lastIndexOf( PATH_SEPARATOR ) );
		}
	}

	private String getRelativeName(String resourceName) {
		if ( !resourceName.contains( PATH_SEPARATOR ) ) {
			return resourceName;
		}
		else {
			return resourceName.substring( resourceName.lastIndexOf( PATH_SEPARATOR ) + 1 );
		}
	}

	private boolean xmlMappedTypeExists(String fullyQualifiedClassName) {
		Elements utils = context.getProcessingEnvironment().getElementUtils();
		return utils.getTypeElement( fullyQualifiedClassName ) != null;
	}

	private TypeElement getXmlMappedType(String fullyQualifiedClassName) {
		Elements utils = context.getProcessingEnvironment().getElementUtils();
		return utils.getTypeElement( fullyQualifiedClassName );
	}


	private AccessType determineGlobalAccessType(EntityMappings mappings) {
		AccessType accessType = DEFAULT_XML_ACCESS_TYPE;

		if ( mappings.getAccess() != null ) {
			accessType = mapXmlAccessTypeToJpaAccessType( mappings.getAccess() );
			return accessType; // no need to check persistence unit default
		}

		PersistenceUnitMetadata meta = mappings.getPersistenceUnitMetadata();
		if ( meta != null ) {
			PersistenceUnitDefaults persistenceUnitDefaults = meta.getPersistenceUnitDefaults();
			if ( persistenceUnitDefaults != null ) {
				org.hibernate.jpamodelgen.xml.jaxb.AccessType xmlAccessType = persistenceUnitDefaults.getAccess();
				if ( xmlAccessType != null ) {
					accessType = mapXmlAccessTypeToJpaAccessType( xmlAccessType );
				}
			}
		}
		return accessType;
	}

	private AccessType mapXmlAccessTypeToJpaAccessType(org.hibernate.jpamodelgen.xml.jaxb.AccessType xmlAccessType) {
		switch ( xmlAccessType ) {
			case FIELD: {
				return AccessType.FIELD;
			}
			case PROPERTY: {
				return AccessType.PROPERTY;
			}
		}
		return null;
	}
}


