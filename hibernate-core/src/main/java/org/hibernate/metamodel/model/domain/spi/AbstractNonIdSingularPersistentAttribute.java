/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.Map;

import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.engine.internal.ForeignKeys;
import org.hibernate.engine.internal.NonNullableTransientDependencies;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
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

	protected MutabilityPlan<J> mutabilityPlan;

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
	public int getStateArrayPosition() {
		return stateArrayPosition;
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

	@Override
	public void collectNonNullableTransientEntities(
			Object value,
			ForeignKeys.Nullifier nullifier,
			NonNullableTransientDependencies nonNullableTransientEntities,
			SharedSessionContractImplementor session) {
		// most implementations have nothing to do here
	}

	@Override
	public Object replace(J originalValue, J targetValue, Object owner, Map copyCache, SessionImplementor session) {
		if ( LazyPropertyInitializer.UNFETCHED_PROPERTY == originalValue ) {
			return targetValue;
		}
		else {
			if ( !mutabilityPlan.isMutable() ||
					( targetValue != LazyPropertyInitializer.UNFETCHED_PROPERTY &&
							getJavaTypeDescriptor().areEqual( originalValue, targetValue ) ) ) {
				return originalValue;
			}
			else {
				return mutabilityPlan.deepCopy( originalValue );
			}
		}
	}

	@Override
	public boolean isDirty(Object originalValue, Object currentValue, SharedSessionContractImplementor session) {
		return !getJavaTypeDescriptor().areEqual( (J) originalValue, (J) currentValue );
	}
}
