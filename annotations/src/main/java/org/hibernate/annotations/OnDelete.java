package org.hibernate.annotations;

import static java.lang.annotation.ElementType.*;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;


/**
 * Strategy to use on collections, arrays and on joined subclasses delete
 * OnDelete of secondary tables currently not supported.
 *
 * @author Emmanuel Bernard
 */
@Target({METHOD, FIELD, TYPE})
@Retention(RUNTIME)
public @interface OnDelete {
	OnDeleteAction action();
}
