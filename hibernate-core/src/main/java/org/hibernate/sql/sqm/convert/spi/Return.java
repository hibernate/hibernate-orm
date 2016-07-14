/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.convert.spi;

import org.hibernate.Incubating;
import org.hibernate.sql.sqm.exec.results.spi.ReturnReader;

/**
 * @author Steve Ebersole
 */
@Incubating
public class Return {
	private final String resultVariable;
	private final ReturnReader returnReader;

	public Return(String resultVariable, ReturnReader returnReader) {
		this.resultVariable = resultVariable;
		this.returnReader = returnReader;
	}

	public String getResultVariable() {
		return resultVariable;
	}

	public ReturnReader getReturnReader() {
		return returnReader;
	}
}
