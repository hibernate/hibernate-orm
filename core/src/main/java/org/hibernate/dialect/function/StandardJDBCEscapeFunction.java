package org.hibernate.dialect.function;

import java.util.List;

import org.hibernate.type.Type;
import org.hibernate.engine.SessionFactoryImplementor;

/**
 * Analogous to {@link org.hibernate.dialect.function.StandardSQLFunction}
 * except that standard JDBC escape sequences (i.e. {fn blah}) are used when
 * rendering the SQL.
 *
 * @author Steve Ebersole
 */
public class StandardJDBCEscapeFunction extends StandardSQLFunction {
	public StandardJDBCEscapeFunction(String name) {
		super( name );
	}

	public StandardJDBCEscapeFunction(String name, Type typeValue) {
		super( name, typeValue );
	}

	public String render(List args, SessionFactoryImplementor factory) {
		return "{fn " + super.render( args, factory ) + "}";
	}

	public String toString() {
		return "{fn " + getName() + "...}";
	}
}
