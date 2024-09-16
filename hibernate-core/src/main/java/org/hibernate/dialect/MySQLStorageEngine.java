/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

/**
 * This interface defines how various MySQL storage engines behave in regard to Hibernate functionality.
 *
 * @author Vlad Mihalcea
 */
public interface MySQLStorageEngine {

	boolean supportsCascadeDelete();

	String getTableTypeString(String engineKeyword);

	boolean hasSelfReferentialForeignKeyBug();

	boolean dropConstraints();
}
