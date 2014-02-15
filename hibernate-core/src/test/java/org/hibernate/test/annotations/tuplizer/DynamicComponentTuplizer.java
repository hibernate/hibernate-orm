package org.hibernate.test.annotations.tuplizer;

import org.hibernate.metamodel.spi.binding.CompositeAttributeBindingContainer;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tuple.Instantiator;
import org.hibernate.tuple.component.PojoComponentTuplizer;

/**
 * @author Emmanuel Bernard
 */
public class DynamicComponentTuplizer extends PojoComponentTuplizer {

	public DynamicComponentTuplizer(
			ServiceRegistry serviceRegistry,
			CompositeAttributeBindingContainer compositeAttributeBindingContainer,
			boolean isIdentifierMapper) {
		super( serviceRegistry, compositeAttributeBindingContainer, isIdentifierMapper);
	}

	@Override
	protected Instantiator buildInstantiator(
			CompositeAttributeBindingContainer compositeAttributeBindingContainer,
			boolean isIdentifierMapper) {
		return new DynamicInstantiator(
				compositeAttributeBindingContainer.getAttributeContainer().getDescriptor().getName().fullName()
		);
	}
}