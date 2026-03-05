/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.annotations.Temporal;
import org.hibernate.models.spi.ModelsContext;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class HistoryPartitioningAnnotation implements Temporal.HistoryPartitioning {
	private String currentPartition;
	private String historyPartition;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public HistoryPartitioningAnnotation(ModelsContext modelContext) {
		this.currentPartition = "";
		this.historyPartition = "";
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public HistoryPartitioningAnnotation(Temporal.HistoryPartitioning annotation, ModelsContext modelContext) {
		this.currentPartition = annotation.currentPartition();
		this.historyPartition = annotation.historyPartition();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public HistoryPartitioningAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.currentPartition = (String) attributeValues.get( "currentPartition" );
		this.historyPartition = (String) attributeValues.get( "historyPartition" );
		if ( this.currentPartition == null ) {
			this.currentPartition = "";
		}
		if ( this.historyPartition == null ) {
			this.historyPartition = "";
		}
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return Temporal.HistoryPartitioning.class;
	}

	@Override
	public String currentPartition() {
		return currentPartition;
	}

	public void currentPartition(String value) {
		this.currentPartition = value;
	}

	@Override
	public String historyPartition() {
		return historyPartition;
	}

	public void historyPartition(String value) {
		this.historyPartition = value;
	}
}
