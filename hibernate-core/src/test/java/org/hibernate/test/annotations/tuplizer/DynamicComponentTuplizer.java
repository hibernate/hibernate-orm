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
	private final Instantiator instantiator;
	public DynamicComponentTuplizer(Component component) {
		super( component );
		this.instantiator  = new DynamicInstantiator(component.getComponentClassName() );
	}

	public DynamicComponentTuplizer(CompositeAttributeBindingContainer component, boolean isIdentifierMapper) {
		super( component, isIdentifierMapper );
		this.instantiator  = new DynamicInstantiator(component.getClassReference().getName() );
	}

	protected Instantiator buildInstantiator(Component component) {
		return new DynamicInstantiator( component.getComponentClassName() );
	}

	@Override
	protected Instantiator getInstantiator() {
		return  instantiator;
	}
}
