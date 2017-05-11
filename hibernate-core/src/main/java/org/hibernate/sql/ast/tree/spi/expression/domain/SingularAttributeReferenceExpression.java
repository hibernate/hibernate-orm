/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.tree.spi.expression.domain;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.persister.common.internal.SingularPersistentAttributeEntity;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.Navigable;
import org.hibernate.persister.common.spi.SingularPersistentAttribute;
import org.hibernate.persister.queryable.spi.ExpressableType;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.consume.spi.SqlSelectAstToJdbcSelectConverter;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.ast.tree.spi.select.Selectable;
import org.hibernate.sql.ast.tree.spi.select.SelectableBasicTypeImpl;
import org.hibernate.sql.ast.tree.spi.select.SelectableEmbeddedTypeImpl;
import org.hibernate.sql.ast.tree.spi.select.SelectableEntityTypeImpl;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.EmbeddedType;

/**
 * @author Steve Ebersole
 */
public class SingularAttributeReferenceExpression implements NavigableReferenceExpression {
	private final ColumnReferenceSource columnBindingSource;
	private final SingularPersistentAttribute referencedAttribute;
	private final NavigablePath navigablePath;

	private final Selectable selectable;
	private List<ColumnReference> columnBindings;

	public SingularAttributeReferenceExpression(
			ColumnReferenceSource columnBindingSource,
			SingularPersistentAttribute referencedAttribute,
			NavigablePath navigablePath) {
		this.columnBindingSource = columnBindingSource;
		this.referencedAttribute = referencedAttribute;
		this.navigablePath = navigablePath;

		this.selectable = resolveSelectable( referencedAttribute );
	}

	private Selectable resolveSelectable(SingularPersistentAttribute<?,?> referencedAttribute) {
		switch ( referencedAttribute.getAttributeTypeClassification() ) {
			case BASIC: {
				return new SelectableBasicTypeImpl(
						this,
						columnBindingSource.resolveColumnReference( referencedAttribute.getColumns().get( 0 ) ),
						(BasicType) referencedAttribute.getOrmType()
				);
			}
			case EMBEDDED: {
				return new SelectableEmbeddedTypeImpl(
						this,
						getColumnReferences(),
						(EmbeddedType) referencedAttribute.getOrmType()
				);
			}
			case MANY_TO_ONE:
			case ONE_TO_ONE: {
				final SingularPersistentAttributeEntity entityTypedAttribute = (SingularPersistentAttributeEntity) referencedAttribute;
				return new SelectableEntityTypeImpl(
						this,
						navigablePath,
						columnBindingSource,
						entityTypedAttribute.getAssociatedEntityPersister(),
						// shallow? dunno...
						false
				);
			}
			default: {
				throw new NotYetImplementedException(
						"Resolution of Selectable for singular attribute type [" +
								referencedAttribute.getAttributeTypeClassification().name() +
								"] not yet implemented"
				);
			}
		}
	}

	public SingularPersistentAttribute<?,?> getReferencedAttribute() {
		return referencedAttribute;
	}

	@Override
	public ExpressableType getType() {
		return referencedAttribute;
	}

	@Override
	public void accept(SqlSelectAstToJdbcSelectConverter walker) {
		walker.visitSingularAttributeReference( this );
	}

	@Override
	public Selectable getSelectable() {
		return selectable;
	}

	@Override
	public Navigable getNavigable() {
		return getReferencedAttribute();
	}

	@Override
	public List<ColumnReference> getColumnReferences() {
		if ( columnBindings == null ) {
			columnBindings = resolveNonSelectColumnBindings();
		}
		return columnBindings;
	}

	private List<ColumnReference> resolveNonSelectColumnBindings() {
		final List<ColumnReference> columnBindings = new ArrayList<>();
		for ( Column column : getReferencedAttribute().getColumns() ) {
			columnBindings.add( columnBindingSource.resolveColumnReference( column ) );
		}
		return columnBindings;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public TableGroup getSourceTableGroup() {
		return columnBindingSource.getTableGroup();
	}
}
