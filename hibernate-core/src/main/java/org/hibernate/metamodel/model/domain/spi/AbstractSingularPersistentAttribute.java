/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.property.access.spi.PropertyAccess;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSingularPersistentAttribute<O,J>
		extends AbstractPersistentAttribute<O,J>
		implements SingularPersistentAttribute<O,J> {
	private final Disposition disposition;

	public AbstractSingularPersistentAttribute(
			ManagedTypeDescriptor<O> runtimeContainer,
			PersistentAttributeMapping bootAttribute,
			PropertyAccess propertyAccess,
			Disposition disposition) {
		super(
				runtimeContainer,
				bootAttribute,
				propertyAccess
		);

		this.disposition = disposition;
	}

	@Override
	public Disposition getDisposition() {
		return disposition;
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
	public boolean isCollection() {
		return false;
	}

	@Override
	public boolean isAssociation() {
		return getPersistentAttributeType() == PersistentAttributeType.MANY_TO_ONE
				|| getPersistentAttributeType() == PersistentAttributeType.ONE_TO_ONE;
	}

	@Override
	public SimpleTypeDescriptor<J> getAttributeType() {
		return getType();
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
