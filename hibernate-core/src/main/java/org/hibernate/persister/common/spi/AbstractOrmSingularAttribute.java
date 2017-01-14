/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.common.spi;

import org.hibernate.orm.type.spi.Type;
import org.hibernate.property.access.spi.PropertyAccess;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractOrmSingularAttribute<O extends Type>
		extends AbstractAttribute
		implements SingularAttribute {
	private final O ormType;
	private final Disposition disposition;
	private final boolean nullable;

	public AbstractOrmSingularAttribute(
			ManagedTypeImplementor attributeContainer,
			String name,
			PropertyAccess propertyAccess,
			O ormType,
			Disposition disposition,
			boolean nullable) {
		super( attributeContainer, name, propertyAccess );
		this.ormType = ormType;
		this.disposition = disposition;
		this.nullable = nullable;
	}

	@Override
	public O getOrmType() {
		return ormType;
	}

	@Override
	public boolean isNullable() {
		return nullable;
	}

	@Override
	public boolean isCollection() {
		return false;
	}

	@Override
	public Disposition getDisposition() {
		return disposition;
	}

	@Override
	public boolean isId() {
		return disposition == Disposition.ID;
	}

	@Override
	public boolean isVersion() {
		return disposition == Disposition.VERSION;
	}

	@Override
	public boolean isOptional() {
		return isNullable();
	}

	@Override
	public BindableType getBindableType() {
		return BindableType.SINGULAR_ATTRIBUTE;
	}

	@Override
	public Class getBindableJavaType() {
		return ormType.getJavaType();
	}

	@Override
	public String getTypeName() {
		return ormType.getTypeName();
	}
}
