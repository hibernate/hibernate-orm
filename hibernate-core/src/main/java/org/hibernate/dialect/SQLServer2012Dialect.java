/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.dialect;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.internal.util.StringHelper;
/**
 * Microsoft SQL Server 2012 Dialect
 *
 * @author Brett Meyer
 */
public class SQLServer2012Dialect extends SQLServer2008Dialect {

	@Override
	public boolean supportsSequences() {
		return true;
	}

	@Override
	public boolean supportsPooledSequences() {
		return true;
	}

	@Override
	public String getCreateSequenceString(String sequenceName) {
		return "create sequence " + sequenceName;
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence " + sequenceName;
	}

	@Override
	public String getSelectSequenceNextValString(String sequenceName) {
		return "next value for " + sequenceName;
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		return "select " + getSelectSequenceNextValString( sequenceName );
	}

	@Override
	public String getQuerySequencesString() {
		return "select name from sys.sequences";
	}
	
	@Override
	public String getQueryHintString(String sql, List<String> hints) {
		final String hint = StringHelper.join(", ", hints.iterator());

		if (StringHelper.isEmpty(hint)) {
			return sql;
		}

		final StringBuilder buffer = new StringBuilder(sql.length()
				+ hint.length() + 12);
		final int pos = sql.indexOf(";");
		if (pos > -1) {
			buffer.append(sql.substring(0, pos));
		} else {
			buffer.append(sql);
		}
		buffer.append(" OPTION (").append(hint).append(")");
		if (pos > -1) {
			buffer.append(";");
		}
		sql = buffer.toString();

		return sql;
	}
}
