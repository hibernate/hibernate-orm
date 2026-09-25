package org.hibernate.type.spi;

import org.hibernate.metamodel.mapping.EmbeddableValuedModelPart;
import org.hibernate.metamodel.mapping.internal.MappingModelCreationProcess;
import org.hibernate.type.CompositeType;

/**
 * @author Steve Ebersole
 */
@org.hibernate.Internal
public interface CompositeTypeImplementor extends CompositeType {
	void injectMappingModelPart(EmbeddableValuedModelPart part, MappingModelCreationProcess process);
	EmbeddableValuedModelPart getMappingModelPart();
}
