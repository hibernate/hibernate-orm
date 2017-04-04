/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.internal.util.StringHelper;

/**
 * Abstract SQL case fragment renderer
 *
 * @author Gavin King, Simon Harris
 */
public abstract class CaseFragment {
	public abstract String toFragmentString();

	protected String returnColumnName;

	protected Map cases = new LinkedHashMap();

	public CaseFragment setReturnColumnName(String returnColumnName) {
		this.returnColumnName = returnColumnName;
		return this;
	}

	public CaseFragment setReturnColumnName(String returnColumnName, String suffix) {
		return setReturnColumnName( new Alias(suffix).toAliasString(returnColumnName) );
	}

	public CaseFragment addWhenColumnNotNull(String alias, String columnName, String value) {
		cases.put( StringHelper.qualify( alias, columnName ), value );
		return this;
	}
}
