/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import jakarta.persistence.metamodel.IdentifiableType;
import jakarta.persistence.metamodel.SingularAttribute;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Extension to the JPA {@link IdentifiableType} contract.
 *
 * @author Steve Ebersole
 */
public interface IdentifiableDomainType<J>
		extends ManagedDomainType<J>, IdentifiableType<J> {

	@Nullable PathSource<?> getIdentifierDescriptor();

	@Override
	<Y> SingularPersistentAttribute<? super J, Y> getId(Class<Y> type);

	@Override
	<Y> SingularPersistentAttribute<J, Y> getDeclaredId(Class<Y> type);

	@Override
	<Y> SingularPersistentAttribute<? super J, Y> getVersion(Class<Y> type);

	@Override
	<Y> SingularPersistentAttribute<J, Y> getDeclaredVersion(Class<Y> type);

	@Override
	Set<SingularAttribute<? super J, ?>> getIdClassAttributes();

	@Override
	SimpleDomainType<?> getIdType();

	@Override
	@Nullable IdentifiableDomainType<? super J> getSupertype();

	boolean hasIdClass();

	@Nullable SingularPersistentAttribute<? super J,?> findIdAttribute();

	void visitIdClassAttributes(Consumer<SingularPersistentAttribute<? super J,?>> action);

	@Nullable SingularPersistentAttribute<? super J, ?> findVersionAttribute();

	@Nullable List<? extends PersistentAttribute<? super J, ?>> findNaturalIdAttributes();
}
