/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.spi;

import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.type.CompositeType;

/**
 * @author Steve Ebersole
 */
public interface CompositeTypeImplementor extends CompositeType {
	void injectMappingModelPart(EmbeddableValuedModelPart part, MappingModelCreationProcess process);
	EmbeddableValuedModelPart getMappingModelPart();
}
