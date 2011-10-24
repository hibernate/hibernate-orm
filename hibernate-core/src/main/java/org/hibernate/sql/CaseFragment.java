/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
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
