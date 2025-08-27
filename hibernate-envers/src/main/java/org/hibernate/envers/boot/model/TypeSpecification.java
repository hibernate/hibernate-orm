/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.boot.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmConfigParameterType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmTypeSpecificationType;

/**
 * Contract for a type specification mapping.
 *
 * @author Chris Cranford
 */
public class TypeSpecification implements ConfigParameterContainer, Bindable<JaxbHbmTypeSpecificationType>, Cloneable<TypeSpecification> {

	private final String name;
	private final Map<String, String> parameters;

	public TypeSpecification(String name) {
		this.name = name;
		this.parameters = new HashMap<>();
	}

	public TypeSpecification(TypeSpecification other) {
		this.name = other.name;
		this.parameters = new HashMap<>();
		for ( Map.Entry<String, String> entry : other.parameters.entrySet() ) {
			setParameter( entry.getKey(), entry.getValue() );
		}
	}

	@Override
	public Map<String, String> getParameters() {
		return Collections.unmodifiableMap( parameters );
	}

	@Override
	public void setParameter(String name, String value) {
		this.parameters.put( name, value );
	}

	@Override
	public JaxbHbmTypeSpecificationType build() {
		final JaxbHbmTypeSpecificationType definition = new JaxbHbmTypeSpecificationType();
		definition.setName(name);

		for ( Map.Entry<String, String> entry : parameters.entrySet() ) {
			final JaxbHbmConfigParameterType param = new JaxbHbmConfigParameterType();
			param.setName( entry.getKey() );
			param.setValue( entry.getValue() );

			definition.getConfigParameters().add( param );
		}

		return definition;
	}

	@Override
	public TypeSpecification deepCopy() {
		return new TypeSpecification( this );
	}
}
