/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.hana;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicJavaDescriptor;
import org.hibernate.type.descriptor.sql.AbstractTemplateSqlTypeDescriptor;
import org.hibernate.type.descriptor.sql.BasicBinder;
import org.hibernate.type.descriptor.sql.BasicExtractor;

import org.geolatte.geom.Point;

public class HANAPointTypeDescriptor extends AbstractTemplateSqlTypeDescriptor {

	/**
	 * An instance of the descrtiptor
	 */
	public static final HANAPointTypeDescriptor INSTANCE = new HANAPointTypeDescriptor();
	private static final long serialVersionUID = -6978798264716544804L;

	@Override
	public int getSqlType() {
		return Types.STRUCT;
	}

	@Override
	public boolean canBeRemapped() {
		return false;
	}

	@Override
	public <X> ValueBinder<X> createBinder(final BasicJavaDescriptor<X> javaTypeDescriptor) {
		return new BasicBinder<X>( javaTypeDescriptor, this ) {

			@Override
			protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
					throws SQLException {
				final Point<?> geometry = getJavaDescriptor().unwrap( value, Point.class, options );
				st.setObject( index, HANASpatialUtils.toEWKB( geometry ) );
			}

			@Override
			protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
					throws SQLException {
				final Point<?> geometry = getJavaDescriptor().unwrap( value, Point.class, options );
				st.setObject( name, HANASpatialUtils.toEWKB( geometry ) );
			}

		};
	}

	@Override
	public <X> ValueExtractor<X> createExtractor(final BasicJavaDescriptor<X> javaTypeDescriptor) {
		return new BasicExtractor<X>( javaTypeDescriptor, this ) {

			@Override
			protected X doExtract(ResultSet rs, String name, WrapperOptions options) throws SQLException {
				return getJavaDescriptor().wrap( HANASpatialUtils.toGeometry( rs.getObject( name ) ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
				return getJavaDescriptor().wrap( HANASpatialUtils.toGeometry( statement.getObject( index ) ), options );
			}

			@Override
			protected X doExtract(CallableStatement statement, String name, WrapperOptions options)
					throws SQLException {
				return getJavaDescriptor().wrap( HANASpatialUtils.toGeometry( statement.getObject( name ) ), options );
			}
		};
	}

}
