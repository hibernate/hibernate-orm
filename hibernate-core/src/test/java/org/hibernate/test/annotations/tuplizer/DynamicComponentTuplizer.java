package org.hibernate.test.annotations.tuplizer;

import org.hibernate.metamodel.spi.binding.EmbeddableBinding;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tuple.Instantiator;
import org.hibernate.tuple.component.PojoComponentTuplizer;

/**
 * @author Emmanuel Bernard
 */
public class DynamicComponentTuplizer extends PojoComponentTuplizer {

	public DynamicComponentTuplizer(
			ServiceRegistry serviceRegistry,
			EmbeddableBinding embeddableBinding,
			boolean isIdentifierMapper) {
		super( serviceRegistry, embeddableBinding, isIdentifierMapper);
	}

	@Override
	protected Instantiator buildInstantiator(
			EmbeddableBinding embeddableBinding,
			boolean isIdentifierMapper) {
		return new DynamicInstantiator(
				embeddableBinding.getAttributeContainer().getDescriptor().getName().toString()
		);
	}
}