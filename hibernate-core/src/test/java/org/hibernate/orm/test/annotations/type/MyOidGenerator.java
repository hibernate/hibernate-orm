/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.type;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

/**
 * @author Emmanuel Bernard
 */
public class MyOidGenerator implements IdentifierGenerator {

	private int counter;

	public Object generate(SharedSessionContractImplementor session, Object aObject) throws HibernateException {
		counter++;
		return new MyOid( 0, 0, 0, counter );
	}
}
