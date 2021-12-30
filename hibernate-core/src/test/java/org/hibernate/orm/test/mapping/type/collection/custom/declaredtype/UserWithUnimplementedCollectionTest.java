/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
 */
package org.hibernate.orm.test.mapping.type.collection.custom.declaredtype;

import org.hibernate.AnnotationException;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Max Rydahl Andersen
 * @author David Weinberg Negative test when specifying a type that can't be mapped as a collection
 */
public class UserWithUnimplementedCollectionTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{ UserWithUnimplementedCollection.class, Email.class };
	}

	@Override
	protected String getCacheConcurrencyStrategy() {
		return "nonstrict-read-write";
	}

	@Override
	protected void buildSessionFactory() {
		try {
			super.buildSessionFactory();
			fail( "Expected exception" );
		}
		catch (Exception e) {
			assertThat( e ).isInstanceOf( AnnotationException.class );
			assertThat( e ).hasMessageStartingWith( "Illegal attempt to map a non collection as a @OneToMany, @ManyToMany or @CollectionOfElements:" );
			assertThat( e ).hasMessageEndingWith( ".emailAddresses" );
		}
	}

	@Test
	public void testSessionFactoryFailsToBeCreated() {

	}

}
