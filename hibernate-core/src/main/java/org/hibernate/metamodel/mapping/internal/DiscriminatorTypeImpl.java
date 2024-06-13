/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.metamodel.mapping.DiscriminatorConverter;
import org.hibernate.metamodel.mapping.DiscriminatorType;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.internal.ConvertedBasicTypeImpl;

/**
 * Specialization of ConvertedBasicTypeImpl to expose access to the
 * {@link #underlyingJdbcMapping} of this discriminator - the bit that handles
 * the relationship between the relational JavaType and the JdbcType
 *
 * @author Steve Ebersole
 */
public class DiscriminatorTypeImpl<O> extends ConvertedBasicTypeImpl<O> implements DiscriminatorType<O> {
	private final JavaType<O> domainJavaType;
	private final BasicType<?> underlyingJdbcMapping;

	public DiscriminatorTypeImpl(
			BasicType<?> underlyingJdbcMapping,
			DiscriminatorConverter<O,?> discriminatorValueConverter) {
		super(
				discriminatorValueConverter.getDiscriminatorName(),
				"Discriminator type " + discriminatorValueConverter.getDiscriminatorName(),
				underlyingJdbcMapping.getJdbcType(),
				discriminatorValueConverter
		);

		assert underlyingJdbcMapping.getJdbcJavaType() == discriminatorValueConverter.getRelationalJavaType();
		this.domainJavaType = discriminatorValueConverter.getDomainJavaType();
		this.underlyingJdbcMapping = underlyingJdbcMapping;
	}

	@Override
	public BasicType<?> getUnderlyingJdbcMapping() {
		return underlyingJdbcMapping;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public DiscriminatorConverter getValueConverter() {
		return (DiscriminatorConverter) super.getValueConverter();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Class getJavaType() {
		return domainJavaType.getJavaTypeClass();
	}

	@Override
	public boolean canDoExtraction() {
		return underlyingJdbcMapping.canDoExtraction();
	}

	@Override
	public JavaType<O> getExpressibleJavaType() {
		return domainJavaType;
	}
}
