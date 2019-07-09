/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

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
