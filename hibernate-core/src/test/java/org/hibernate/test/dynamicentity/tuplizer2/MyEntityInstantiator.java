/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.test.dynamicentity.tuplizer2;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.test.dynamicentity.Address;
import org.hibernate.test.dynamicentity.Company;
import org.hibernate.test.dynamicentity.Customer;
import org.hibernate.test.dynamicentity.Person;
import org.hibernate.test.dynamicentity.ProxyHelper;
import org.hibernate.tuple.Instantiator;

/**
 * @author Steve Ebersole
 */
public class MyEntityInstantiator implements Instantiator {
	private final String entityName;

	public MyEntityInstantiator(String entityName) {
		this.entityName = entityName;
	}

	public Object instantiate(Serializable id) {
		if ( Person.class.getName().equals( entityName ) ) {
			return ProxyHelper.newPersonProxy( id );
		}
		if ( Customer.class.getName().equals( entityName ) ) {
			return ProxyHelper.newCustomerProxy( id );
		}
		else if ( Company.class.getName().equals( entityName ) ) {
			return ProxyHelper.newCompanyProxy( id );
		}
		else if ( Address.class.getName().equals( entityName ) ) {
			return ProxyHelper.newAddressProxy( id );
		}
		else {
			throw new IllegalArgumentException( "unknown entity for instantiation [" + entityName + "]" );
		}
	}

	public Object instantiate() {
		return instantiate( null );
	}

	public boolean isInstance(Object object) {
		try {
			return ReflectHelper.classForName( entityName ).isInstance( object );
		}
		catch( Throwable t ) {
			throw new HibernateException( "could not get handle to entity-name as interface : " + t );
		}
	}
}
