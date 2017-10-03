/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.type.descriptor.java.MutabilityPlan;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractSingularPersistentAttribute<O,J>
		extends AbstractPersistentAttribute<O,J>
		implements SingularPersistentAttribute<O,J> {
	private final Disposition disposition;

	private final MutabilityPlan<J> mutabilityPlan;

	private final boolean nullable;
	private final boolean insertable;
	private final boolean updatable;
	private final boolean includedInDirtyChecking;

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

		// todo (6.0) : determine mutability plan based on JTD & @Immutable
		//		for now just use the JTD MP
		this.mutabilityPlan = getJavaTypeDescriptor().getMutabilityPlan();

		this.nullable = bootAttribute.isOptional();
		this.insertable = bootAttribute.isInsertable();
		this.updatable = bootAttribute.isUpdateable();
		this.includedInDirtyChecking = bootAttribute.isIncludedInDirtyChecking();
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

	@Override
	public boolean isInsertable() {
		return insertable;
	}

	@Override
	public boolean isUpdatable() {
		return updatable;
	}

	@Override
	public boolean isIncludedInDirtyChecking() {
		return includedInDirtyChecking;
	}

	@Override
	public MutabilityPlan<J> getMutabilityPlan() {
		return mutabilityPlan;
	}
}
