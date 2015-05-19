/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.collection.norevision;

import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.envers.test.BaseEnversFunctionalTestCase;
import org.hibernate.envers.test.Priority;

import org.junit.Test;

public abstract class AbstractCollectionChangeTest extends BaseEnversFunctionalTestCase {
	protected Integer personId;

	@Override
	protected void addSettings(Map settings) {
		super.addSettings( settings );

		settings.put( EnversSettings.REVISION_ON_COLLECTION_CHANGE, getCollectionChangeValue() );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {Person.class, Name.class};
	}

	protected abstract String getCollectionChangeValue();

	protected abstract List<Integer> getExpectedPersonRevisions();

	@Test
	@Priority(10)
	public void initData() {
		Session session = openSession();

		// Rev 1
		session.getTransaction().begin();
		Person p = new Person();
		Name n = new Name();
		n.setName( "name1" );
		p.getNames().add( n );
		session.saveOrUpdate( p );
		session.getTransaction().commit();

		// Rev 2
		session.getTransaction().begin();
		n.setName( "Changed name" );
		session.saveOrUpdate( p );
		session.getTransaction().commit();

		// Rev 3
		session.getTransaction().begin();
		Name n2 = new Name();
		n2.setName( "name2" );
		p.getNames().add( n2 );
		session.getTransaction().commit();

		personId = p.getId();
	}

	@Test
	public void testPersonRevisionCount() {
		assert getAuditReader().getRevisions( Person.class, personId ).equals( getExpectedPersonRevisions() );
	}
}
