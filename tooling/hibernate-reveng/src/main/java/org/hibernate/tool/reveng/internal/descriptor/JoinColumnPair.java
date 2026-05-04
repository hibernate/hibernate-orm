/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.descriptor;

/**
 * A pair of column names representing a foreign key column
 * and its referenced column in the target table.
 *
 * @author Koen Aers
 */
public record JoinColumnPair(String fkColumnName, String referencedColumnName) {}
