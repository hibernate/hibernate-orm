/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.expression.domain;

import java.util.List;

import org.hibernate.loader.PropertyPath;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.sql.ast.from.ColumnBinding;
import org.hibernate.sql.ast.select.Selectable;
import org.hibernate.sql.ast.select.SelectableEntityTypeImpl;
import org.hibernate.sql.exec.spi.SqlAstSelectInterpreter;
import org.hibernate.type.spi.Type;

/**
 * @author Andrea Boriero
 * @author Steve Ebersole
 */
public class EntityReferenceExpression implements DomainReferenceExpression {
	private final ColumnBindingSource columnBindingSource;
	private final EntityPersister entityPersister;
	private final PropertyPath propertyPath;

	private final SelectableEntityTypeImpl selectable;

	public EntityReferenceExpression(
			ColumnBindingSource columnBindingSource,
			EntityPersister entityPersister,
			PropertyPath propertyPath,
			boolean isShallow) {
		this.columnBindingSource = columnBindingSource;
		this.entityPersister = entityPersister;
		this.propertyPath = propertyPath;

		this.selectable = new SelectableEntityTypeImpl(
				this,
				propertyPath,
				columnBindingSource,
				entityPersister,
				isShallow
		);
	}

	public EntityPersister getEntityPersister() {
		return entityPersister;
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
	public void accept(SqlAstSelectInterpreter walker) {
		walker.visitEntityExpression( this );
	}

	@Override
	public PropertyPath getPropertyPath() {
		return propertyPath;
	}

	@Override
	public List<ColumnBinding> getColumnBindings() {
		return selectable.getColumnBinding();
	}

	@Override
	public EntityPersister getDomainReference() {
		return getEntityPersister();
	}
}
