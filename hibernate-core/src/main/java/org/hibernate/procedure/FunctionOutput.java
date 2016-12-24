/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.procedure;

import org.hibernate.result.Output;

/**
 * Used for non-ref-cursor function output
 *
 * @author Steve Ebersole
 */
public class FunctionOutput implements Output {
	private final Object value;

	public FunctionOutput(Object value) {
		this.value = value;
	}

	public Object getValue() {
		return value;
	}

	@Override
	public boolean isResultSet() {
		return false;
	}
}
