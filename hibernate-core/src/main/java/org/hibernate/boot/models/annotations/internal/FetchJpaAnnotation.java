/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.models.spi.ModelsContext;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.Fetch;
import jakarta.persistence.FetchType;
import jakarta.persistence.QueryHint;

import static org.hibernate.boot.models.JpaAnnotations.FETCH;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class FetchJpaAnnotation implements Fetch {
	private String graph;
	private String[] subgraph;
	private FetchType type;
	private int batchSize;
	private CacheStoreMode cacheStoreMode;
	private CacheRetrieveMode cacheRetrieveMode;
	private QueryHint[] hints;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public FetchJpaAnnotation(ModelsContext modelContext) {
		this.graph = "";
		this.subgraph = new String[0];
		this.type = FetchType.EAGER;
		this.batchSize = -1;
		this.cacheStoreMode = CacheStoreMode.USE;
		this.cacheRetrieveMode = CacheRetrieveMode.USE;
		this.hints = new QueryHint[0];
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public FetchJpaAnnotation(Fetch annotation, ModelsContext modelContext) {
		this.graph = annotation.graph();
		this.subgraph = annotation.subgraph();
		this.type = annotation.type();
		this.batchSize = annotation.batchSize();
		this.cacheStoreMode = annotation.cacheStoreMode();
		this.cacheRetrieveMode = annotation.cacheRetrieveMode();
		this.hints = extractJdkValue( annotation, FETCH, "hints", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public FetchJpaAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.graph = (String) attributeValues.get( "graph" );
		this.subgraph = (String[]) attributeValues.get( "subgraph" );
		this.type = (FetchType) attributeValues.get( "type" );
		this.batchSize = (int) attributeValues.get( "batchSize" );
		this.cacheStoreMode = (CacheStoreMode) attributeValues.get( "cacheStoreMode" );
		this.cacheRetrieveMode = (CacheRetrieveMode) attributeValues.get( "cacheRetrieveMode" );
		this.hints = (QueryHint[]) attributeValues.get( "hints" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Fetch.class;
	}

	@Override
	public String graph() {
		return graph;
	}

	public void graph(String value) {
		this.graph = value;
	}

	@Override
	public String[] subgraph() {
		return subgraph;
	}

	public void subgraph(String[] value) {
		this.subgraph = value;
	}

	@Override
	public FetchType type() {
		return type;
	}

	public void type(FetchType value) {
		this.type = value;
	}

	@Override
	public int batchSize() {
		return batchSize;
	}

	public void batchSize(int value) {
		this.batchSize = value;
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
	public QueryHint[] hints() {
		return hints;
	}

	public void hints(QueryHint[] value) {
		this.hints = value;
	}
}
