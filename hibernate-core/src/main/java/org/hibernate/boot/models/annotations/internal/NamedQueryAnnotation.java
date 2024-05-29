/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.CacheMode;
import org.hibernate.annotations.CacheModeType;
import org.hibernate.annotations.FlushModeType;
import org.hibernate.annotations.NamedQuery;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedQueryImpl;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;

import static org.hibernate.boot.models.HibernateAnnotations.NAMED_QUERY;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.xml.internal.QueryProcessing.interpretFlushMode;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class NamedQueryAnnotation implements NamedQuery {
	private String name;
	private String query;
	private Class<?> resultClass;
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

	public NamedQueryAnnotation(SourceModelBuildingContext modelContext) {
		resultClass = void.class;
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
	}

	public NamedQueryAnnotation(NamedQuery annotation, SourceModelBuildingContext modelContext) {
		this.name = annotation.name();
		this.query = annotation.query();
		this.resultClass = annotation.resultClass();
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
	}

	public NamedQueryAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.name = extractJandexValue( annotation, NAMED_QUERY, "name", modelContext );
		this.query = extractJandexValue( annotation, NAMED_QUERY, "query", modelContext );
		this.resultClass = extractJandexValue( annotation, NAMED_QUERY, "resultClass", modelContext );
		this.flushMode = extractJandexValue( annotation, NAMED_QUERY, "flushMode", modelContext );
		this.cacheable = extractJandexValue( annotation, NAMED_QUERY, "cacheable", modelContext );
		this.cacheRegion = extractJandexValue( annotation, NAMED_QUERY, "cacheRegion", modelContext );
		this.fetchSize = extractJandexValue( annotation, NAMED_QUERY, "fetchSize", modelContext );
		this.timeout = extractJandexValue( annotation, NAMED_QUERY, "timeout", modelContext );
		this.comment = extractJandexValue( annotation, NAMED_QUERY, "comment", modelContext );
		this.cacheStoreMode = extractJandexValue( annotation, NAMED_QUERY, "cacheStoreMode", modelContext );
		this.cacheRetrieveMode = extractJandexValue( annotation, NAMED_QUERY, "cacheRetrieveMode", modelContext );
		this.cacheMode = extractJandexValue( annotation, NAMED_QUERY, "cacheMode", modelContext );
		this.readOnly = extractJandexValue( annotation, NAMED_QUERY, "readOnly", modelContext );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return NamedQuery.class;
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


	public void apply(JaxbNamedQueryImpl jaxbNamedQuery, XmlDocumentContext xmlDocumentContext) {
		name( jaxbNamedQuery.getName() );
		query( jaxbNamedQuery.getQuery() );

		if ( jaxbNamedQuery.isCacheable() != null ) {
			final boolean cacheable = jaxbNamedQuery.isCacheable();
			cacheable( cacheable );
			if ( cacheable ) {
				if ( StringHelper.isNotEmpty( jaxbNamedQuery.getCacheRegion() ) ) {
					cacheRegion( jaxbNamedQuery.getCacheRegion() );
				}

				final CacheMode cacheMode = jaxbNamedQuery.getCacheMode();
				if ( cacheMode != null && cacheMode != CacheMode.IGNORE ) {
					cacheMode( CacheModeType.fromCacheMode( cacheMode ) );
				}
			}
		}

		if ( jaxbNamedQuery.getFetchSize() != null ) {
			fetchSize( jaxbNamedQuery.getFetchSize() );
		}

		if ( jaxbNamedQuery.getTimeout() != null ) {
			timeout( jaxbNamedQuery.getTimeout() );
		}

		if ( StringHelper.isNotEmpty( jaxbNamedQuery.getComment() ) ) {
			comment( jaxbNamedQuery.getComment() );
		}

		if ( jaxbNamedQuery.isReadOnly() != null ) {
			readOnly( jaxbNamedQuery.isReadOnly() );
		}
		if ( jaxbNamedQuery.getFlushMode() != null ) {
			flushMode( interpretFlushMode( jaxbNamedQuery.getFlushMode() ) );
		}
	}
}
