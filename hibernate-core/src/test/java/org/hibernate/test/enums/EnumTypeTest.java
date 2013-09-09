/* 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.test.enums;

import static org.junit.Assert.assertEquals;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Brett Meyer
 */
public class EnumTypeTest extends BaseCoreFunctionalTestCase {

	protected String[] getMappings() {
		return new String[] { "enums/Person.hbm.xml" };
	}
	
	@Test
	@TestForIssue(jiraKey = "HHH-8153")
	public void hbmEnumTypeTest() {
		Session s = openSession();
		s.getTransaction().begin();
		s.persist( Person.person( Gender.MALE, HairColor.BROWN ) );
		s.persist( Person.person( Gender.MALE, HairColor.BLACK ) );
		s.persist( Person.person( Gender.FEMALE, HairColor.BROWN ) );
		s.persist( Person.person( Gender.FEMALE, HairColor.BLACK ) );
		s.getTransaction().commit();
		s.clear();
		
		s.getTransaction().begin();
		assertEquals(s.createCriteria( Person.class )
				.add( Restrictions.eq( "gender", Gender.MALE ) )
				.list().size(), 2);
		assertEquals(s.createCriteria( Person.class )
				.add( Restrictions.eq( "gender", Gender.MALE ) )
				.add( Restrictions.eq( "hairColor", HairColor.BROWN ) )
				.list().size(), 1);
		assertEquals(s.createCriteria( Person.class )
				.add( Restrictions.eq( "gender", Gender.FEMALE ) )
				.list().size(), 2);
		assertEquals(s.createCriteria( Person.class )
				.add( Restrictions.eq( "gender", Gender.FEMALE ) )
				.add( Restrictions.eq( "hairColor", HairColor.BROWN ) )
				.list().size(), 1);
		s.getTransaction().commit();
		s.close();
	}
}
