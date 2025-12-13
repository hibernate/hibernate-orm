/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.usertype;

import java.util.Properties;

/**
 * Support for parameterizable types. A {@link UserType} or {@link UserCollectionType}
 * may be made parameterizable by implementing this interface. Arguments to parameters
 * may be specified via {@link org.hibernate.annotations.Type#parameters}. In XML,
 * parameters for a type may be set by using a nested type element for the property
 * element in the mapping file, or by defining a typedef.
 *
 * @deprecated This interface provides little type safety, and results in verbosity
 * at the use site. A much better approach is now provided by the possibility of
 * declaring a {@link UserType} via an intermediate type annotation.
 * <p>For example, we could define a type annotation for the type {@code TimePeriod}:
 * <pre>
 * &#64;Type(PeriodType.class) // the type itself
 * &#64;Target({METHOD, FIELD})
 * &#64;Retention(RUNTIME)
 * // the type annotation:
 * public @interface TimePeriod {
 *     // member specifying custom configuration
 *     // affecting the behavior of the UserType:
 *     boolean days() default false;
 * }
 * </pre>
 * <p>Then the {@code UserType} implementation receives an instance of the type annotation
 * in its constructor:
 * <pre>
 * static class PeriodType implements UserType&lt;Period&gt; {
 *    private final boolean days;
 *
 *    // constructor configures the UserType from
 *    // information in the annotation instance:
 *    PeriodType(TimePeriod timePeriod) {
 *        days = timePeriod.days();
 *    }
 *    // implementation of UserType operations:
 *    ...
 * }
 * </pre>
 * <p>Therefore, use of {@code ParameterizedType} is no longer encouraged.
 *
 * @see org.hibernate.annotations.Type#parameters
 *
 * @author Michael Gloegl
 */
@Deprecated(since = "7.3")
public interface ParameterizedType {
    /**
     * Gets called by Hibernate to pass the configured type parameters to
     * the implementation.
     */
    void setParameterValues(Properties parameters);
}
