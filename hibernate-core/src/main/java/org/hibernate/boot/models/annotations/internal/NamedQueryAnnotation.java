/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.CacheMode;
import org.hibernate.annotations.FlushModeType;
import org.hibernate.annotations.NamedQuery;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedHqlQueryImpl;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import org.hibernate.query.QueryFlushMode;

import static org.hibernate.boot.models.xml.internal.QueryProcessing.interpretFlushMode;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class NamedQueryAnnotation implements NamedQuery {
	private String name;
	private String query;
	private Class<?> resultClass;
	private FlushModeType flushMode;
	private QueryFlushMode flush;
	boolean cacheable;
	String cacheRegion;
	int fetchSize;
	int timeout;
	String comment;
	CacheStoreMode cacheStoreMode;
	CacheRetrieveMode cacheRetrieveMode;
	boolean readOnly;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public NamedQueryAnnotation(ModelsContext modelContext) {
		resultClass = void.class;
		flushMode = FlushModeType.PERSISTENCE_CONTEXT;
		flush = QueryFlushMode.DEFAULT;
		cacheable = false;
		cacheRegion = "";
		fetchSize = -1;
		timeout = -1;
		comment = "";
		cacheStoreMode = CacheStoreMode.USE;
		cacheRetrieveMode = CacheRetrieveMode.USE;
		readOnly = false;
	}

	/**
	 * Used in creating annotation instances from JDK and Jandes variant
	 */
	public NamedQueryAnnotation(NamedQuery annotation, ModelsContext modelContext) {
		this.name = annotation.name();
		this.query = annotation.query();
		this.resultClass = annotation.resultClass();
		this.flushMode = annotation.flushMode();
		this.flush = annotation.flush();
		this.cacheable = annotation.cacheable();
		this.cacheRegion = annotation.cacheRegion();
		this.fetchSize = annotation.fetchSize();
		this.timeout = annotation.timeout();
		this.comment = annotation.comment();
		this.cacheStoreMode = annotation.cacheStoreMode();
		this.cacheRetrieveMode = annotation.cacheRetrieveMode();
		if ( annotation.cacheMode() != CacheMode.NORMAL ) {
			this.cacheStoreMode = annotation.cacheMode().getJpaStoreMode();
			this.cacheRetrieveMode = annotation.cacheMode().getJpaRetrieveMode();
		}
		this.readOnly = annotation.readOnly();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public NamedQueryAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.name = (String) attributeValues.get( "name" );
		this.query = (String) attributeValues.get( "query" );
		this.resultClass = (Class<?>) attributeValues.get( "resultClass" );
		this.flushMode = (FlushModeType) attributeValues.get( "flushMode" );
		this.flush = (QueryFlushMode) attributeValues.get( "flush" );
		this.cacheable = (boolean) attributeValues.get( "cacheable" );
		this.cacheRegion = (String) attributeValues.get( "cacheRegion" );
		this.fetchSize = (int) attributeValues.get( "fetchSize" );
		this.timeout = (int) attributeValues.get( "timeout" );
		this.comment = (String) attributeValues.get( "comment" );
		this.cacheStoreMode = (CacheStoreMode) attributeValues.get( "cacheStoreMode" );
		this.cacheRetrieveMode = (CacheRetrieveMode) attributeValues.get( "cacheRetrieveMode" );
		this.readOnly = (boolean) attributeValues.get( "readOnly" );
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
	public QueryFlushMode flush() {
		return flush;
	}

	public void flush(QueryFlushMode value) {
		this.flush = value;
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
	public CacheMode cacheMode() {
		return CacheMode.fromJpaModes( cacheRetrieveMode, cacheStoreMode );
	}

	@Override
	public boolean readOnly() {
		return readOnly;
	}

	public void readOnly(boolean value) {
		this.readOnly = value;
	}


	public void apply(JaxbNamedHqlQueryImpl jaxbNamedQuery, XmlDocumentContext xmlDocumentContext) {
		name( jaxbNamedQuery.getName() );
		query( jaxbNamedQuery.getQuery() );

		if ( jaxbNamedQuery.isCacheable() != null ) {
			final boolean cacheable = jaxbNamedQuery.isCacheable();
			cacheable( cacheable );
			if ( cacheable ) {
				if ( StringHelper.isNotEmpty( jaxbNamedQuery.getCacheRegion() ) ) {
					cacheRegion( jaxbNamedQuery.getCacheRegion() );
				}

				if ( jaxbNamedQuery.getCacheMode() != null ) {
					if ( jaxbNamedQuery.getCacheMode().isGetEnabled() ) {
						cacheRetrieveMode( CacheRetrieveMode.USE );
					}
					if ( jaxbNamedQuery.getCacheMode().isPutEnabled() ) {
						cacheStoreMode( CacheStoreMode.USE );
					}
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
