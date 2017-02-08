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
import org.hibernate.persister.collection.spi.CollectionElement;
import org.hibernate.persister.collection.spi.CollectionElementEntity;
import org.hibernate.persister.collection.spi.CollectionPersister;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.from.ColumnBinding;
import org.hibernate.sql.ast.from.TableGroup;
import org.hibernate.sql.ast.select.Selectable;
import org.hibernate.sql.ast.select.SelectableBasicTypeImpl;
import org.hibernate.sql.ast.select.SelectableEmbeddedTypeImpl;
import org.hibernate.sql.ast.select.SelectableEntityTypeImpl;
import org.hibernate.sql.exec.spi.SqlSelectAstToJdbcSelectConverter;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.EmbeddedType;
import org.hibernate.type.spi.Type;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeElementReferenceExpression implements NavigableReferenceExpression {
	private final CollectionPersister collectionPersister;
	private final ColumnBindingSource columnBindingSource;
	private final PropertyPath propertyPath;

	private final Selectable selectable;
	private final List<ColumnBinding> columnBindings;

	public PluralAttributeElementReferenceExpression(
			CollectionPersister collectionPersister,
			TableGroup columnBindingSource,
			PropertyPath propertyPath,
			boolean isShallow) {
		this.collectionPersister = collectionPersister;
		this.columnBindingSource = columnBindingSource;
		this.propertyPath = propertyPath;

		// todo : why are these casts to Column needed? elementReference.getColumns() returns List<Column>

		final CollectionElement elementReference = collectionPersister.getElementReference();
		switch ( elementReference.getClassification() ) {
			case BASIC: {
				final Column column = (Column) elementReference.getColumns().get( 0 );
				final ColumnBinding columnBinding = columnBindingSource.resolveColumnBinding( column );
				this.columnBindings = Collections.singletonList( columnBinding );
				this.selectable = new SelectableBasicTypeImpl(
						this,
						columnBinding,
						(BasicType) elementReference.getOrmType()
				);
				break;
			}
			case EMBEDDABLE: {
				this.columnBindings = new ArrayList<>();
				for ( Object ugh : elementReference.getColumns() ) {
					final Column column = (Column) ugh;
					this.columnBindings.add( columnBindingSource.resolveColumnBinding( column ) );
				}
				this.selectable = new SelectableEmbeddedTypeImpl(
						this,
						columnBindings,
						(EmbeddedType) elementReference
				);
				break;
			}
			case ONE_TO_MANY:
			case MANY_TO_MANY: {
				final CollectionElementEntity<?> entityElement = (CollectionElementEntity<?>) collectionPersister.getElementReference();
				this.columnBindings = new ArrayList<>();
				for ( Column column : entityElement.getColumns() ) {
					this.columnBindings.add( columnBindingSource.resolveColumnBinding( column ) );
				}
				this.selectable = new SelectableEntityTypeImpl(
						this,
						getNavigablePath(),
						columnBindingSource,
						( (CollectionElementEntity) collectionPersister.getElementReference() ).getEntityPersister(),
						isShallow
				);
				break;
			}
			default: {
				throw new NotYetImplementedException(
						"Resolution of Selectable for plural-attribute elements of classification [" +
								elementReference.getClassification().name() +
								"] not yet implemented"
				);
			}
		}
	}

	@Override
	public Type getType() {
		return null;
	}

	@Override
	public Selectable getSelectable() {
		return selectable;
	}

	@Override
	public void accept(SqlSelectAstToJdbcSelectConverter walker) {
		walker.visitPluralAttributeElement( this );
	}

	@Override
	public PropertyPath getNavigablePath() {
		return propertyPath;
	}

	@Override
	public List<ColumnBinding> getColumnBindings() {
		return columnBindings;
	}

	@Override
	public CollectionElement getNavigable() {
		return collectionPersister.getElementReference();
	}


}
