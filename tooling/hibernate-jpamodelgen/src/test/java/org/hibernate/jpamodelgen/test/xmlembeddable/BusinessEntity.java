/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.xmlembeddable;

import java.io.Serializable;

import org.hibernate.jpamodelgen.test.xmlembeddable.foo.BusinessId;

/**
 * @author Hardy Ferentschik
 */
public abstract class BusinessEntity<T extends Serializable> implements Serializable {
	private Long id;

	private BusinessId<T> businessId;
}



