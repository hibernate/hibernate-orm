package org.hibernate.test.collection.set;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.hibernate.Session;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Felix Feisst (feisst dot felix at gmail dot com)
 */
public class UserTypeTest extends BaseCoreFunctionalTestCase {

	@Override
	protected String[] getMappings() {
		return new String[] { "collection/set/UserTypeMapping.hbm.xml" };
	}

	@Test
	public void testWithNonNullField() {
		final PersonInfo info1 = new PersonInfo( 160, "brown" );
		final PersonInfo info2 = new PersonInfo( 170, "black" );

		Session session = openSession();

		session.getTransaction().begin();
		final PersonInfoEntity entity = new PersonInfoEntity();
		entity.setId( 1L );
		entity.getInfos().add( info1 );
		entity.getInfos().add( info2 );
		session.save( entity );
		session.getTransaction().commit();

		session.getTransaction().begin();
		entity.getInfos().remove( info1 );
		session.getTransaction().commit();

		session = openSession();
		// reread entity from DB
		PersonInfoEntity reloaded = (PersonInfoEntity) session.get( PersonInfoEntity.class, 1L );
		assertEquals( "Unexpected person infos", Collections.singleton( info2 ), reloaded.getInfos() );
	}

	@Test
	public void testWithNullField() {
		final PersonInfo info1 = new PersonInfo( 160, null ); // null field
		final PersonInfo info2 = new PersonInfo( 170, "black" );

		Session session = openSession();

		session.getTransaction().begin();
		final PersonInfoEntity entity = new PersonInfoEntity();
		entity.setId( 2L );
		entity.getInfos().add( info1 );
		entity.getInfos().add( info2 );
		session.save( entity );
		session.getTransaction().commit();

		session.getTransaction().begin();
		entity.getInfos().remove( info1 ); // info with null field (= null column) should be removed
		session.getTransaction().commit();

		session = openSession();
		// reread entity from DB
		PersonInfoEntity reloaded = (PersonInfoEntity) session.get( PersonInfoEntity.class, 2L );
		assertEquals( "Unexpected person infos", Collections.singleton( info2 ), reloaded.getInfos() );
	}

}
