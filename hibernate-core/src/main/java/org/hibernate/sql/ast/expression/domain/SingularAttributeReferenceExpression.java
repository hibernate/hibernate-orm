/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.expression.domain;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.loader.PropertyPath;
import org.hibernate.persister.common.internal.SingularPersistentAttributeEntity;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.Navigable;
import org.hibernate.persister.common.spi.SingularPersistentAttribute;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.from.ColumnBinding;
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
public class SingularAttributeReferenceExpression implements NavigableReferenceExpression {
	private final ColumnBindingSource columnBindingSource;
	private final SingularPersistentAttribute referencedAttribute;
	private final NavigablePath navigablePath;

	private final Selectable selectable;
	private List<ColumnBinding> columnBindings;

	public SingularAttributeReferenceExpression(
			ColumnBindingSource columnBindingSource,
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
						columnBindingSource.resolveColumnBinding( referencedAttribute.getColumns().get( 0 ) ),
						(BasicType) referencedAttribute.getOrmType()
				);
			}
			case EMBEDDED: {
				return new SelectableEmbeddedTypeImpl(
						this,
						getColumnBindings(),
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
	public Type getType() {
		return referencedAttribute.getOrmType();
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
	public List<ColumnBinding> getColumnBindings() {
		if ( columnBindings == null ) {
			columnBindings = resolveNonSelectColumnBindings();
		}
		return columnBindings;
	}

	private List<ColumnBinding> resolveNonSelectColumnBindings() {
		final List<ColumnBinding> columnBindings = new ArrayList<>();
		for ( Column column : getReferencedAttribute().getColumns() ) {
			columnBindings.add( columnBindingSource.resolveColumnBinding( column ) );
		}
		return columnBindings;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}
}
