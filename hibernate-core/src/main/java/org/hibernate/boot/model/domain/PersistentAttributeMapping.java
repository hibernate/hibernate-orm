/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.domain;

import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentAttributeDescriptor;
import org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute;
import org.hibernate.tuple.ValueGeneration;

/**
 * @author Steve Ebersole
 */
public interface PersistentAttributeMapping extends MetaAttributable, ValueMappingContainer {
	String getName();

	default boolean isVirtual() {
		return false;
	}

	default boolean isBackRef() {
		return false;
	}

	ValueMapping getValueMapping();

	String getPropertyAccessorName();

	boolean isNaturalIdentifier();
	boolean isLob();

	String getCascade();

	boolean isSelectable();

	EntityMapping getEntity();

	/**
	 * @todo (6.0) : different from `#isNullable`
	 * 		1) how?
	 * 		2) expose `#isNullable` here too?
	 */
	boolean isOptional();
	boolean isUpdateable();
	boolean isInsertable();
	boolean isIncludedInDirtyChecking();
	boolean isIncludedInOptimisticLocking();

	boolean isLazy();
	String getLazyGroup();

	ValueGeneration getValueGenerationStrategy();

	String getMappedBy();

	// todo (6.0) : `#makeRuntimeAttribute` should not be exposed on API

	// todo (6.0) relatedly (^^), we ought to pass the PersistentAttributeMapping into the runtime ctor as we build it
	// 		- the alternative is to pass many values to its ctor (which we have now)

	<O,T> PersistentAttributeDescriptor<O,T> makeRuntimeAttribute(
			ManagedTypeDescriptor<O> runtimeContainer,
			ManagedTypeMapping bootContainer,
			SingularPersistentAttribute.Disposition singularAttributeDisposition,
			RuntimeModelCreationContext context);

}
