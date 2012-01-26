/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.inheritancediscriminator;

import org.hibernate.Session;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQLDialect;

import org.hibernate.testing.junit.functional.DatabaseSpecificFunctionalTestCase;

/**
 * @author Pawel Stawicki
 */
public class PersistChildEntitiesWithDiscriminatorTest extends DatabaseSpecificFunctionalTestCase  {
	public PersistChildEntitiesWithDiscriminatorTest(String string) {
		super( string );
	}

	@Override
	public String[] getMappings() {
		return new String[] { "inheritancediscriminator/Mappings.hbm.xml" };
	}

	@Override
	public boolean appliesTo(Dialect dialect) {
		return PostgreSQLDialect.class.isInstance( dialect );
	}

	public void testIt() {
		Session session = openSession();
		session.beginTransaction();
		InheritingEntity child = new InheritingEntity();
		child.setSomeValue("blabla");
		session.save(child);
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		session.delete( child );
		session.getTransaction().commit();
		session.close();
	}

}
