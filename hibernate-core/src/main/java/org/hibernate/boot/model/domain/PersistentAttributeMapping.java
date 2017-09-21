/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.domain;

import org.hibernate.mapping.MetaAttributable;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.ManagedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.SingularPersistentAttribute;
import org.hibernate.tuple.ValueGeneration;

/**
 * @author Steve Ebersole
 */
public interface PersistentAttributeMapping extends ValueMappingContainer, MetaAttributable {
	String getName();

	default boolean isVirtual() {
		return false;
	}

	default boolean isBackRef() {
		return false;
	}

	String getPropertyAccessorName();

	boolean isNaturalIdentifier();
	boolean isLob();

	String getCascade();
	boolean isOptional();
	boolean isUpdateable();
	boolean isInsertable();
	boolean isSelectable();
	boolean isIncludedInOptimisticLocking();

	boolean isLazy();
	String getLazyGroup();

	ValueGeneration getValueGenerationStrategy();

	<O,T> PersistentAttribute<O,T> makeRuntimeAttribute(
			ManagedTypeDescriptor<O> runtimeContainer,
			ManagedTypeMapping bootContainer,
			SingularPersistentAttribute.Disposition singularAttributeDisposition,
			RuntimeModelCreationContext context);
}
