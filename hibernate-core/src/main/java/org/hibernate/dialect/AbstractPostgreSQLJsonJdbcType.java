/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.SQLException;

import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;

/**
 * @author Christian Beikov
 */
public abstract class AbstractPostgreSQLJsonJdbcType extends PostgreSQLPGObjectJdbcType implements AggregateJdbcType {

	private final EmbeddableMappingType embeddableMappingType;

	public AbstractPostgreSQLJsonJdbcType(EmbeddableMappingType embeddableMappingType, String typeName) {
		super( typeName, SqlTypes.JSON );
		this.embeddableMappingType = embeddableMappingType;
	}

	@Override
	public EmbeddableMappingType getEmbeddableMappingType() {
		return embeddableMappingType;
	}

	@Override
	protected <X> X fromString(String string, JavaType<X> javaType, WrapperOptions options) throws SQLException {
		if ( embeddableMappingType != null ) {
			return JsonHelper.fromString(
					embeddableMappingType,
					string,
					javaType.getJavaTypeClass() != Object[].class,
					options
			);
		}
		return options.getSessionFactory().getFastSessionServices().getJsonFormatMapper().fromString(
				string,
				javaType,
				options
		);
	}

	@Override
	protected <X> String toString(X value, JavaType<X> javaType, WrapperOptions options) {
		if ( embeddableMappingType != null ) {
			return JsonHelper.toString( embeddableMappingType, value, options );
		}
		return options.getSessionFactory().getFastSessionServices().getJsonFormatMapper().toString(
				value,
				javaType,
				options
		);
	}

	@Override
	public Object createJdbcValue(Object domainValue, WrapperOptions options) throws SQLException {
		assert embeddableMappingType != null;
		return JsonHelper.toString( embeddableMappingType, domainValue, options );
	}

	@Override
	public Object[] extractJdbcValues(Object rawJdbcValue, WrapperOptions options) throws SQLException {
		assert embeddableMappingType != null;
		return JsonHelper.fromString( embeddableMappingType, (String) rawJdbcValue, false, options );
	}
}
