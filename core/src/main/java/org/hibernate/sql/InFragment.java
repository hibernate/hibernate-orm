//$Id: InFragment.java 7022 2005-06-05 06:36:26Z oneovthafew $
package org.hibernate.sql;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.util.StringHelper;

/**
 * An SQL IN expression.
 * <br>
 * <code>... in(...)</code>
 * <br>
 * @author Gavin King
 */
public class InFragment {

	public static final String NULL = "null";
	public static final String NOT_NULL = "not null";

	private String columnName;
	private List values = new ArrayList();

	/**
	 * @param value, an SQL literal, NULL, or NOT_NULL
	 */
	public InFragment addValue(Object value) {
		values.add(value);
		return this;
	}

	public InFragment setColumn(String columnName) {
		this.columnName = columnName;
		return this;
	}

	public InFragment setColumn(String alias, String columnName) {
		this.columnName = StringHelper.qualify(alias, columnName);
		return setColumn(this.columnName);
	}

	public InFragment setFormula(String alias, String formulaTemplate) {
		this.columnName = StringHelper.replace(formulaTemplate, Template.TEMPLATE, alias);
		return setColumn(this.columnName);
	}

	public String toFragmentString() {
		if ( values.size()==0 ) return "1=2";
		StringBuffer buf = new StringBuffer( values.size() * 5 );
		buf.append(columnName);
		//following doesn't handle (null, not null) but unnecessary
		//since this would mean all rows
		if ( values.size()>1 ) {
			boolean allowNull = false;
			buf.append(" in (");
			Iterator iter = values.iterator();
			while ( iter.hasNext() ) {
				Object value = iter.next();
				if ( NULL.equals(value) ) {
					allowNull = true;
				}
				else if ( NOT_NULL.equals(value) ) {
					throw new IllegalArgumentException("not null makes no sense for in expression");
				}
				else {
					buf.append(value);
					buf.append(", ");
				}
			}
			buf.setLength( buf.length()-2 );
			buf.append(')');
			if (allowNull) {
				buf.insert(0, " is null or ")
					.insert(0, columnName)
					.insert(0, '(')
					.append(')');
			}
		}
		else {
			Object value = values.iterator().next();
			if ( NULL.equals(value) ) {
				buf.append(" is null");
			}
			else if ( NOT_NULL.equals(value) ) {
				buf.append(" is not null");
			}
			else {
				buf.append("=").append(value);
			}
		}
		return buf.toString();
	}
}
