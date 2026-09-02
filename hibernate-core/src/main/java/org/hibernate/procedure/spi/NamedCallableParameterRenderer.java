/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure.spi;

import org.hibernate.SPI;
import org.hibernate.sql.spi.SqlAppender;

import static org.hibernate.SPI.Role.IMPLEMENT;

/// Renders one named parameter of a JDBC procedure or function call.
///
/// Append the complete named-argument fragment, including its JDBC parameter
/// marker. Do not append a leading separator, quote the supplied name, advance
/// any parameter position, or retain either argument. Hibernate invokes this
/// renderer only after deciding that the parameter name may be passed to the
/// driver.
///
/// @since 8.0
/// @author Steve Ebersole
@FunctionalInterface
@SPI(IMPLEMENT)
public interface NamedCallableParameterRenderer {
	/// Append one named callable argument.
	///
	/// @param sqlAppender the in-flight callable SQL
	/// @param parameterName the non-null, unquoted parameter name
	void render(SqlAppender sqlAppender, String parameterName);
}
