//$Id$
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

/**
 * Define a set of tuplizer for an entity or a component
 * @author Emmanuel Bernard
 */
@java.lang.annotation.Target( {ElementType.TYPE, ElementType.FIELD, ElementType.METHOD} )
@Retention( RetentionPolicy.RUNTIME )
public @interface Tuplizers {
	Tuplizer[] value();
}
