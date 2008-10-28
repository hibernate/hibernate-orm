//$Id$
package org.hibernate.annotations;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.FIELD;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Defines @Any and @ManyToAny set of metadata.
 * Can be defined at the entity level or the package level
 *
 * @author Emmanuel Bernard
 */
@java.lang.annotation.Target( { PACKAGE, TYPE } )
@Retention( RUNTIME )
public @interface AnyMetaDefs {
	AnyMetaDef[] value();
}
