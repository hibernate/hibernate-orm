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
	private List columns = new ArrayList();
	//private List aliases = new ArrayList();
	private List columnAliases = new ArrayList();
	private String extraSelectList;
	private String[] usedAliases;

	public SelectFragment() {}

	public List getColumns() {
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
		for (int i=0; i<columnNames.length; i++) addColumn( columnNames[i] );
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
		for (int i=0; i<columnNames.length; i++) addColumn( tableAlias, columnNames[i] );
		return this;
	}

	public SelectFragment addColumns(String tableAlias, String[] columnNames, String[] columnAliases) {
		for (int i=0; i<columnNames.length; i++) {
			if ( columnNames[i]!=null ) addColumn( tableAlias, columnNames[i], columnAliases[i] );
		}
		return this;
	}

	public SelectFragment addFormulas(String tableAlias, String[] formulas, String[] formulaAliases) {
		for ( int i=0; i<formulas.length; i++ ) {
			if ( formulas[i]!=null ) addFormula( tableAlias, formulas[i], formulaAliases[i] );
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
		Iterator iter = columns.iterator();
		Iterator columnAliasIter = columnAliases.iterator();
		//HashMap columnsUnique = new HashMap();
		HashSet columnsUnique = new HashSet();
		if (usedAliases!=null) columnsUnique.addAll( Arrays.asList(usedAliases) );
		while ( iter.hasNext() ) {
			String column = (String) iter.next();
			String columnAlias = (String) columnAliasIter.next();
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
