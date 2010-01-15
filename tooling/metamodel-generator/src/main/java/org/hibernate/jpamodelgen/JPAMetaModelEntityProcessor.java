// $Id$
/*
* JBoss, Home of Professional Open Source
* Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.hibernate.jpamodelgen;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import static javax.lang.model.SourceVersion.RELEASE_6;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.persistence.AccessType;
import javax.persistence.Embeddable;
import javax.persistence.MappedSuperclass;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;

import org.hibernate.jpamodelgen.annotation.AnnotationMetaEntity;
import org.hibernate.jpamodelgen.xml.XmlMetaEntity;
import org.hibernate.jpamodelgen.xml.jaxb.Entity;
import org.hibernate.jpamodelgen.xml.jaxb.EntityMappings;
import org.hibernate.jpamodelgen.xml.jaxb.ObjectFactory;
import org.hibernate.jpamodelgen.xml.jaxb.Persistence;
import org.hibernate.jpamodelgen.xml.jaxb.PersistenceUnitDefaults;
import org.hibernate.jpamodelgen.xml.jaxb.PersistenceUnitMetadata;

/**
 * Main annotation processor.
 *
 * @author Max Andersen
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(RELEASE_6)
public class JPAMetaModelEntityProcessor extends AbstractProcessor {

	private static final String PATH_SEPARATOR = "/";
	private static final String PERSISTENCE_XML = "/META-INF/persistence.xml";
	private static final String ORM_XML = "/META-INF/orm.xml";
	private static final Boolean ALLOW_OTHER_PROCESSORS_TO_CLAIM_ANNOTATIONS = Boolean.FALSE;
	private static final String ENTITY_ANN = javax.persistence.Entity.class.getName();
	private static final String MAPPED_SUPERCLASS_ANN = MappedSuperclass.class.getName();
	private static final String EMBEDDABLE_ANN = Embeddable.class.getName();
	private static final AccessType DEFAULT_XML_ACCESS_TYPE = AccessType.PROPERTY;
	private static final String PERSISTENCE_XML_XSD = "persistence_2_0.xsd";
	private static final String ORM_XSD = "orm_2_0.xsd";

	private boolean xmlProcessed = false;
	private Context context;

	public void init(ProcessingEnvironment env) {
		super.init( env );
		context = new Context( env );
		context.logMessage( Diagnostic.Kind.NOTE, "Init Processor " + this );
	}

	@Override
	public boolean process(final Set<? extends TypeElement> annotations,
						   final RoundEnvironment roundEnvironment) {

		if ( roundEnvironment.processingOver() ) {
			context.logMessage( Diagnostic.Kind.NOTE, "Last processing round." );
			createMetaModelClasses();
			context.logMessage( Diagnostic.Kind.NOTE, "Finished processing" );
			return ALLOW_OTHER_PROCESSORS_TO_CLAIM_ANNOTATIONS;
		}

		if ( !xmlProcessed ) {
			parsePersistenceXml();
		}

		if ( !hostJPAAnnotations( annotations ) ) {
			context.logMessage( Diagnostic.Kind.NOTE, "Current processing round does not contain entities" );
			return ALLOW_OTHER_PROCESSORS_TO_CLAIM_ANNOTATIONS;
		}

		Set<? extends Element> elements = roundEnvironment.getRootElements();
		for ( Element element : elements ) {
			context.logMessage( Diagnostic.Kind.NOTE, "Processing " + element.toString() );
			handleRootElementAnnotationMirrors( element );
		}

		return ALLOW_OTHER_PROCESSORS_TO_CLAIM_ANNOTATIONS;
	}

	private void createMetaModelClasses() {
		for ( MetaEntity entity : context.getMetaEntitiesToProcess().values() ) {
			context.logMessage( Diagnostic.Kind.NOTE, "Writing meta model for " + entity );
			ClassWriter.writeFile( entity, context );
		}

		//process left over, in most cases is empty
		for ( String className : context.getElementsAlreadyProcessed() ) {
			context.getMetaSuperclassAndEmbeddableToProcess().remove( className );
		}

		for ( MetaEntity entity : context.getMetaSuperclassAndEmbeddableToProcess().values() ) {
			context.logMessage( Diagnostic.Kind.NOTE, "Writing meta model for " + entity );
			ClassWriter.writeFile( entity, context );
		}
	}

	private boolean hostJPAAnnotations(Set<? extends TypeElement> annotations) {
		for ( TypeElement type : annotations ) {
			final String typeName = type.getQualifiedName().toString();
			if ( typeName.equals( ENTITY_ANN ) ) {
				return true;
			}
			else if ( typeName.equals( EMBEDDABLE_ANN ) ) {
				return true;
			}
			else if ( typeName.equals( MAPPED_SUPERCLASS_ANN ) ) {
				return true;
			}
		}
		return false;
	}

	private void parsePersistenceXml() {
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
		xmlProcessed = true;
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

	private boolean xmlMappedTypeExists(String fullyQualifiedClassName) {
		Elements utils = processingEnv.getElementUtils();
		return utils.getTypeElement( fullyQualifiedClassName ) != null;
	}

	private TypeElement getXmlMappedType(String fullyQualifiedClassName) {
		Elements utils = processingEnv.getElementUtils();
		return utils.getTypeElement( fullyQualifiedClassName );
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

	private void handleRootElementAnnotationMirrors(final Element element) {

		List<? extends AnnotationMirror> annotationMirrors = element
				.getAnnotationMirrors();

		for ( AnnotationMirror mirror : annotationMirrors ) {
			final String annotationType = mirror.getAnnotationType().toString();

			if ( element.getKind() == ElementKind.CLASS ) {
				if ( annotationType.equals( ENTITY_ANN ) ) {
					AnnotationMetaEntity metaEntity = new AnnotationMetaEntity( ( TypeElement ) element, context );
					// TODO instead of just adding the entity we have to do some merging.
					context.getMetaEntitiesToProcess().put( metaEntity.getQualifiedName(), metaEntity );
				}
				else if ( annotationType.equals( MAPPED_SUPERCLASS_ANN )
						|| annotationType.equals( EMBEDDABLE_ANN ) ) {
					AnnotationMetaEntity metaEntity = new AnnotationMetaEntity( ( TypeElement ) element, context );

					// TODO instead of just adding the entity we have to do some merging.
					context.getMetaSuperclassAndEmbeddableToProcess().put( metaEntity.getQualifiedName(), metaEntity );
				}
			}
		}
	}

	private InputStream getInputStreamForResource(String resource) {
		String pkg = getPackage( resource );
		String name = getRelativeName( resource );
		context.logMessage( Diagnostic.Kind.NOTE, "Reading resource " + resource );
		InputStream ormStream;
		try {
			FileObject fileObject = processingEnv.getFiler().getResource( StandardLocation.CLASS_OUTPUT, pkg, name );
			ormStream = fileObject.openInputStream();
		}
		catch ( IOException e1 ) {
//			processingEnv.getMessager()
//					.printMessage(
//							Diagnostic.Kind.WARNING,
//							"Could not load " + resource + " using processingEnv.getFiler().getResource(). Using classpath..."
//					);

			// TODO
			// unfortunately, the Filer.getResource API seems not to be able to load from /META-INF. One gets a
			// FilerException with the message with "Illegal name /META-INF". This means that we have to revert to
			// using the classpath. This might mean that we find a persistence.xml which is 'part of another jar.
			// Not sure what else we can do here
			ormStream = this.getClass().getResourceAsStream( resource );
		}
		return ormStream;
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
			context.logMessage( Diagnostic.Kind.NOTE, resource + " not found." );
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
}
