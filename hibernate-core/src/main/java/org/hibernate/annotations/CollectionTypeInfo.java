/**
 * @(#)CollectionType
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specify a CollectionType via annotations so that a developer can
 * specify a UserCollectionType as well as the standard collection types.
 *
 * @author dwsjoquist (Douglas W Sjoquist)
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface CollectionTypeInfo {
    /**
     * fully qualified class name of type,
     * used like the "collection-type" argument in xml bindings is used.
     */
    String name();
}
