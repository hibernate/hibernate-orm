/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.query.spi;

import java.io.Serializable;

import org.hibernate.type.Type;

/**
 * Metadata about the query return(s).
 *
 * @author Steve Ebersole
 */
public class ReturnMetadata implements Serializable {
	private final String[] returnAliases;
	private final Type[] returnTypes;

	ReturnMetadata(String[] returnAliases, Type[] returnTypes) {
		this.returnAliases = returnAliases;
		this.returnTypes = returnTypes;
	}

	public String[] getReturnAliases() {
		return returnAliases;
	}

	public Type[] getReturnTypes() {
		return returnTypes;
	}
}
