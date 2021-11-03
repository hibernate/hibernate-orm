/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.type.contribution.jts;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import org.locationtech.jts.geom.Point;

/**
 * @author Steve Ebersole
 */
public class PointJdbcType implements JdbcType {
	public static final int POINT_TYPE_CODE = SqlTypes.GEOMETRY;

	@Override
	public String getFriendlyName() {
		return "POINT";
	}

	@Override
	public int getJdbcTypeCode() {
		return POINT_TYPE_CODE;
	}

	private final ValueBinder<Point> binder = new BasicBinder<Point>( PointJavaType.INSTANCE, this ) {
		@Override
		protected void doBindNull(PreparedStatement st, int index, WrapperOptions options) throws SQLException {
			// according to documentation, can be treated as character data
			st.setNull( index, Types.VARCHAR );
		}

		@Override
		protected void doBind(PreparedStatement st, Point value, int index, WrapperOptions options) throws SQLException {
			st.setObject( index,value );
		}

		@Override
		protected void doBindNull(CallableStatement st, String name, WrapperOptions options) throws SQLException {
			// according to documentation, can be treated as character data
			st.setNull( name, Types.VARCHAR );
		}

		@Override
		protected void doBind(CallableStatement st, Point value, String name, WrapperOptions options) throws SQLException {
			st.setObject( name, value );
		}
	};

	private final ValueExtractor<Point> extractor = new BasicExtractor<Point>( PointJavaType.INSTANCE, this ) {
		@Override
		protected Point doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
			return rs.getObject( paramIndex, Point.class );
		}

		@Override
		protected Point doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
			return statement.getObject( index, Point.class );
		}

		@Override
		protected Point doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
			return statement.getObject( name, Point.class );
		}
	};

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaTypeDescriptor) {
		//noinspection unchecked
		return (ValueBinder<X>) binder;
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaTypeDescriptor) {
		//noinspection unchecked
		return (ValueExtractor<X>) extractor;
	}
}
