/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

/**
 * The Oracle specific JDBC type code.
 */
public class OracleTypes {
	public static final int CURSOR = -10;
	public static final int JSON = 2016;

	public static final int VECTOR = -105;
	public static final int VECTOR_INT8 = -106;
	public static final int VECTOR_FLOAT32 = -107;
	public static final int VECTOR_FLOAT64 = -108;
}
