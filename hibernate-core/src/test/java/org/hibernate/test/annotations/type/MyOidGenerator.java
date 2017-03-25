/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.type;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

/**
 * @author Emmanuel Bernard
 */
public class MyOidGenerator implements IdentifierGenerator {

	private int counter;

	public Serializable generate(SharedSessionContractImplementor session, Object aObject) throws HibernateException {
		counter++;
		return new MyOid( 0, 0, 0, counter );
	}
}

