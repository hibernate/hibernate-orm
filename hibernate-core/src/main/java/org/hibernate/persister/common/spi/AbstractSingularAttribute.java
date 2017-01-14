/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.common.spi;

import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.type.spi.Type;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSingularAttribute<O,J, T extends Type<J>>
		extends AbstractAttribute<O,J>
		implements SingularAttribute<O,J> {
	private final T ormType;
	private final boolean nullable;
	private final Disposition disposition;

	public AbstractSingularAttribute(
			ManagedTypeImplementor attributeContainer,
			String name,
			PropertyAccess propertyAccess,
			T ormType,
			Disposition disposition,
			boolean nullable) {
		super( attributeContainer, name, propertyAccess );
		this.ormType = ormType;
		this.disposition = disposition;
		this.nullable = nullable;
	}

	@Override
	public T getOrmType() {
		return ormType;
	}

	@Override
	public String getTypeName() {
		return getOrmType().getName();
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
	public javax.persistence.metamodel.Type getType() {
		return this;
	}

	@Override
	public BindableType getBindableType() {
		return BindableType.SINGULAR_ATTRIBUTE;
	}

	@Override
	public Class getBindableJavaType() {
		return getOrmType().getJavaType();
	}
}
