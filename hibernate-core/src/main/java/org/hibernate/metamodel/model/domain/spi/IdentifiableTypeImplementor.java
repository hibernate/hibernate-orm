/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.Set;
import java.util.function.Consumer;
import javax.persistence.metamodel.IdentifiableType;

/**
 * Hibernate extension to the JPA {@link IdentifiableType} descriptor
 *
 * @author Steve Ebersole
 */
public interface IdentifiableTypeImplementor<J> extends IdentifiableType<J>, ManagedTypeImplementor<J> {
	boolean hasIdClass();

	SingularAttributeImplementor<? super J,?> locateIdAttribute();

	void collectIdClassAttributes(Set<SingularAttributeImplementor<? super J,?>> attributes);

	void visitIdClassAttributes(Consumer<SingularAttributeImplementor<? super J,?>> attributeConsumer);

	interface InFlightAccess<X> extends ManagedTypeImplementor.InFlightAccess<X> {
		void applyIdAttribute(SingularAttributeImplementor<X, ?> idAttribute);
		void applyIdClassAttributes(Set<SingularAttributeImplementor<? super X, ?>> idClassAttributes);
		void applyVersionAttribute(SingularAttributeImplementor<X, ?> versionAttribute);
	}

	@Override
	InFlightAccess<J> getInFlightAccess();

	@Override
	SimpleTypeImplementor<?> getIdType();

	@Override
	<Y> SingularAttributeImplementor<J, Y> getDeclaredId(Class<Y> type);

	@Override
	<Y> SingularAttributeImplementor<? super J, Y> getId(Class<Y> type);

	SingularAttributeImplementor<? super J,?> locateVersionAttribute();

	@Override
	<Y> SingularAttributeImplementor<? super J, Y> getVersion(Class<Y> type);

	@Override
	<Y> SingularAttributeImplementor<J, Y> getDeclaredVersion(Class<Y> type);

	@Override
	IdentifiableTypeImplementor<? super J> getSuperType();

	@Override
	default IdentifiableTypeImplementor<? super J> getSupertype() {
		return getSuperType();
	}
}
