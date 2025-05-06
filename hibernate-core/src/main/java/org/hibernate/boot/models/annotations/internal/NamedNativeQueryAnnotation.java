/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.CacheMode;
import org.hibernate.annotations.FlushModeType;
import org.hibernate.annotations.NamedNativeQuery;
import org.hibernate.boot.jaxb.mapping.spi.JaxbNamedNativeQueryImpl;
import org.hibernate.boot.jaxb.mapping.spi.JaxbSynchronizedTableImpl;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.models.spi.MutableClassDetails;
import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import org.hibernate.query.QueryFlushMode;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class NamedNativeQueryAnnotation implements NamedNativeQuery {
	private String name;
	private String query;
	private Class<?> resultClass;
	private String resultSetMapping;
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
	String[] querySpaces;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public NamedNativeQueryAnnotation(ModelsContext modelContext) {
		resultClass = void.class;
		resultSetMapping = "";
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
		querySpaces = new String[0];
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public NamedNativeQueryAnnotation(NamedNativeQuery annotation, ModelsContext modelContext) {
		this.name = annotation.name();
		this.query = annotation.query();
		this.resultClass = annotation.resultClass();
		this.resultSetMapping = annotation.resultSetMapping();
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
		this.querySpaces = annotation.querySpaces();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public NamedNativeQueryAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.name = (String) attributeValues.get( "name" );
		this.query = (String) attributeValues.get( "query" );
		this.resultClass = (Class<?>) attributeValues.get( "resultClass" );
		this.resultSetMapping = (String) attributeValues.get( "resultSetMapping" );
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
		this.querySpaces = (String[]) attributeValues.get( "querySpaces" );
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

	@Override
	public String[] querySpaces() {
		return querySpaces;
	}

	public void querySpaces(String[] value) {
		this.querySpaces = value;
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
