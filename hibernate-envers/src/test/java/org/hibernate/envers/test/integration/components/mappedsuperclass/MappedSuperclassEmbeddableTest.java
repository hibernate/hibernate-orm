/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.envers.test.integration.components.mappedsuperclass;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.testing.TestForIssue;

/**
 * @author Jakob Braeuchi.
 */
@TestForIssue(jiraKey = "HHH-8908")
public class MappedSuperclassEmbeddableTest extends BaseEnversJPAFunctionalTestCase {
	private long id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { SimplePerson.class, AbstractCode.class, Code.class, TestCode.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		SimplePerson person = new SimplePerson();
		person.setName( "Person 1" );
		person.setTestCode( new TestCode( 84 ) );
		person.setGenericCode( new Code( 42, "TestCodeart" ) );

		EntityTransaction tx = em.getTransaction();
		tx.begin();
		em.persist( person );
		tx.commit();
		em.close();

		id = person.getId();
	}

	@Test
	public void testEmbeddableThatExtendsMappedSuperclass() {

		// Reload and Compare Revision
		EntityManager em = getEntityManager();
		SimplePerson personLoaded = em.find( SimplePerson.class, id );

		AuditReader reader = AuditReaderFactory.get( em );

		List<Number> revs = reader.getRevisions( SimplePerson.class, id );
		Assert.assertEquals( 1, revs.size() );

		SimplePerson personRev1 = reader.find( SimplePerson.class, id, revs.get( 0 ) );

		Assert.assertEquals( personLoaded.getName(), personRev1.getName() );

		// Generic Code is read from AUD Table
		Assert.assertEquals( personLoaded.getGenericCode(), personRev1.getGenericCode() );

		// Test Code is read from AUD Table
		Assert.assertEquals( personLoaded.getTestCode(), personRev1.getTestCode() );
	}
}
