/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.type.descriptor.java.MutabilityPlan;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractNonIdSingularPersistentAttribute<O,J>
		extends AbstractSingularPersistentAttribute<O,J>
		implements NonIdPersistentAttribute<O,J> {

	private final boolean nullable;
	private final boolean insertable;
	private final boolean updatable;
	private final boolean includedInDirtyChecking;

	private int stateArrayPosition;
	private MutabilityPlan<J> mutabilityPlan;


	public AbstractNonIdSingularPersistentAttribute(
			ManagedTypeDescriptor<O> runtimeContainer,
			PersistentAttributeMapping bootAttribute,
			PropertyAccess propertyAccess,
			Disposition disposition) {
		super( runtimeContainer, bootAttribute, propertyAccess, disposition );

		this.nullable = bootAttribute.isOptional();
		this.insertable = bootAttribute.isInsertable();
		this.updatable = bootAttribute.isUpdateable();
		this.includedInDirtyChecking = bootAttribute.isIncludedInDirtyChecking();
	}

	@Override
	public void setStateArrayPosition(int position) {
		this.stateArrayPosition = position;
	}

	protected void instantiationComplete(
			PersistentAttributeMapping bootModelAttribute,
			RuntimeModelCreationContext context) {
		// todo (6.0) : determine mutability plan based on JTD & @Immutable
		//		for now just use the JTD MP
		this.mutabilityPlan = getJavaTypeDescriptor().getMutabilityPlan();
	}

	@Override
	public int getStateArrayPosition() {
		return stateArrayPosition;
	}

	@Override
	public Class<J> getJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}

	@Override
	public boolean isNullable() {
		return nullable;
	}

	@Override
	public boolean isOptional() {
		return isNullable();
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
