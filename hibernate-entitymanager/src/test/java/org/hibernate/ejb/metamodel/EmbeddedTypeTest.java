/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009, Red Hat Inc. or third-party contributors as
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
package org.hibernate.ejb.metamodel;

import javax.persistence.EntityManager;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.ejb.test.TestCase;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class EmbeddedTypeTest extends TestCase {
	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Product.class, ShelfLife.class
		};
	}

	public void testSingularAttributeAccessByName() {
		// HHH-4702
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		SingularAttribute soldDate_ = em.getMetamodel().embeddable( ShelfLife.class )
				.getSingularAttribute( "soldDate" );
		assertEquals( java.sql.Date.class, soldDate_.getBindableJavaType());
		assertEquals( java.sql.Date.class, soldDate_.getType().getJavaType() );
		assertEquals( java.sql.Date.class, soldDate_.getJavaType() );

		em.getTransaction().commit();
		em.close();
	}
}
