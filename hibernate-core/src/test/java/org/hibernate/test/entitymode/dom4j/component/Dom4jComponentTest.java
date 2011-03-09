/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.entitymode.dom4j.component;

import org.hibernate.EntityMode;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.junit.Test;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Steve Ebersole
 */
public class Dom4jComponentTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "entitymode/dom4j/component/Mapping.hbm.xml" };
	}

	@Override
	public void configure(Configuration cfg) {
		cfg.setProperty( Environment.DEFAULT_ENTITY_MODE, EntityMode.DOM4J.toString() );
	}

	@Test
	@FailureExpected( jiraKey = "HHH-1907" )
	public void testSetAccessorsFailureExpected() {
		// An example of part of the issue discussed in HHH-1907
		Session session = openSession();
		session.beginTransaction();
		session.getSession( EntityMode.POJO ).save( new ComponentOwner( new Component( new ComponentReference() ) ) );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		session.createQuery( "from ComponentOwner" ).list();
		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		session.createQuery( "delete ComponentOwner" ).executeUpdate();
		session.createQuery( "delete ComponentReference" ).executeUpdate();
		session.getTransaction().commit();
		session.close();
	}
}
