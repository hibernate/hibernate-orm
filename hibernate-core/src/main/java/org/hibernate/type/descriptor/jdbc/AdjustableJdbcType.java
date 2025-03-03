/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

import org.hibernate.type.descriptor.java.JavaType;

/**
 * Extension contract for JdbcType implementations that understand how to
 * adjust themselves relative to where/how they are used (e.g. accounting
 * for LOB, nationalized, primitive/wrapper, etc).
 *
 * @author Christian Beikov
 */
public interface AdjustableJdbcType extends JdbcType {

	/**
	 * Perform the adjustment
	 */
	JdbcType resolveIndicatedType(JdbcTypeIndicators indicators, JavaType<?> domainJtd);
}
