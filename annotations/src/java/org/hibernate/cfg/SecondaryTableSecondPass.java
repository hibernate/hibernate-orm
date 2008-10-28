//$Id$
package org.hibernate.cfg;

import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.annotations.common.reflection.XAnnotatedElement;
import org.hibernate.cfg.annotations.EntityBinder;

/**
 * @author Emmanuel Bernard
 */
public class SecondaryTableSecondPass implements SecondPass {
	private EntityBinder entityBinder;
	private PropertyHolder propertyHolder;
	private XAnnotatedElement annotatedClass;

	public SecondaryTableSecondPass(EntityBinder entityBinder, PropertyHolder propertyHolder, XAnnotatedElement annotatedClass) {
		this.entityBinder = entityBinder;
		this.propertyHolder = propertyHolder;
		this.annotatedClass = annotatedClass;
	}

	public void doSecondPass(Map persistentClasses) throws MappingException {
		entityBinder.finalSecondaryTableBinding( propertyHolder );
		
	}
}
