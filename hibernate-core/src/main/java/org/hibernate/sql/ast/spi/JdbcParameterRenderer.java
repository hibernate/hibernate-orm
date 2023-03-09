/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.sql.ast.spi;

import org.hibernate.service.Service;
import org.hibernate.sql.ast.internal.JdbcParameterRendererStandard;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * Extension point for rendering parameter markers.
 * <p/>
 * Generally Hibernate will use the JDBC standard marker - {@code ?}.  Many
 * databases support alternative marker syntax, often numbered.
 * <p/>
 * Originally developed as an extension point for use from Hibernate Reactive
 * to handle the fact that some Vert.X drivers (ok, their PGSQL driver) only
 * support native parameter marker syntax instead of the JDBC standard
 *
 * @see org.hibernate.cfg.AvailableSettings#DIALECT_NATIVE_PARAM_MARKERS
 *
 * @author Steve Ebersole
 */
public interface JdbcParameterRenderer extends Service {
	/**
	 * Render the parameter for the given position
	 *
	 * @param position The 1-based position of the parameter.
	 * @param jdbcType The type of the parameter, if known
	 */
	String renderJdbcParameter(int position, JdbcType jdbcType);

	static boolean isStandardRenderer(JdbcParameterRenderer check) {
		return check == null || JdbcParameterRendererStandard.class.equals( check.getClass() );
	}
}
