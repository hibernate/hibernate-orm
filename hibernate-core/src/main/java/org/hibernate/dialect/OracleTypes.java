/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
