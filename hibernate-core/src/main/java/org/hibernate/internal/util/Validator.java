/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util;

import static org.hibernate.internal.log.ValidatorLogger.INSTANCE;

/**
 * Utility class to validate objects and assert preconditions. I18N Exceptions
 * within Hibernate standard.
 *
 * @author Boris Unckel
 *
 * @see <a href=
 *      "https://github.com/wildfly/wildfly-common/blob/master/src/main/java/org/wildfly/common/Assert.java">org.wildfly.common.Assert</a>
 * @see <a href=
 *      "https://github.com/google/guava/blob/master/guava/src/com/google/common/base/Preconditions.java">com.google.base.Preconditions</a>
 * @see <a href=
 *      "https://github.com/apache/commons-lang/blob/master/src/main/java/org/apache/commons/lang3/Validate.java">org.apache.commons.lang3.Validate</a>
 */
public final class Validator {

	/**
	 * Validates a object to be not null.
	 *
	 * @param paramName of the object, will be part of the exception text, cannot be
	 *                  null.
	 * @param object    to validate.
	 * @param <T>       the object type.
	 * @return the object, never null, for chaining.
	 * @throws NullPointerException     if object is null.
	 * @throws IllegalArgumentException if paramName is null.
	 */
	public static <T> T checkNotNullNPE(final String paramName, final T object) throws NullPointerException {
		notNullIAE("paramName", paramName);
		if (object == null) {
			throw INSTANCE.nullParamNPE(paramName);
		}
		return object;
	}

	/**
	 * Validates a object to be not null.
	 *
	 * @param paramName of the object, will be part of the exception text, cannot be
	 *                  null.
	 * @param object    to validate.
	 * @param <T>       the object type.
	 * @return the object, never null, for chaining.
	 * @throws IllegalArgumentException if paramName or object are null.
	 */
	public static <T> T checkNotNullIAE(final String paramName, final T object) throws IllegalArgumentException {
		notNullIAE("paramName", paramName);
		notNullIAE(paramName, object);
		return object;
	}

	private static <T> void notNullIAE(final String paramName, final T object) throws NullPointerException {
		if (object == null) {
			throw INSTANCE.nullParamIAE(paramName);
		}
	}

	private Validator() {
		throw new IllegalStateException("No instance");
	}

}
