/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies that SQL {@code update} statements for the annotated entity
 * are generated dynamically, and only include columns which are actually
 * being updated.
 * <p>
 * This might result in improved performance if it is common to change
 * only some of the attributes of the entity. However, there is a cost
 * associated with generating the SQL at runtime.
 * <p>
 *
 * @author Steve Ebersole
 */
@Target( TYPE )
@Retention( RUNTIME )
public @interface DynamicUpdate {
}
