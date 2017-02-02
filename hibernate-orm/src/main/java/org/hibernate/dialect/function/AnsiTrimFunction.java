/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.function;

import org.hibernate.engine.spi.SessionFactoryImplementor;

/**
 * Defines support for rendering according to ANSI SQL <tt>TRIM</tt> function specification.
 *
 * @author Steve Ebersole
 */
public class AnsiTrimFunction extends TrimFunctionTemplate {
	protected String render(Options options, String trimSource, SessionFactoryImplementor factory) {
		return String.format(
				"trim(%s %s from %s)",
				options.getTrimSpecification().getName(),
				options.getTrimCharacter(),
				trimSource
		);
	}
}
