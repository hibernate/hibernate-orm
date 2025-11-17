/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.interfaces.hbm.propertiesAudited2;

import org.hibernate.envers.Audited;

/**
 * @author Hern�n Chanfreau
 */
public interface SimpleInterface {

	long getId();

	void setId(long id);

	@Audited
	String getData();

	void setData(String data);

	@Audited
	int getNumerito();

	void setNumerito(int num);

}
