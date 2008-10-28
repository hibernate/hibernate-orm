//$Id$
package org.hibernate.annotations;

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * ForceDiscriminator flag
 * To be placed at the root entity near @DiscriminatorColumn or @DiscriminatorFormula
 *
 * @author Serg Prasolov
 */
@Target({ElementType.TYPE}) @Retention(RetentionPolicy.RUNTIME)
public @interface ForceDiscriminator {}
