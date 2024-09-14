/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.model.domain;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import jakarta.persistence.metamodel.IdentifiableType;
import jakarta.persistence.metamodel.SingularAttribute;

import org.hibernate.query.sqm.SqmPathSource;

/**
 * Extension to the JPA {@link IdentifiableType} contract.
 *
 * @author Steve Ebersole
 */
public interface IdentifiableDomainType<J> extends ManagedDomainType<J>, IdentifiableType<J> {
	SqmPathSource<?> getIdentifierDescriptor();

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
	IdentifiableDomainType<? super J> getSupertype();

	boolean hasIdClass();

	SingularPersistentAttribute<? super J,?> findIdAttribute();

	void visitIdClassAttributes(Consumer<SingularPersistentAttribute<? super J,?>> action);

	SingularPersistentAttribute<? super J, ?> findVersionAttribute();

	List<? extends PersistentAttribute<? super J, ?>> findNaturalIdAttributes();
}
