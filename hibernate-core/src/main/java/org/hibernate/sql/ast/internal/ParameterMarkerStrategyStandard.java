/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.ast.internal;

import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * The standard ParameterMarkerStrategy based on the standard JDBC {@code ?} marker
 *
 * @author Steve Ebersole
 */
public class ParameterMarkerStrategyStandard implements ParameterMarkerStrategy {
	/**
	 * Singleton access
	 */
	public static final ParameterMarkerStrategyStandard INSTANCE = new ParameterMarkerStrategyStandard();

	@Override
	public String createMarker(int position, JdbcType jdbcType) {
		return "?";
	}

	public static boolean isStandardRenderer(ParameterMarkerStrategy check) {
		return check == null || ParameterMarkerStrategyStandard.class.equals( check.getClass() );
	}
}
