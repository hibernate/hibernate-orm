/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.spi.id.cte;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Evandro Pires da Silva
 * @author Vlad Mihalcea
 */
public class CTE {

	private String tableName;
	private List<String> columns = new LinkedList<String>();
	private List<Object[]> selectResult = new LinkedList<Object[]>();

	public CTE(String tableName) {
		this.tableName = tableName;
	}

	public CTE addColumn(String columnName) {
		columns.add(columnName);
		return this;
	}

	public CTE addColumns(String[] columns) {
		if (columns != null) {
			for (String column : columns) {
				addColumn(column);
			}
		}
		return this;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String toStatementString() {
		StringBuilder buf = new StringBuilder((columns.size() * 15)
				+ tableName.length() + 10);

		buf.append("with ").append(tableName).append(" ( ");
		Iterator<String> iterator = columns.iterator();
		while (iterator.hasNext()) {
			buf.append(iterator.next());
			if (iterator.hasNext()) {
				buf.append(", ");
			}
		}
		StringBuilder parameters = new StringBuilder();
		for (Object[] result : this.selectResult) {
			if (parameters.length() > 0) {
				parameters.append(", ");
			}
			parameters.append("(");
			for (int i = 0; i < result.length; i++) {
				if (!parameters.toString().endsWith("(")) {
					parameters.append(", ");
				}
				parameters.append("?");
			}
			parameters.append(")");
		}

		buf.append(" ) as ( values ").append(parameters).append(" )");
		return buf.toString();
	}

	public List<Object[]> getSelectResult() {
		return selectResult;
	}

	public void setSelectResult(List<Object[]> selectResult) {
		this.selectResult = selectResult;
	}

}
