//$Id$
package org.hibernate.test.annotations.tuplizer;
import org.hibernate.mapping.Component;
import org.hibernate.tuple.Instantiator;
import org.hibernate.tuple.component.PojoComponentTuplizer;

/**
 * @author Emmanuel Bernard
 */
public class DynamicComponentTuplizer extends PojoComponentTuplizer {

	public DynamicComponentTuplizer(Component component) {
		super( component );
	}

	protected Instantiator buildInstantiator(Component component) {
		return new DynamicInstantiator( component.getComponentClassName() );	//To change body of overridden methods use File | Settings | File Templates.
	}
}
