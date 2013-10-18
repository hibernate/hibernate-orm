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
package com.hps.hibernate.dialect;

import org.hibernate.dialect.SQLServer2008Dialect;


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
	public String getLimitString(String sql, boolean hasOffset) {
		return new StringBuffer( sql.length()+20 )
				.append( sql )
				.append( hasOffset ? " offset ? rows fetch next ? rows only" : " offset 0 rows fetch next ? rows only" )
				.toString();
	}

	@Override
	public boolean bindLimitParametersInReverseOrder() {
		return false ;
	}

	@Override
	public boolean supportsVariableLimit() {
		return true;
	}

	@Override
	public boolean bindLimitParametersFirst() {
		return false ;
	}

	@Override
	public boolean useMaxForLimit() {
		return false ;
	}

	@Override
	public boolean forceLimitUsage() {
		return true ;
	}

	@Override
	public boolean supportsLimit() {
		return true;
	}

    @Override
    public boolean supportsLimitOffset() {
            return true ;
    }
}
