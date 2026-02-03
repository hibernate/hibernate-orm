/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.annotations.spi;

import java.lang.annotation.Annotation;

/**
 * Commonality for annotations which define custom insert, update and delete SQL
 *
 * @author Steve Ebersole
 */
public interface CustomSqlDetails extends Annotation {
	String sql();

	void sql(String value);

	boolean callable();

	void callable(boolean value);

	String table();

	void table(String value);

	java.lang.Class<? extends org.hibernate.jdbc.Expectation> verify();

	void verify(java.lang.Class<? extends org.hibernate.jdbc.Expectation> value);
}
