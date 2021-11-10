/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg.annotations.reflection.internal;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.Collections;
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
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappings;
import org.hibernate.boot.jaxb.mapping.spi.JaxbSequenceGenerator;
import org.hibernate.boot.jaxb.mapping.spi.JaxbTableGenerator;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.ClassLoaderAccess;

/**
 * MetadataProvider aware of the JPA Deployment descriptor (orm.xml, ...).
 *
 * @author Emmanuel Bernard
 */
@SuppressWarnings("unchecked")
public final class JPAXMLOverriddenMetadataProvider implements MetadataProvider {

	private static final MetadataProvider STATELESS_BASE_DELEGATE = new JavaMetadataProvider();

	private final ClassLoaderAccess classLoaderAccess;
	private final XMLContext xmlContext;

	/**
	 * We allow fully disabling XML sources so to improve the efficiency of
	 * the boot process for those not using it.
	 */
	private final boolean xmlMappingEnabled;

	private Map<Object, Object> defaults;
	private Map<AnnotatedElement, AnnotationReader> cache;

	public JPAXMLOverriddenMetadataProvider(BootstrapContext bootstrapContext) {
		this.classLoaderAccess = bootstrapContext.getClassLoaderAccess();
		this.xmlContext = new XMLContext( bootstrapContext );
		this.xmlMappingEnabled = bootstrapContext.getMetadataBuildingOptions().isXmlMappingEnabled();
	}

	//all of the above can be safely rebuilt from XMLContext: only XMLContext this object is serialized
	@Override
	public AnnotationReader getAnnotationReader(AnnotatedElement annotatedElement) {
		if ( cache == null ) {
			cache =  new HashMap<>(50 );
		}
		AnnotationReader reader = cache.get( annotatedElement );
		if (reader == null) {
			if ( xmlContext.hasContext() ) {
				reader = new JPAXMLOverriddenAnnotationReader( annotatedElement, xmlContext, classLoaderAccess );
			}
			else {
				reader = STATELESS_BASE_DELEGATE.getAnnotationReader( annotatedElement );
			}
			cache.put( annotatedElement, reader );
		}
		return reader;
	}

	// @Override
	// FIXME this method was introduced in HCANN 5.1.1: we can't mark it as @Override yet,
	// but it's expected to be invoked when we're running with the right HCANN version.
	public void reset() {
		//It's better to remove the HashMap, as it could grow rather large:
		//when doing a clear() the internal buckets array is not scaled down.
		this.cache = null;
	}

	@Override
	public Map<Object, Object> getDefaults() {
		if ( !xmlMappingEnabled ) {
			return Collections.emptyMap();
		}
		else {
			if ( defaults == null ) {
				defaults = new HashMap<>();
				XMLContext.Default xmlDefaults = xmlContext.getDefaultWithGlobalCatalogAndSchema();

				defaults.put( "schema", xmlDefaults.getSchema() );
				defaults.put( "catalog", xmlDefaults.getCatalog() );
				defaults.put( "delimited-identifier", xmlDefaults.getDelimitedIdentifier() );
				defaults.put( "cascade-persist", xmlDefaults.getCascadePersist() );
				List<Class> entityListeners = new ArrayList<Class>();
				for ( String className : xmlContext.getDefaultEntityListeners() ) {
					try {
						entityListeners.add( classLoaderAccess.classForName( className ) );
					}
					catch ( ClassLoadingException e ) {
						throw new IllegalStateException( "Default entity listener class not found: " + className );
					}
				}
				defaults.put( EntityListeners.class, entityListeners );
				for ( JaxbEntityMappings entityMappings : xmlContext.getAllDocuments() ) {
					List<JaxbSequenceGenerator> jaxbSequenceGenerators = entityMappings.getSequenceGenerator();
					List<SequenceGenerator> sequenceGenerators = ( List<SequenceGenerator> ) defaults.get( SequenceGenerator.class );
					if ( sequenceGenerators == null ) {
						sequenceGenerators = new ArrayList<>();
						defaults.put( SequenceGenerator.class, sequenceGenerators );
					}
					for ( JaxbSequenceGenerator element : jaxbSequenceGenerators ) {
						sequenceGenerators.add( JPAXMLOverriddenAnnotationReader.buildSequenceGeneratorAnnotation( element ) );
					}

					List<JaxbTableGenerator> jaxbTableGenerators = entityMappings.getTableGenerator();
					List<TableGenerator> tableGenerators = ( List<TableGenerator> ) defaults.get( TableGenerator.class );
					if ( tableGenerators == null ) {
						tableGenerators = new ArrayList<>();
						defaults.put( TableGenerator.class, tableGenerators );
					}
					for ( JaxbTableGenerator element : jaxbTableGenerators ) {
						tableGenerators.add(
								JPAXMLOverriddenAnnotationReader.buildTableGeneratorAnnotation(
										element, xmlDefaults
								)
						);
					}

					List<NamedQuery> namedQueries = ( List<NamedQuery> ) defaults.get( NamedQuery.class );
					if ( namedQueries == null ) {
						namedQueries = new ArrayList<>();
						defaults.put( NamedQuery.class, namedQueries );
					}
					List<NamedQuery> currentNamedQueries = JPAXMLOverriddenAnnotationReader.buildNamedQueries(
							entityMappings.getNamedQuery(),
							xmlDefaults,
							classLoaderAccess
					);
					namedQueries.addAll( currentNamedQueries );

					List<NamedNativeQuery> namedNativeQueries = ( List<NamedNativeQuery> ) defaults.get( NamedNativeQuery.class );
					if ( namedNativeQueries == null ) {
						namedNativeQueries = new ArrayList<>();
						defaults.put( NamedNativeQuery.class, namedNativeQueries );
					}
					List<NamedNativeQuery> currentNamedNativeQueries = JPAXMLOverriddenAnnotationReader.buildNamedNativeQueries(
							entityMappings.getNamedNativeQuery(),
							xmlDefaults,
							classLoaderAccess
					);
					namedNativeQueries.addAll( currentNamedNativeQueries );

					List<SqlResultSetMapping> sqlResultSetMappings = ( List<SqlResultSetMapping> ) defaults.get(
							SqlResultSetMapping.class
					);
					if ( sqlResultSetMappings == null ) {
						sqlResultSetMappings = new ArrayList<>();
						defaults.put( SqlResultSetMapping.class, sqlResultSetMappings );
					}
					List<SqlResultSetMapping> currentSqlResultSetMappings = JPAXMLOverriddenAnnotationReader.buildSqlResultsetMappings(
							entityMappings.getSqlResultSetMapping(),
							xmlDefaults,
							classLoaderAccess
					);
					sqlResultSetMappings.addAll( currentSqlResultSetMappings );

					List<NamedStoredProcedureQuery> namedStoredProcedureQueries = (List<NamedStoredProcedureQuery>)defaults.get( NamedStoredProcedureQuery.class );
					if(namedStoredProcedureQueries==null){
						namedStoredProcedureQueries = new ArrayList<>(  );
						defaults.put( NamedStoredProcedureQuery.class, namedStoredProcedureQueries );
					}
					List<NamedStoredProcedureQuery> currentNamedStoredProcedureQueries = JPAXMLOverriddenAnnotationReader
							.buildNamedStoreProcedureQueries(
							entityMappings.getNamedStoredProcedureQuery(),
							xmlDefaults,
							classLoaderAccess
					);
					namedStoredProcedureQueries.addAll( currentNamedStoredProcedureQueries );
				}
			}
			return defaults;
		}
	}

	public XMLContext getXMLContext() {
		return xmlContext;
	}
}
