package org.hibernate.envers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a property which will hold the collection of entity names modified during each revision.
 * This annotation expects field of Set&lt;String&gt; type.
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface ModifiedEntityNames {
}
