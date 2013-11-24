/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.type.descriptor.converter;

import javax.persistence.AttributeConverter;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

import org.jboss.logging.Logger;

/**
 * Adapts the Hibernate Type contract to incorporate JPA AttributeConverter calls.
 *
 * @author Steve Ebersole
 */
public class AttributeConverterTypeAdapter<T> extends AbstractSingleColumnStandardBasicType<T> {
	private static final Logger log = Logger.getLogger( AttributeConverterTypeAdapter.class );

	private final String name;

	private final Class modelType;
	private final Class jdbcType;
	private final AttributeConverter<? extends T,?> attributeConverter;

	public AttributeConverterTypeAdapter(
			String name,
			AttributeConverter<? extends T,?> attributeConverter,
			SqlTypeDescriptor sqlTypeDescriptorAdapter,
			Class modelType,
			Class jdbcType,
			JavaTypeDescriptor<T> entityAttributeJavaTypeDescriptor) {
		super( sqlTypeDescriptorAdapter, entityAttributeJavaTypeDescriptor );
		this.name = name;
		this.modelType = modelType;
		this.jdbcType = jdbcType;
		this.attributeConverter = attributeConverter;

		log.debug( "Created AttributeConverterTypeAdapter -> " + name );
	}

	@Override
	public String getName() {
		return name;
	}

	public Class getModelType() {
		return modelType;
	}

	public Class getJdbcType() {
		return jdbcType;
	}

	public AttributeConverter<? extends T,?> getAttributeConverter() {
		return attributeConverter;
	}
}
