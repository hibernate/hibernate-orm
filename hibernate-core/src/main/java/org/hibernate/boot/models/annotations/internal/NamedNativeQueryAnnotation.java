/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CacheModeType;
import org.hibernate.annotations.FlushModeType;
import org.hibernate.annotations.NamedNativeQuery;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedNativeQueryImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbSynchronizedTableImpl;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.models.spi.MutableClassDetails;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;

import static org.hibernate.boot.models.HibernateAnnotations.NAMED_NATIVE_QUERY;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class NamedNativeQueryAnnotation implements NamedNativeQuery {
	private String name;
	private String query;
	private Class<?> resultClass;
	private String resultSetMapping;
	private FlushModeType flushMode;
	boolean cacheable;
	String cacheRegion;
	int fetchSize;
	int timeout;
	String comment;
	CacheStoreMode cacheStoreMode;
	CacheRetrieveMode cacheRetrieveMode;
	CacheModeType cacheMode;
	boolean readOnly;
	String[] querySpaces;
	boolean callable;

	public NamedNativeQueryAnnotation(SourceModelBuildingContext modelContext) {
		resultClass = void.class;
		resultSetMapping = "";
		flushMode = FlushModeType.PERSISTENCE_CONTEXT;
		cacheable = false;
		cacheRegion = "";
		fetchSize = -1;
		timeout = -1;
		comment = "";
		cacheStoreMode = CacheStoreMode.USE;
		cacheRetrieveMode = CacheRetrieveMode.USE;
		cacheMode = CacheModeType.NORMAL;
		readOnly = false;
		querySpaces = new String[0];
		callable = false;
	}

	public NamedNativeQueryAnnotation(NamedNativeQuery annotation, SourceModelBuildingContext modelContext) {
		this.name = annotation.name();
		this.query = annotation.query();
		this.resultClass = annotation.resultClass();
		this.resultSetMapping = annotation.resultSetMapping();
		this.flushMode = annotation.flushMode();
		this.cacheable = annotation.cacheable();
		this.cacheRegion = annotation.cacheRegion();
		this.fetchSize = annotation.fetchSize();
		this.timeout = annotation.timeout();
		this.comment = annotation.comment();
		this.cacheStoreMode = annotation.cacheStoreMode();
		this.cacheRetrieveMode = annotation.cacheRetrieveMode();
		this.cacheMode = annotation.cacheMode();
		this.readOnly = annotation.readOnly();
		this.querySpaces = annotation.querySpaces();
		this.callable = annotation.callable();
	}

	public NamedNativeQueryAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.name = extractJandexValue( annotation, NAMED_NATIVE_QUERY, "name", modelContext );
		this.query = extractJandexValue( annotation, NAMED_NATIVE_QUERY, "query", modelContext );
		this.resultClass = extractJandexValue( annotation, NAMED_NATIVE_QUERY, "resultClass", modelContext );
		this.resultSetMapping = extractJandexValue( annotation, NAMED_NATIVE_QUERY, "resultSetMapping", modelContext );
		this.flushMode = extractJandexValue( annotation, NAMED_NATIVE_QUERY, "flushMode", modelContext );
		this.cacheable = extractJandexValue( annotation, NAMED_NATIVE_QUERY, "cacheable", modelContext );
		this.cacheRegion = extractJandexValue( annotation, NAMED_NATIVE_QUERY, "cacheRegion", modelContext );
		this.fetchSize = extractJandexValue( annotation, NAMED_NATIVE_QUERY, "fetchSize", modelContext );
		this.timeout = extractJandexValue( annotation, NAMED_NATIVE_QUERY, "timeout", modelContext );
		this.comment = extractJandexValue( annotation, NAMED_NATIVE_QUERY, "comment", modelContext );
		this.cacheStoreMode = extractJandexValue( annotation, NAMED_NATIVE_QUERY, "cacheStoreMode", modelContext );
		this.cacheRetrieveMode = extractJandexValue( annotation, NAMED_NATIVE_QUERY, "cacheRetrieveMode", modelContext );
		this.cacheMode = extractJandexValue( annotation, NAMED_NATIVE_QUERY, "cacheMode", modelContext );
		this.readOnly = extractJandexValue( annotation, NAMED_NATIVE_QUERY, "readOnly", modelContext );
		this.querySpaces = extractJandexValue( annotation, NAMED_NATIVE_QUERY, "querySpaces", modelContext );
		this.callable = extractJandexValue( annotation, NAMED_NATIVE_QUERY, "callable", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return NamedNativeQuery.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
	}


	@Override
	public String query() {
		return query;
	}

	public void query(String value) {
		this.query = value;
	}


	@Override
	public Class<?> resultClass() {
		return resultClass;
	}

	public void resultClass(Class<?> value) {
		this.resultClass = value;
	}

	@Override
	public String resultSetMapping() {
		return resultSetMapping;
	}

	public void resultSetMapping(String value) {
		this.resultSetMapping = value;
	}

	@Override
	public FlushModeType flushMode() {
		return flushMode;
	}

	public void flushMode(FlushModeType value) {
		this.flushMode = value;
	}

	@Override
	public boolean cacheable() {
		return cacheable;
	}

	public void cacheable(boolean value) {
		this.cacheable = value;
	}

	@Override
	public String cacheRegion() {
		return cacheRegion;
	}

	public void cacheRegion(String value) {
		this.cacheRegion = value;
	}

	@Override
	public int fetchSize() {
		return fetchSize;
	}

	public void fetchSize(int value) {
		this.fetchSize = value;
	}

	@Override
	public int timeout() {
		return timeout;
	}

	public void timeout(int value) {
		this.timeout = value;
	}

	@Override
	public String comment() {
		return comment;
	}

	public void comment(String value) {
		this.comment = value;
	}

	@Override
	public CacheStoreMode cacheStoreMode() {
		return cacheStoreMode;
	}

	public void cacheStoreMode(CacheStoreMode value) {
		this.cacheStoreMode = value;
	}

	@Override
	public CacheRetrieveMode cacheRetrieveMode() {
		return cacheRetrieveMode;
	}

	public void cacheRetrieveMode(CacheRetrieveMode value) {
		this.cacheRetrieveMode = value;
	}

	@Override
	public CacheModeType cacheMode() {
		return cacheMode;
	}

	public void cacheMode(CacheModeType value) {
		this.cacheMode = value;
	}

	@Override
	public boolean readOnly() {
		return readOnly;
	}

	public void readOnly(boolean value) {
		this.readOnly = value;
	}

	@Override
	public String[] querySpaces() {
		return querySpaces;
	}

	public void querySpaces(String[] value) {
		this.querySpaces = value;
	}

	@Override
	public boolean callable() {
		return callable;
	}

	public void callable(boolean value) {
		this.callable = value;
	}

	public void apply(JaxbNamedNativeQueryImpl jaxbNamedQuery, XmlDocumentContext xmlDocumentContext) {
		name( jaxbNamedQuery.getName() );
		query( jaxbNamedQuery.getQuery() );

		applyResultClassAndSynchronizations( jaxbNamedQuery, xmlDocumentContext );

		if ( StringHelper.isNotEmpty( jaxbNamedQuery.getResultSetMapping() ) ) {
			resultSetMapping( jaxbNamedQuery.getResultSetMapping() );
		}
	}

	private void applyResultClassAndSynchronizations(
			JaxbNamedNativeQueryImpl jaxbNamedQuery,
			XmlDocumentContext xmlDocumentContext) {
		final List<String> syncSpaces = new ArrayList<>();

		if ( StringHelper.isNotEmpty( jaxbNamedQuery.getResultClass() ) ) {
			final MutableClassDetails resultClassDetails = xmlDocumentContext.resolveJavaType( jaxbNamedQuery.getResultClass() );
			syncSpaces.add( resultClassDetails.getName() );
			resultClass( resultClassDetails.toJavaClass() );
		}

		for ( JaxbSynchronizedTableImpl synchronization : jaxbNamedQuery.getSynchronizations() ) {
			syncSpaces.add( synchronization.getTable() );
		}

		if ( CollectionHelper.isNotEmpty( syncSpaces ) ) {
			querySpaces( syncSpaces.toArray( String[]::new ) );
		}
	}
}
