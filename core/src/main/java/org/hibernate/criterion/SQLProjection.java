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
package org.hibernate.criterion;


import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.type.Type;
import org.hibernate.util.StringHelper;

/**
 * A SQL fragment. The string {alias} will be replaced by the
 * alias of the root entity.
 */
public class SQLProjection implements Projection {

	private final String sql;
	private final String groupBy;
	private final Type[] types;
	private String[] aliases;
	private String[] columnAliases;
	private boolean grouped;

	public String toSqlString(
			Criteria criteria, 
			int loc, 
			CriteriaQuery criteriaQuery)
	throws HibernateException {
		return StringHelper.replace( sql, "{alias}", criteriaQuery.getSQLAlias(criteria) );
	}

	public String toGroupSqlString(Criteria criteria, CriteriaQuery criteriaQuery)
	throws HibernateException {
		return StringHelper.replace( groupBy, "{alias}", criteriaQuery.getSQLAlias(criteria) );
	}

	public Type[] getTypes(Criteria crit, CriteriaQuery criteriaQuery)
	throws HibernateException {
		return types;
	}

	public String toString() {
		return sql;
	}

	protected SQLProjection(String sql, String[] columnAliases, Type[] types) {
		this(sql, null, columnAliases, types);
	}
	
	protected SQLProjection(String sql, String groupBy, String[] columnAliases, Type[] types) {
		this.sql = sql;
		this.types = types;
		this.aliases = columnAliases;
		this.columnAliases = columnAliases;
		this.grouped = groupBy!=null;
		this.groupBy = groupBy;
	}

	public String[] getAliases() {
		return aliases;
	}
	
	public String[] getColumnAliases(int loc) {
		return columnAliases;
	}
	
	public boolean isGrouped() {
		return grouped;
	}

	public Type[] getTypes(String alias, Criteria crit, CriteriaQuery criteriaQuery) {
		return null; //unsupported
	}

	public String[] getColumnAliases(String alias, int loc) {
		return null; //unsupported
	}
}
