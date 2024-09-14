/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;
import java.util.Map;

import org.hibernate.boot.jaxb.mapping.spi.JaxbStoredProcedureParameterImpl;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.SourceModelBuildingContext;

import jakarta.persistence.StoredProcedureParameter;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class StoredProcedureParameterJpaAnnotation implements StoredProcedureParameter {
	private String name;
	private jakarta.persistence.ParameterMode mode;
	private java.lang.Class<?> type;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public StoredProcedureParameterJpaAnnotation(SourceModelBuildingContext modelContext) {
		this.name = "";
		this.mode = jakarta.persistence.ParameterMode.IN;
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public StoredProcedureParameterJpaAnnotation(
			StoredProcedureParameter annotation,
			SourceModelBuildingContext modelContext) {
		this.name = annotation.name();
		this.mode = annotation.mode();
		this.type = annotation.type();
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public StoredProcedureParameterJpaAnnotation(
			Map<String, Object> attributeValues,
			SourceModelBuildingContext modelContext) {
		this.name = (String) attributeValues.get( "name" );
		this.mode = (jakarta.persistence.ParameterMode) attributeValues.get( "mode" );
		this.type = (Class<?>) attributeValues.get( "type" );
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return StoredProcedureParameter.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
	}


	@Override
	public jakarta.persistence.ParameterMode mode() {
		return mode;
	}

	public void mode(jakarta.persistence.ParameterMode value) {
		this.mode = value;
	}


	@Override
	public java.lang.Class<?> type() {
		return type;
	}

	public void type(java.lang.Class<?> value) {
		this.type = value;
	}


	public void apply(JaxbStoredProcedureParameterImpl jaxbParam, XmlDocumentContext xmlDocumentContext) {
		if ( StringHelper.isNotEmpty( jaxbParam.getName() ) ) {
			name( jaxbParam.getName() );
		}

		if ( jaxbParam.getMode() != null ) {
			mode( jaxbParam.getMode() );
		}

		if ( StringHelper.isNotEmpty( jaxbParam.getClazz() ) ) {
			type( xmlDocumentContext.resolveJavaType( jaxbParam.getClazz() ).toJavaClass() );
		}
	}
}
