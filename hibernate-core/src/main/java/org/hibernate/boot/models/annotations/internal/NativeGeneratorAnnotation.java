/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.internal;

import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.TableGenerator;
import org.hibernate.annotations.NativeGenerator;
import org.hibernate.boot.model.internal.GeneratorStrategies;
import org.hibernate.models.spi.ModelsContext;

import java.lang.annotation.Annotation;
import java.util.Map;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class NativeGeneratorAnnotation implements NativeGenerator {
	private SequenceGenerator sequenceForm;
	private TableGenerator tableForm;

	/**
	 * Used in legacy hbm.xml handling.  See {@linkplain GeneratorStrategies#generatorClass}
	 */
	public NativeGeneratorAnnotation() {
		this.sequenceForm = new SequenceGeneratorJpaAnnotation( null );
		this.tableForm = new TableGeneratorJpaAnnotation( null );
	}

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public NativeGeneratorAnnotation(ModelsContext modelContext) {
		this.sequenceForm = new SequenceGeneratorJpaAnnotation( modelContext );
		this.tableForm = new TableGeneratorJpaAnnotation( modelContext );
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public NativeGeneratorAnnotation(NativeGenerator annotation, ModelsContext modelContext) {
		this.sequenceForm = annotation.sequenceForm();
		this.tableForm = annotation.tableForm();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public NativeGeneratorAnnotation(Map<String, Object> attributeValues, ModelsContext modelContext) {
		this.sequenceForm = (SequenceGenerator) attributeValues.get( "sequenceForm" );
		this.tableForm = (TableGenerator) attributeValues.get( "tableForm" );
	}

	@Override
	public SequenceGenerator sequenceForm() {
		return sequenceForm;
	}

	public void sequenceForm(SequenceGenerator sequenceForm) {
		this.sequenceForm = sequenceForm;
	}

	@Override
	public TableGenerator tableForm() {
		return tableForm;
	}

	public void tableForm(TableGenerator tableForm) {
		this.tableForm = tableForm;
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return NativeGenerator.class;
	}
}
