/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.function;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.Type;

/**
 * Analogous to {@link org.hibernate.dialect.function.StandardSQLFunction}
 * except that standard JDBC escape sequences (i.e. {fn blah}) are used when
 * rendering the SQL.
 *
 * @author Steve Ebersole
 */
public class StandardJDBCEscapeFunction extends StandardSQLFunction {
	/**
	 * Constructs a StandardJDBCEscapeFunction
	 *
	 * @param name The function name
	 * @param typeValue The function return type
	 */
	public StandardJDBCEscapeFunction(String name, Type typeValue) {
		super( name, typeValue );
	}

	@Override
	public String render(Type argumentType, List args, SessionFactoryImplementor factory) {
		return "{fn " + super.render( argumentType, args, factory ) + "}";
	}

	@Override
	public String toString() {
		return "{fn " + getName() + "...}";
	}
}
