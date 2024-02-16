/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
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
public class JsonJavaType<T> extends FormatMapperBasedJavaType<T> {

	public JsonJavaType(
			Type type,
			MutabilityPlan<T> mutabilityPlan,
			TypeConfiguration typeConfiguration) {
		super( type, mutabilityPlan, typeConfiguration );
	}

	@Override
	protected FormatMapper getFormatMapper(TypeConfiguration typeConfiguration) {
		return typeConfiguration.getSessionFactory().getFastSessionServices().getJsonFormatMapper();
	}

	@Override
	public JdbcType getRecommendedJdbcType(JdbcTypeIndicators context) {
		return context.getJdbcType( SqlTypes.JSON );
	}

	@Override
	public String toString() {
		return "JsonJavaType(" + getTypeName() + ")";
	}
}
