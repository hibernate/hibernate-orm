package org.hibernate.cfg.annotations.reflection;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityListeners;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.TableGenerator;

import org.dom4j.Element;

import org.hibernate.annotations.common.reflection.AnnotationReader;
import org.hibernate.annotations.common.reflection.MetadataProvider;
import org.hibernate.annotations.common.reflection.java.JavaMetadataProvider;
import org.hibernate.util.ReflectHelper;

/**
 * MetadataProvider aware of the JPA Deployment descriptor
 *
 * @author Emmanuel Bernard
 */
public class JPAMetadataProvider implements MetadataProvider {
	private MetadataProvider delegate = new JavaMetadataProvider();
	private XMLContext xmlContext = new XMLContext();
	private Map<Object, Object> defaults;
	private Map<AnnotatedElement, AnnotationReader> cache = new HashMap<AnnotatedElement, AnnotationReader>(100);

	public AnnotationReader getAnnotationReader(AnnotatedElement annotatedElement) {
		AnnotationReader reader = cache.get( annotatedElement );
		if (reader == null) {
			if ( xmlContext.hasContext() ) {
				reader = new JPAOverridenAnnotationReader( annotatedElement, xmlContext );
			}
			else {
				reader = delegate.getAnnotationReader( annotatedElement );
			}
			cache.put(annotatedElement, reader);
		}
		return reader;
	}

	public Map<Object, Object> getDefaults() {
		if ( defaults == null ) {
			defaults = new HashMap<Object, Object>();
			XMLContext.Default xmlDefaults = xmlContext.getDefault( null );

			defaults.put( "delimited-identifier", xmlDefaults.getDelimitedIdentifier() );
			List<Class> entityListeners = new ArrayList<Class>();
			for ( String className : xmlContext.getDefaultEntityListeners() ) {
				try {
					entityListeners.add( ReflectHelper.classForName( className, this.getClass() ) );
				}
				catch ( ClassNotFoundException e ) {
					throw new IllegalStateException( "Default entity listener class not found: " + className );
				}
			}
			defaults.put( EntityListeners.class, entityListeners );
			for ( Element element : xmlContext.getAllDocuments() ) {
				@SuppressWarnings( "unchecked" )
				List<Element> elements = element.elements( "sequence-generator" );
				List<SequenceGenerator> sequenceGenerators = ( List<SequenceGenerator> ) defaults.get( SequenceGenerator.class );
				if ( sequenceGenerators == null ) {
					sequenceGenerators = new ArrayList<SequenceGenerator>();
					defaults.put( SequenceGenerator.class, sequenceGenerators );
				}
				for ( Element subelement : elements ) {
					sequenceGenerators.add( JPAOverridenAnnotationReader.buildSequenceGeneratorAnnotation( subelement ) );
				}

				elements = element.elements( "table-generator" );
				List<TableGenerator> tableGenerators = ( List<TableGenerator> ) defaults.get( TableGenerator.class );
				if ( tableGenerators == null ) {
					tableGenerators = new ArrayList<TableGenerator>();
					defaults.put( TableGenerator.class, tableGenerators );
				}
				for ( Element subelement : elements ) {
					tableGenerators.add(
							JPAOverridenAnnotationReader.buildTableGeneratorAnnotation(
									subelement, xmlDefaults
							)
					);
				}

				List<NamedQuery> namedQueries = ( List<NamedQuery> ) defaults.get( NamedQuery.class );
				if ( namedQueries == null ) {
					namedQueries = new ArrayList<NamedQuery>();
					defaults.put( NamedQuery.class, namedQueries );
				}
				List<NamedQuery> currentNamedQueries = JPAOverridenAnnotationReader.buildNamedQueries(
						element, false, xmlDefaults
				);
				namedQueries.addAll( currentNamedQueries );

				List<NamedNativeQuery> namedNativeQueries = ( List<NamedNativeQuery> ) defaults.get( NamedNativeQuery.class );
				if ( namedNativeQueries == null ) {
					namedNativeQueries = new ArrayList<NamedNativeQuery>();
					defaults.put( NamedNativeQuery.class, namedNativeQueries );
				}
				List<NamedNativeQuery> currentNamedNativeQueries = JPAOverridenAnnotationReader.buildNamedQueries(
						element, true, xmlDefaults
				);
				namedNativeQueries.addAll( currentNamedNativeQueries );

				List<SqlResultSetMapping> sqlResultSetMappings = ( List<SqlResultSetMapping> ) defaults.get(
						SqlResultSetMapping.class
				);
				if ( sqlResultSetMappings == null ) {
					sqlResultSetMappings = new ArrayList<SqlResultSetMapping>();
					defaults.put( SqlResultSetMapping.class, sqlResultSetMappings );
				}
				List<SqlResultSetMapping> currentSqlResultSetMappings = JPAOverridenAnnotationReader.buildSqlResultsetMappings(
						element, xmlDefaults
				);
				sqlResultSetMappings.addAll( currentSqlResultSetMappings );
			}
		}
		return defaults;
	}

	public XMLContext getXMLContext() {
		return xmlContext;
	}
}
