/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function;

import org.hibernate.metamodel.model.domain.AllowableFunctionReturnType;
import org.hibernate.query.sqm.function.NamedSqmFunctionDescriptor;

/**
 * Provides a standard implementation that supports the majority of the HQL
 * functions that are translated to SQL. The Dialect and its sub-classes use
 * this class to provide details required for processing of the associated
 * function.
 *
 * @author David Channon
 */
public class StandardSQLFunction extends NamedSqmFunctionDescriptor {
	private final AllowableFunctionReturnType type;

	public StandardSQLFunction(String name) {
		this( name, null );
	}

	public StandardSQLFunction(String name, AllowableFunctionReturnType type) {
		this( name, true, type );
	}

	public StandardSQLFunction(String name, boolean requiresArguments, AllowableFunctionReturnType type) {
		super( name, requiresArguments, null, null, null);
		this.type = type;
	}

	public String getName() {
		return getFunctionName();
	}

	public AllowableFunctionReturnType getType() {
		return type;
	}
}
