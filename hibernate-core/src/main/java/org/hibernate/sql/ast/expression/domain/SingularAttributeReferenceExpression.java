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
import org.hibernate.persister.common.internal.SingularAttributeEntity;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.DomainReferenceImplementor;
import org.hibernate.persister.common.spi.SingularAttribute;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.from.ColumnBinding;
import org.hibernate.sql.ast.select.Selectable;
import org.hibernate.sql.ast.select.SelectableBasicTypeImpl;
import org.hibernate.sql.ast.select.SelectableEmbeddedTypeImpl;
import org.hibernate.sql.ast.select.SelectableEntityTypeImpl;
import org.hibernate.sql.exec.spi.SqlAstSelectInterpreter;
import org.hibernate.type.BasicType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.Type;

/**
 * @author Steve Ebersole
 */
public class SingularAttributeReferenceExpression implements DomainReferenceExpression {
	private final ColumnBindingSource columnBindingSource;
	private final SingularAttribute referencedAttribute;
	private final PropertyPath propertyPath;

	private final Selectable selectable;
	private List<ColumnBinding> columnBindings;

	public SingularAttributeReferenceExpression(
			ColumnBindingSource columnBindingSource,
			SingularAttribute referencedAttribute,
			PropertyPath propertyPath) {
		this.columnBindingSource = columnBindingSource;
		this.referencedAttribute = referencedAttribute;
		this.propertyPath = propertyPath;

		this.selectable = resolveSelectable( referencedAttribute );
	}

	private Selectable resolveSelectable(SingularAttribute referencedAttribute) {
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
						(CompositeType) referencedAttribute.getOrmType()
				);
			}
			case MANY_TO_ONE:
			case ONE_TO_ONE: {
				final SingularAttributeEntity entityTypedAttribute = (SingularAttributeEntity) referencedAttribute;
				return new SelectableEntityTypeImpl(
						this,
						propertyPath,
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

	public SingularAttribute getReferencedAttribute() {
		return referencedAttribute;
	}

	@Override
	public Type getType() {
		return referencedAttribute.getOrmType();
	}

	@Override
	public void accept(SqlAstSelectInterpreter walker) {
		walker.visitSingularAttributeReference( this );
	}

	@Override
	public Selectable getSelectable() {
		return selectable;
	}

	@Override
	public DomainReferenceImplementor getDomainReference() {
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
	public PropertyPath getPropertyPath() {
		return propertyPath;
	}
}
