/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression.domain;

import org.hibernate.persister.collection.spi.CollectionIndex;
import org.hibernate.persister.collection.spi.CollectionPersister;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.sql.ast.consume.spi.SqlSelectAstToJdbcSelectConverter;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeIndexReference implements NavigableReference {
	private final NavigableContainerReference containerReference;
	private final ColumnReferenceSource columnReferenceSource;
	private final NavigablePath navigablePath;

	private final CollectionPersister collectionPersister;


	public PluralAttributeIndexReference(
			NavigableContainerReference containerReference,
			CollectionPersister collectionPersister,
			ColumnReferenceSource columnReferenceSource,
			NavigablePath navigablePath) {
		this.containerReference = containerReference;
		this.collectionPersister = collectionPersister;
		this.columnReferenceSource = columnReferenceSource;
		this.navigablePath = navigablePath;
	}


	@Override
	public CollectionIndex getNavigable() {
		return collectionPersister.getIndexDescriptor();
	}

	@Override
	public CollectionIndex getType() {
		return getNavigable();
	}

	@Override
	public CollectionIndex getSelectable() {
		return getNavigable();
	}

	@Override
	public void accept(SqlSelectAstToJdbcSelectConverter walker) {
		walker.visitPluralAttributeIndex( this );
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public ColumnReferenceSource getContributedColumnReferenceSource() {
		return columnReferenceSource;
	}

	@Override
	public NavigableContainerReference getNavigableContainerReference() {
		return containerReference;
	}
}