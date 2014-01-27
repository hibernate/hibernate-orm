//$Id$
package org.hibernate.test.annotations.tuplizer;
import org.hibernate.mapping.Component;
import org.hibernate.metamodel.spi.binding.CompositeAttributeBindingContainer;
import org.hibernate.tuple.Instantiator;
import org.hibernate.tuple.component.PojoComponentTuplizer;

/**
 * @author Emmanuel Bernard
 */
public class DynamicComponentTuplizer extends PojoComponentTuplizer {

	public DynamicComponentTuplizer(Component component) {
		super( component );
	}

	public DynamicComponentTuplizer(CompositeAttributeBindingContainer compositeAttributeBindingContainer, boolean isIdentifierMapper) {
		super( compositeAttributeBindingContainer, isIdentifierMapper);
	}

	protected Instantiator buildInstantiator(Component component) {
		return new DynamicInstantiator( component.getComponentClassName() );
	}

	protected Instantiator buildInstantiator(
			CompositeAttributeBindingContainer compositeAttributeBindingContainer,
			boolean isIdentifierMapper) {
		return new DynamicInstantiator( compositeAttributeBindingContainer.getClassReference().getName() );
	}
}