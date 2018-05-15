/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.procedure;

/**
 * Describes the function return for ProcedureCalls that represent calls to
 * a function ({@code "{? = call ...} syntax) rather that a proc ({@code {call ...} syntax)
 *
 * @author Steve Ebersole
 */
public interface FunctionReturn<T> extends ProcedureParameter<T> {
	int getJdbcTypeCode();
}
