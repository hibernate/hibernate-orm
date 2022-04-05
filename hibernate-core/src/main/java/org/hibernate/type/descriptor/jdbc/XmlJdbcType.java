/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLXML;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Specialized type mapping for {@code SQLXML} and the XML SQL data type.
 *
 * @author Christian Beikov
 */
public class XmlJdbcType implements JdbcType {
	/**
	 * Singleton access
	 */
	public static final XmlJdbcType INSTANCE = new XmlJdbcType();

	@Override
	public int getJdbcTypeCode() {
		return SqlTypes.SQLXML;
	}

	@Override
	public String toString() {
		return "XmlJdbcType";
	}

	@Override
	public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaType<T> javaType) {
		// No literal support for now
		return null;
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		return new XmlValueBinder<>( javaType, this );
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		return new BasicExtractor<>( javaType, this ) {
			@Override
			protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
				return getObject( rs.getSQLXML( paramIndex ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return getObject( statement.getSQLXML( index ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
				return getObject( statement.getSQLXML( name ), options );
			}

			private X getObject(SQLXML sqlxml, WrapperOptions options) throws SQLException {
				if ( sqlxml == null ) {
					return null;
				}
				return options.getSessionFactory().getFastSessionServices().getXmlFormatMapper().fromString(
						sqlxml.getString(),
						getJavaType(),
						options
				);
			}
		};
	}

	protected static class XmlValueBinder<X> extends BasicBinder<X> {
		public XmlValueBinder(JavaType<X> javaType, JdbcType jdbcType) {
			super( javaType, jdbcType );
		}

		@Override
		protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
				throws SQLException {
			final String xml = options.getSessionFactory().getFastSessionServices().getXmlFormatMapper().toString(
					value,
					getJavaType(),
					options
			);
			SQLXML sqlxml = st.getConnection().createSQLXML();
			sqlxml.setString( xml );
			st.setSQLXML( index, sqlxml );
		}

		@Override
		protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
				throws SQLException {
			final String xml = options.getSessionFactory().getFastSessionServices().getXmlFormatMapper().toString(
					value,
					getJavaType(),
					options
			);
			SQLXML sqlxml = st.getConnection().createSQLXML();
			sqlxml.setString( xml );
			st.setSQLXML( name, sqlxml );
		}
	}
}
