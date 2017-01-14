/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.expression.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.loader.PropertyPath;
import org.hibernate.persister.collection.spi.CollectionPersister;
import org.hibernate.persister.collection.spi.CollectionIndex;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.DomainReferenceImplementor;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.from.ColumnBinding;
import org.hibernate.sql.ast.from.TableGroup;
import org.hibernate.sql.ast.select.Selectable;
import org.hibernate.sql.ast.select.SelectableBasicTypeImpl;
import org.hibernate.sql.ast.select.SelectableEmbeddedTypeImpl;
import org.hibernate.sql.exec.spi.SqlAstSelectInterpreter;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.EmbeddedType;
import org.hibernate.type.spi.Type;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeIndexReferenceExpression implements DomainReferenceExpression {
	private final CollectionPersister collectionPersister;
	private final ColumnBindingSource columnBindingSource;
	private final PropertyPath propertyPath;

	private final List<ColumnBinding> columnBindings;
	private final Selectable selectable;

	public PluralAttributeIndexReferenceExpression(
			CollectionPersister collectionPersister,
			TableGroup columnBindingSource,
			PropertyPath propertyPath) {
		this.collectionPersister = collectionPersister;
		this.columnBindingSource = columnBindingSource;
		this.propertyPath = propertyPath;

		// todo : why are these casts to Column needed?  indexReference.getColumns() returns List<Column>

		final CollectionIndex indexReference = collectionPersister.getIndexReference();
		switch ( indexReference.getClassification() ) {
			case BASIC: {
				final ColumnBinding columnBinding = columnBindingSource.resolveColumnBinding( (Column) indexReference.getColumns().get( 0 ) );
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
	public DomainReferenceImplementor getDomainReference() {
		return collectionPersister.getIndexReference();
	}

	@Override
	public Type getType() {
		return collectionPersister.getIndexReference().getOrmType();
	}

	@Override
	public Selectable getSelectable() {
		return selectable;
	}

	@Override
	public void accept(SqlAstSelectInterpreter walker) {
		walker.visitPluralAttributeIndex( this );
	}

	@Override
	public PropertyPath getPropertyPath() {
		return propertyPath;
	}

	@Override
	public List<ColumnBinding> getColumnBindings() {
		return columnBindings;
	}
}