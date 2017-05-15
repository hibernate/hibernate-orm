/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.tree.spi.expression.domain;

import org.hibernate.persister.collection.spi.CollectionElement;
import org.hibernate.persister.collection.spi.CollectionPersister;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.sql.ast.consume.spi.SqlSelectAstToJdbcSelectConverter;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeElementReference implements NavigableReference {
	private final NavigableContainerReference collectionReference;

	private final CollectionPersister collectionPersister;
	private final ColumnReferenceSource columnReferenceSource;
	private final NavigablePath navigablePath;

	public PluralAttributeElementReference(
			NavigableContainerReference collectionReference,
			CollectionPersister collectionPersister,
			TableGroup columnReferenceSource,
			NavigablePath navigablePath) {
		this.collectionReference = collectionReference;
		this.collectionPersister = collectionPersister;
		this.columnReferenceSource = columnReferenceSource;
		this.navigablePath = navigablePath;
	}

	@Override
	public CollectionElement getType() {
		return getNavigable();
	}

	@Override
	public CollectionElement getSelectable() {
		return getNavigable();
	}

	@Override
	public void accept(SqlSelectAstToJdbcSelectConverter walker) {
		walker.visitPluralAttributeElement( this );
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public NavigableContainerReference getNavigableContainerReference() {
		return collectionReference;
	}

	@Override
	public ColumnReferenceSource getContributedColumnReferenceSource() {
		return columnReferenceSource;
	}

	@Override
	public CollectionElement getNavigable() {
		return collectionPersister.getElementDescriptor();
	}
}
