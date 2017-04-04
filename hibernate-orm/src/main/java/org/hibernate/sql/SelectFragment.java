/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.hibernate.internal.util.StringHelper;

/**
 * A fragment of an SQL <tt>SELECT</tt> clause
 *
 * @author Gavin King
 */
public class SelectFragment {
	private String suffix;
	private List<String> columns = new ArrayList<String>();
	//private List aliases = new ArrayList();
	private List<String> columnAliases = new ArrayList<String>();
	private String extraSelectList;
	private String[] usedAliases;

	public SelectFragment() {}

	public List<String> getColumns() {
		return columns;
	}

	public String getExtraSelectList() {
		return extraSelectList;
	}

	public SelectFragment setUsedAliases(String[] aliases) {
		usedAliases = aliases;
		return this;
	}

	public SelectFragment setExtraSelectList(String extraSelectList) {
		this.extraSelectList = extraSelectList;
		return this;
	}

	public SelectFragment setExtraSelectList(CaseFragment caseFragment, String fragmentAlias) {
		setExtraSelectList( caseFragment.setReturnColumnName(fragmentAlias, suffix).toFragmentString() );
		return this;
	}

	public SelectFragment setSuffix(String suffix) {
		this.suffix = suffix;
		return this;
	}

	public SelectFragment addColumn(String columnName) {
		addColumn(null, columnName);
		return this;
	}

	public SelectFragment addColumns(String[] columnNames) {
		for ( String columnName : columnNames ) {
			addColumn( columnName );
		}
		return this;
	}

	public SelectFragment addColumn(String tableAlias, String columnName) {
		return addColumn(tableAlias, columnName, columnName);
	}

	public SelectFragment addColumn(String tableAlias, String columnName, String columnAlias) {
		columns.add( StringHelper.qualify(tableAlias, columnName) );
		//columns.add(columnName);
		//aliases.add(tableAlias);
		columnAliases.add(columnAlias);
		return this;
	}

	public SelectFragment addColumns(String tableAlias, String[] columnNames) {
		for ( String columnName : columnNames ) {
			addColumn( tableAlias, columnName );
		}
		return this;
	}

	public SelectFragment addColumns(String tableAlias, String[] columnNames, String[] columnAliases) {
		for (int i=0; i<columnNames.length; i++) {
			if ( columnNames[i]!=null ) {
				addColumn( tableAlias, columnNames[i], columnAliases[i] );
			}
		}
		return this;
	}

	public SelectFragment addFormulas(String tableAlias, String[] formulas, String[] formulaAliases) {
		for ( int i=0; i<formulas.length; i++ ) {
			if ( formulas[i]!=null ) {
				addFormula( tableAlias, formulas[i], formulaAliases[i] );
			}
		}
		return this;
	}

	public SelectFragment addFormula(String tableAlias, String formula, String formulaAlias) {
		columns.add( StringHelper.replace( formula, Template.TEMPLATE, tableAlias ) );
		columnAliases.add(formulaAlias);
		return this;
	}

	public SelectFragment addColumnTemplate(String tableAlias, String columnTemplate, String columnAlias) {
		// In this context, there's no difference between a column template and a formula.
		return addFormula( tableAlias, columnTemplate, columnAlias );
	}

	public SelectFragment addColumnTemplates(String tableAlias, String[] columnTemplates, String[] columnAliases) {
		// In this context, there's no difference between a column template and a formula.
		return addFormulas( tableAlias, columnTemplates, columnAliases );
	}

	public String toFragmentString() {
		StringBuilder buf = new StringBuilder( columns.size() * 10 );
		Iterator<String> iter = columns.iterator();
		Iterator<String> columnAliasIter = columnAliases.iterator();
		//HashMap columnsUnique = new HashMap();
		HashSet<String> columnsUnique = new HashSet<String>();
		if (usedAliases!=null) {
			columnsUnique.addAll( Arrays.asList(usedAliases) );
		}
		while ( iter.hasNext() ) {
			String column = iter.next();
			String columnAlias = columnAliasIter.next();
			//TODO: eventually put this back in, once we think all is fixed
			//Object otherAlias = columnsUnique.put(qualifiedColumn, columnAlias);
			/*if ( otherAlias!=null && !columnAlias.equals(otherAlias) ) {
				throw new AssertionFailure("bug in Hibernate SQL alias generation");
			}*/
			if ( columnsUnique.add(columnAlias) ) {
				buf.append(", ")
					.append(column)
					.append(" as ");
				if (suffix==null) {
					buf.append(columnAlias);
				}
				else {
					buf.append( new Alias(suffix).toAliasString(columnAlias) );
				}
			}
		}
		if (extraSelectList!=null) {
			buf.append(", ")
				.append(extraSelectList);
		}
		return buf.toString();
	}

}
