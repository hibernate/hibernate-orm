/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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

	public static final String NAME_PREFIX = "converted::";

	private final String name;
	private final String description;

	private final Class modelType;
	private final Class jdbcType;
	private final AttributeConverter<? extends T,?> attributeConverter;

	public AttributeConverterTypeAdapter(
			String name,
			String description,
			AttributeConverter<? extends T,?> attributeConverter,
			SqlTypeDescriptor sqlTypeDescriptorAdapter,
			Class modelType,
			Class jdbcType,
			JavaTypeDescriptor<T> entityAttributeJavaTypeDescriptor) {
		super( sqlTypeDescriptorAdapter, entityAttributeJavaTypeDescriptor );
		this.name = name;
		this.description = description;
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

	@Override
	public String toString() {
		return description;
	}
}
