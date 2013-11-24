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
package org.hibernate.cfg.annotations.reflection;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityListeners;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQuery;
import javax.persistence.NamedStoredProcedureQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.TableGenerator;

import org.hibernate.annotations.common.reflection.AnnotationReader;
import org.hibernate.annotations.common.reflection.MetadataProvider;
import org.hibernate.annotations.common.reflection.java.JavaMetadataProvider;
import org.hibernate.internal.util.ReflectHelper;

import org.dom4j.Element;

/**
 * MetadataProvider aware of the JPA Deployment descriptor
 *
 * @author Emmanuel Bernard
 */
@SuppressWarnings("unchecked")
public class JPAMetadataProvider implements MetadataProvider, Serializable {
	private transient MetadataProvider delegate = new JavaMetadataProvider();
	private transient Map<Object, Object> defaults;
	private transient Map<AnnotatedElement, AnnotationReader> cache = new HashMap<AnnotatedElement, AnnotationReader>(100);

	//all of the above can be safely rebuilt from XMLContext: only XMLContext this object is serialized
	private XMLContext xmlContext = new XMLContext();

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		ois.defaultReadObject();
		delegate = new JavaMetadataProvider();
		cache = new HashMap<AnnotatedElement, AnnotationReader>(100);
	}
	@Override
	public AnnotationReader getAnnotationReader(AnnotatedElement annotatedElement) {
		AnnotationReader reader = cache.get( annotatedElement );
		if (reader == null) {
			if ( xmlContext.hasContext() ) {
				reader = new JPAOverriddenAnnotationReader( annotatedElement, xmlContext );
			}
			else {
				reader = delegate.getAnnotationReader( annotatedElement );
			}
			cache.put(annotatedElement, reader);
		}
		return reader;
	}
	@Override
	public Map<Object, Object> getDefaults() {
		if ( defaults == null ) {
			defaults = new HashMap<Object, Object>();
			XMLContext.Default xmlDefaults = xmlContext.getDefault( null );

			defaults.put( "schema", xmlDefaults.getSchema() );
			defaults.put( "catalog", xmlDefaults.getCatalog() );
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
					sequenceGenerators.add( JPAOverriddenAnnotationReader.buildSequenceGeneratorAnnotation( subelement ) );
				}

				elements = element.elements( "table-generator" );
				List<TableGenerator> tableGenerators = ( List<TableGenerator> ) defaults.get( TableGenerator.class );
				if ( tableGenerators == null ) {
					tableGenerators = new ArrayList<TableGenerator>();
					defaults.put( TableGenerator.class, tableGenerators );
				}
				for ( Element subelement : elements ) {
					tableGenerators.add(
							JPAOverriddenAnnotationReader.buildTableGeneratorAnnotation(
									subelement, xmlDefaults
							)
					);
				}

				List<NamedQuery> namedQueries = ( List<NamedQuery> ) defaults.get( NamedQuery.class );
				if ( namedQueries == null ) {
					namedQueries = new ArrayList<NamedQuery>();
					defaults.put( NamedQuery.class, namedQueries );
				}
				List<NamedQuery> currentNamedQueries = JPAOverriddenAnnotationReader.buildNamedQueries(
						element, false, xmlDefaults
				);
				namedQueries.addAll( currentNamedQueries );

				List<NamedNativeQuery> namedNativeQueries = ( List<NamedNativeQuery> ) defaults.get( NamedNativeQuery.class );
				if ( namedNativeQueries == null ) {
					namedNativeQueries = new ArrayList<NamedNativeQuery>();
					defaults.put( NamedNativeQuery.class, namedNativeQueries );
				}
				List<NamedNativeQuery> currentNamedNativeQueries = JPAOverriddenAnnotationReader.buildNamedQueries(
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
				List<SqlResultSetMapping> currentSqlResultSetMappings = JPAOverriddenAnnotationReader.buildSqlResultsetMappings(
						element, xmlDefaults
				);
				sqlResultSetMappings.addAll( currentSqlResultSetMappings );

				List<NamedStoredProcedureQuery> namedStoredProcedureQueries = (List<NamedStoredProcedureQuery>)defaults.get( NamedStoredProcedureQuery.class );
				if(namedStoredProcedureQueries==null){
					namedStoredProcedureQueries = new ArrayList<NamedStoredProcedureQuery>(  );
					defaults.put( NamedStoredProcedureQuery.class, namedStoredProcedureQueries );
				}
				List<NamedStoredProcedureQuery> currentNamedStoredProcedureQueries = JPAOverriddenAnnotationReader.buildNamedStoreProcedureQueries(
						element, xmlDefaults
				);
				namedStoredProcedureQueries.addAll( currentNamedStoredProcedureQueries );
			}
		}
		return defaults;
	}

	public XMLContext getXMLContext() {
		return xmlContext;
	}
}
