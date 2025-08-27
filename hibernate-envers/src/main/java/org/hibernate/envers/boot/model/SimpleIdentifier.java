/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.boot.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmConfigParameterType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmGeneratorSpecificationType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmSimpleIdType;

/**
 * Represents a simple identifier mapping.
 *
 * @author Chris Cranford
 */
public class SimpleIdentifier extends AbstractIdentifier {

	private final String type;
	private final List<Column> columns;
	private final Map<String, String> parameters;
	private String generatorClazz;

	public SimpleIdentifier(String name, String type) {
		super( name );
		this.type = type;
		this.columns = new ArrayList<>();
		this.parameters = new HashMap<>();
	}

	@Override
	public void addAttribute(Attribute attribute) {
		throw new IllegalStateException( "Simple generated identifiers don't support attributes?" );
	}

	public void addColumn(Column column) {
		this.columns.add( column );
	}

	public void setParameter(String name, String value) {
		this.parameters.put( name, value );
	}

	public String getGeneratorClass() {
		return generatorClazz;
	}

	public void setGeneratorClass(String generatorClazz) {
		this.generatorClazz = generatorClazz;
	}

	@Override
	public JaxbHbmSimpleIdType build() {
		final JaxbHbmSimpleIdType identifier = new JaxbHbmSimpleIdType();
		identifier.setName( getName() );
		identifier.setTypeAttribute( type );

		final JaxbHbmGeneratorSpecificationType generator = new JaxbHbmGeneratorSpecificationType();
		generator.setClazz( generatorClazz );

		for ( Map.Entry<String, String> entry : parameters.entrySet() ) {
			final JaxbHbmConfigParameterType param = new JaxbHbmConfigParameterType();
			param.setName( entry.getKey() );
			param.setValue( entry.getValue() );
			generator.getConfigParameters().add( param );
		}

		identifier.setGenerator( generator );

		for ( Column column : columns ) {
			identifier.getColumn().add( column.build() );
		}

		return identifier;
	}
}
