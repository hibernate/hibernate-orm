/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java.spi;

import java.lang.reflect.Type;

import org.hibernate.Incubating;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.format.FormatMapper;
import org.hibernate.type.spi.TypeConfiguration;

@Incubating
public class XmlJavaType<T> extends FormatMapperBasedJavaType<T> {

	public XmlJavaType(
			Type type,
			MutabilityPlan<T> mutabilityPlan,
			TypeConfiguration typeConfiguration) {
		super( type, mutabilityPlan, typeConfiguration );
	}

	@Override
	protected FormatMapper getFormatMapper(TypeConfiguration typeConfiguration) {
		return typeConfiguration.getXmlFormatMapper();
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
		return context.getJdbcType( SqlTypes.SQLXML );
	}

	@Override
	public String toString() {
		return "XmlJavaType(" + getTypeName() + ")";
	}
}
