/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.query.sqm.function.NamedSqmFunctionDescriptor;
import org.hibernate.type.BasicTypeReference;

/**
 * Simplified API allowing users to contribute
 * {@link org.hibernate.query.sqm.function.SqmFunctionDescriptor}s
 * to HQL.
 *
 * @author David Channon
 */
public class StandardSQLFunction extends NamedSqmFunctionDescriptor {
	private final BasicTypeReference<?> type;

	public StandardSQLFunction(String name) {
		this( name, null );
	}

	public StandardSQLFunction(String name, BasicTypeReference<?> type) {
		this( name, true, type );
	}

	public StandardSQLFunction(String name, boolean useParentheses, BasicTypeReference<?> type) {
		super( name, useParentheses, null, null );
		this.type = type;
	}

	public BasicTypeReference<?> getType() {
		return type;
	}
}
