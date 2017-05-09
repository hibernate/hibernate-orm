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

import org.hibernate.persister.collection.spi.CollectionIndex;
import org.hibernate.persister.collection.spi.CollectionPersister;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.tree.spi.from.ColumnReference;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.tree.spi.select.Selectable;
import org.hibernate.sql.ast.tree.spi.select.SelectableBasicTypeImpl;
import org.hibernate.sql.ast.tree.spi.select.SelectableEmbeddedTypeImpl;
import org.hibernate.sql.ast.consume.spi.SqlSelectAstToJdbcSelectConverter;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.EmbeddedType;
import org.hibernate.type.spi.Type;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeIndexReferenceExpression implements NavigableReferenceExpression {
	private final CollectionPersister collectionPersister;
	private final ColumnBindingSource columnBindingSource;
	private final NavigablePath navigablePath;

	private final List<ColumnReference> columnBindings;
	private final Selectable selectable;

	public PluralAttributeIndexReferenceExpression(
			CollectionPersister collectionPersister,
			TableGroup columnBindingSource,
			NavigablePath navigablePath) {
		this.collectionPersister = collectionPersister;
		this.columnBindingSource = columnBindingSource;
		this.navigablePath = navigablePath;

		// todo : why are these casts to Column needed?  indexReference.getColumns() returns List<Column>

		final CollectionIndex indexReference = collectionPersister.getIndexDescriptor();
		switch ( indexReference.getClassification() ) {
			case BASIC: {
				final ColumnReference columnBinding = columnBindingSource.resolveColumnBinding( (Column) indexReference.getColumns().get( 0 ) );
				this.columnBindings = Collections.singletonList( columnBinding );
				this.selectable = new SelectableBasicTypeImpl(
						this,
						columnBinding,
						(BasicType) indexReference.getOrmType()
				);
				break;
			}
			case EMBEDDABLE: {
				this.columnBindings = new ArrayList<>();
				for ( Object ugh : indexReference.getColumns() ) {
					final Column column = (Column) ugh;
					this.columnBindings.add( columnBindingSource.resolveColumnBinding( column ) );
				}
				this.selectable = new SelectableEmbeddedTypeImpl(
						this,
						columnBindings,
						(EmbeddedType) indexReference.getOrmType()
				);
				break;
			}
			case ONE_TO_MANY: {
				throw new NotYetImplementedException(
						"Resolution of Selectable for entity indexes of a plural-attribute not yet implemented"
				);
			}
			case MANY_TO_MANY: {
				throw new NotYetImplementedException(
						"Resolution of Selectable for entity indexes of a plural-attribute not yet implemented"
				);
			}
			default: {
				throw new NotYetImplementedException(
						"Resolution of Selectable for plural-attribute indexes of classification [" +
								indexReference.getClassification().name() +
								"] not yet implemented"
				);
			}
		}
	}


	@Override
	public CollectionIndex getNavigable() {
		return collectionPersister.getIndexDescriptor();
	}

	@Override
	public Type getType() {
		return collectionPersister.getIndexDescriptor().getOrmType();
	}

	@Override
	public Selectable getSelectable() {
		return selectable;
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
	public List<ColumnReference> getColumnReferences() {
		return columnBindings;
	}
}