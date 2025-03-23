/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.collection.norevision;

import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.envers.configuration.EnversSettings;
import org.hibernate.orm.test.envers.BaseEnversFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;

import org.junit.Test;

import org.hibernate.testing.orm.junit.JiraKey;

public abstract class AbstractCollectionChangeTest extends BaseEnversFunctionalTestCase {
	protected Integer personId;
	protected Integer parentId;
	protected Integer houseId;

	@Override
	protected void addSettings(Map settings) {
		super.addSettings( settings );

		settings.put( EnversSettings.REVISION_ON_COLLECTION_CHANGE, getCollectionChangeValue() );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {Person.class, Name.class, Parent.class, Child.class, House.class};
	}

	protected abstract String getCollectionChangeValue();

	protected abstract List<Integer> getExpectedPersonRevisions();

	protected abstract List<Integer> getExpectedParentRevisions();

	protected abstract List<Integer> getExpectedHouseRevisions();

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
		session.persist( p );
		session.getTransaction().commit();

		// Rev 2
		session.getTransaction().begin();
		n.setName( "Changed name" );
		session.merge( p );
		session.getTransaction().commit();

		// Rev 3
		session.getTransaction().begin();
		Name n2 = new Name();
		n2.setName( "name2" );
		p.getNames().add( n2 );
		session.getTransaction().commit();

		personId = p.getId();

		// Rev 4
		session.getTransaction().begin();
		Child child1 = new Child();
		Parent parent = new Parent();
		parent.setName( "P1" );
		child1.setParent( parent );
		parent.getChildren().add( child1 );
		session.persist( child1 );
		session.persist( parent );
		session.getTransaction().commit();

		// Rev 5
		session.getTransaction().begin();
		Child child2 = new Child();
		parent.getChildren().add( child2 );
		child2.setParent( parent );
		session.persist( child2 );
		session.persist( parent );
		session.getTransaction().commit();

		parentId = parent.getId();

		// Rev 6
		session.getTransaction().begin();
		House house = new House();
		house.getColors().add( "Red" );
		session.persist( house );
		session.getTransaction().commit();

		// Rev 7
		session.getTransaction().begin();
		house.getColors().add( "Blue" );
		session.merge( house );
		session.getTransaction().commit();

		houseId = house.getId();

		session.close();
	}

	@Test
	public void testPersonRevisionCount() {
		assert getAuditReader().getRevisions( Person.class, personId ).equals( getExpectedPersonRevisions() );
	}

	@Test
	@JiraKey(value = "HHH-10201")
	public void testParentRevisionCount() {
		assert getAuditReader().getRevisions( Parent.class, parentId ).equals( getExpectedParentRevisions() );
	}

	@Test
	@JiraKey(value = "HHH-10201")
	public void testHouseRevisionCount() {
		assert getAuditReader().getRevisions( House.class, houseId ).equals( getExpectedHouseRevisions() );
	}
}
