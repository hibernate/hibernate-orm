//$Id$
package org.hibernate.annotations;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.FIELD;
import javax.persistence.Column;

/**
 * Describe an identifier column for a bag (ie an idbag)
 * EXPERIMENTAL: the structure of this annotation might slightly change (generator() mix strategy and generator
 * 
 * @author Emmanuel Bernard
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface CollectionId {
	/** Collection id column(s) */
	Column[] columns();
	/** id type, type.type() must be set  */
	Type type();
	/** generator name: 'identity' or a defined generator name */
	String generator();
}
