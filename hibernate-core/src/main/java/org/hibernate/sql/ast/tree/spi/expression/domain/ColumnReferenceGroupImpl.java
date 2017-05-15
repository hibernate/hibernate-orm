/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;


/**
 * @author Steve Ebersole
 */
public class ColumnReferenceGroupImpl implements ColumnReferenceGroup {
	private List<ColumnReference> columnBindings;

	public ColumnReferenceGroupImpl() {
	}

	public void addColumnBinding(ColumnReference columnBinding) {
		if ( columnBindings == null ) {
			columnBindings = new ArrayList<>();
		}
		columnBindings.add( columnBinding );
	}

	public List<ColumnReference> getColumnReferences() {
		return columnBindings == null ? Collections.emptyList() : Collections.unmodifiableList( columnBindings );
	}
}
