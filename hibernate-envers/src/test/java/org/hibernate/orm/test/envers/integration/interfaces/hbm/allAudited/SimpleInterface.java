/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.interfaces.hbm.allAudited;

import org.hibernate.envers.Audited;

/**
 * @author Hern�n Chanfreau
 */
@Audited
public interface SimpleInterface {

	long getId();

	void setId(long id);

	String getData();

	void setData(String data);

}
