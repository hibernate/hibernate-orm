/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache.hhh13179;

public abstract class JoinedSubclassPerson {

private Long oid;

public Long getOid() {
	return oid;
}

public void setOid(Long oid) {
	this.oid = oid;
}
}
