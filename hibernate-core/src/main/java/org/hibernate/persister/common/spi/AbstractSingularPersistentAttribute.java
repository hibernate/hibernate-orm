/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.common.spi;

import org.hibernate.persister.queryable.spi.ExpressableType;
import org.hibernate.property.access.spi.PropertyAccess;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSingularPersistentAttribute<O,J>
		extends AbstractPersistentAttribute<O,J>
		implements SingularPersistentAttribute<O,J> {
	private final boolean nullable;
	private final Disposition disposition;

	public AbstractSingularPersistentAttribute(
			ManagedTypeImplementor<O> attributeContainer,
			String name,
			PropertyAccess propertyAccess,
			ExpressableType<J> expressableType,
			Disposition disposition,
			boolean nullable) {
		super( attributeContainer, name, expressableType.getJavaTypeDescriptor(), propertyAccess );
		this.disposition = disposition;
		this.nullable = nullable;
	}



	@Override
	public Disposition getDisposition() {
		return disposition;
	}

	@Override
	public boolean isNullable() {
		return nullable;
	}

	@Override
	public boolean isId() {
		return getDisposition() == Disposition.ID;
	}

	@Override
	public boolean isVersion() {
		return getDisposition() == Disposition.VERSION;
	}

	@Override
	public boolean isOptional() {
		return nullable;
	}

	@Override
	public boolean isCollection() {
		return false;
	}

	@Override
	public boolean isAssociation() {
		return false;
	}

	@Override
	public javax.persistence.metamodel.Type<J> getType() {
		return this;
	}

	@Override
	public BindableType getBindableType() {
		return BindableType.SINGULAR_ATTRIBUTE;
	}

	@Override
	public Class<J> getBindableJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}
}
