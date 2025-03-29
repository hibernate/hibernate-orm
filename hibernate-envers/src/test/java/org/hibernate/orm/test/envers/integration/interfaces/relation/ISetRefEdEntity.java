/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.interfaces.relation;


/**
 * @author Adam Warski (adam at warski dot org)
 */
public interface ISetRefEdEntity {
	Integer getId();

	void setId(Integer id);

	String getData();

	void setData(String data);
}
