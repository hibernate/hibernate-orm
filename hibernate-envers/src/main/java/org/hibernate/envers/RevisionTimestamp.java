/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a property which will hold the timestamp of the revision in a revision entity, see
 * {@link RevisionListener}. The value of this property will be automatically set by Envers.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Sanne Grinovero
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface RevisionTimestamp {
}
