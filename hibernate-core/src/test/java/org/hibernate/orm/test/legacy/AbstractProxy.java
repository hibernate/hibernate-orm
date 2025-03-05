/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.legacy;

public interface AbstractProxy extends FooProxy {
	void setAbstracts(java.util.Set arg0);
	java.util.Set getAbstracts();
	void setTime(java.sql.Time arg0);
	java.sql.Time getTime();
}
