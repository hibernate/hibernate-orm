/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.ast.spi;

import org.hibernate.service.Service;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * Strategy for generating parameter markers used in {@linkplain java.sql.PreparedStatement preparable} SQL strings.
 * <p/>
 * Generally Hibernate will use the JDBC standard marker - {@code ?}.  Many JDBC drivers support the
 * use of the "native" marker syntax of the underlying database - e.g. {@code $n}, {@code ?n}, ...
 *
 * @implNote Originally developed as an extension point for use from Hibernate Reactive
 * for Vert.X PostgreSQL drivers which only support the native {@code $n} syntax.
 *
 * @see org.hibernate.cfg.AvailableSettings#DIALECT_NATIVE_PARAM_MARKERS
 *
 * @author Steve Ebersole
 */
public interface ParameterMarkerStrategy extends Service {
	/**
	 * Create a parameter marker
	 *
	 * @param position The 1-based position of the parameter.
	 * @param jdbcType The type of the parameter, if known - may be {@code null}.
	 */
	String createMarker(int position, JdbcType jdbcType);
}
