/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.metamodel.spi;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.metamodel.model.domain.spi.TableReferenceJoinCollector;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.tree.spi.from.CollectionTableGroup;
import org.hibernate.sql.ast.tree.spi.from.TableReference;
import org.hibernate.sql.ast.tree.spi.from.TableReferenceJoin;

/**
 * @author Steve Ebersole
 */
public class ElementColumnReferenceQualifier
		extends AbstractColumnReferenceQualifier
		implements ColumnReferenceQualifier, TableReferenceJoinCollector {
	private CollectionTableGroup collectionTableGroup;
	private TableReference root;
	private List<TableReferenceJoin> joins;

	public ElementColumnReferenceQualifier(String uniqueIdentifier) {
		super( uniqueIdentifier );
	}

	@Override
	public void addRoot(TableReference root) {
		this.root = root;
	}

	@Override
	public void collectTableReferenceJoin(TableReferenceJoin tableReferenceJoin) {
		if ( joins == null ) {
			joins = new ArrayList<>();
		}
		joins.add( tableReferenceJoin );
	}

	@Override
	protected TableReference getPrimaryTableReference() {
		return root;
	}

	@Override
	protected List<TableReferenceJoin> getTableReferenceJoins() {
		return joins;
	}
}
