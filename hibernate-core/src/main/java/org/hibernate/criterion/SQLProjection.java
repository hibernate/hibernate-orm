/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.criterion;

import org.hibernate.Criteria;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.type.Type;

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

	protected SQLProjection(String sql, String[] columnAliases, Type[] types) {
		this( sql, null, columnAliases, types );
	}

	protected SQLProjection(String sql, String groupBy, String[] columnAliases, Type[] types) {
		this.sql = sql;
		this.types = types;
		this.aliases = columnAliases;
		this.columnAliases = columnAliases;
		this.grouped = groupBy!=null;
		this.groupBy = groupBy;
	}

	@Override
	public String toSqlString(Criteria criteria, int loc, CriteriaQuery criteriaQuery) {
		return StringHelper.replace( sql, "{alias}", criteriaQuery.getSQLAlias( criteria ) );
	}

	@Override
	public String toGroupSqlString(Criteria criteria, CriteriaQuery criteriaQuery) {
		return StringHelper.replace( groupBy, "{alias}", criteriaQuery.getSQLAlias( criteria ) );
	}

	@Override
	public Type[] getTypes(Criteria crit, CriteriaQuery criteriaQuery) {
		return types;
	}

	@Override
	public String toString() {
		return sql;
	}

	@Override
	public String[] getAliases() {
		return aliases;
	}

	@Override
	public String[] getColumnAliases(int loc) {
		return columnAliases;
	}

	@Override
	public boolean isGrouped() {
		return grouped;
	}

	@Override
	public Type[] getTypes(String alias, Criteria crit, CriteriaQuery criteriaQuery) {
		return null;
	}

	@Override
	public String[] getColumnAliases(String alias, int loc) {
		return null;
	}
}
