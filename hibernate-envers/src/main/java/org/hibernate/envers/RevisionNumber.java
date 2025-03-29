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
 * Marks a property which will hold the number of the revision in a revision entity, see
 * {@link RevisionListener}. Values of this property should form a strictly-increasing sequence
 * of numbers. The value of this property won't be set by Envers. In most cases, this should be
 * an auto-generated database-assigned primary id.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Sanne Grinovero
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface RevisionNumber {
}
