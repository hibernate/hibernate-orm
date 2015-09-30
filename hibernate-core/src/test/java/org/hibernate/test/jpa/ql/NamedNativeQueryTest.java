/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.jpa.ql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.SkipForDialects;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Janario Oliveira
 */
public class NamedNativeQueryTest extends BaseCoreFunctionalTestCase {

	private FromEntity createFrom(String name, String lastName) {
		Session session = openSession();
		session.getTransaction().begin();
		FromEntity fromEntity = new FromEntity( name, lastName );
		session.save( fromEntity );
		session.getTransaction().commit();
		session.close();
		return fromEntity;
	}

	private DestinationEntity createDestination(FromEntity fromEntity, String fullName) {
		final DestinationEntity destinationEntity = new DestinationEntity( fromEntity, fullName );

		Session session = openSession();
		session.getTransaction().begin();
		session.save( destinationEntity );
		session.getTransaction().commit();
		session.close();
		return destinationEntity;
	}

	@SuppressWarnings("unchecked")
	private List<DestinationEntity> findDestinationByIds(List<Integer> ids) {
		Session session = openSession();
		List<DestinationEntity> list = session
				.createQuery( "from DestinationEntity de where de.id in (:ids) order by id" )
				.setParameterList( "ids", ids ).list();
		session.close();
		return list;
	}

	@Test
	public void testSingleSelect() {
		final String name = "Name";
		final String lastName = "LastName";
		final String fullName = name + " " + lastName;
		final DestinationEntity destination = createDestination( createFrom( name, lastName ), fullName );

		Session session = openSession();
		Query select = session.getNamedQuery( "DestinationEntity.selectIds" );
		select.setParameterList( "ids", Collections.singletonList( destination.id ) );
		Object[] unique = (Object[]) select.uniqueResult();
		session.close();

		// Compare the Strings, not the actual IDs.  Can come back as, for ex,
		// a BigDecimal in Oracle.
		assertEquals( destination.id + "", unique[0] + "" );
		assertEquals( destination.from.id + "", unique[1] + "" );
		assertEquals( destination.fullNameFrom, unique[2] );
	}

	@Test
	public void testMultipleSelect() {
		final String name = "Name";
		final String lastName = "LastName";
		final List<Integer> ids = new ArrayList<Integer>();
		final int quantity = 10;
		final List<DestinationEntity> destinations = new ArrayList<DestinationEntity>();
		for ( int i = 0; i < quantity; i++ ) {
			DestinationEntity createDestination = createDestination( createFrom( name + i, lastName + i ), name + i
					+ lastName + i );
			ids.add( createDestination.id );
			destinations.add( createDestination );
		}

		Session session = openSession();
		Query select = session.getNamedQuery( "DestinationEntity.selectIds" );
		select.setParameterList( "ids", ids );
		List list = select.list();
		session.close();

		assertEquals( quantity, list.size() );
		for ( int i = 0; i < list.size(); i++ ) {
			Object[] object = (Object[]) list.get( i );
			DestinationEntity destination = destinations.get( i );
			// Compare the Strings, not the actual IDs.  Can come back as, for ex,
			// a BigDecimal in Oracle.
			assertEquals( destination.id + "", object[0] + "" );
			assertEquals( destination.from.id + "", object[1] + "" );
			assertEquals( destination.fullNameFrom, object[2] );
		}
	}

	@Test
	public void testInsertSingleValue() {
		final String name = "Name";
		final String lastName = "LastName";
		final String fullName = name + " " + lastName;
		final FromEntity fromEntity = createFrom( name, lastName );
		final int id = 10000;// id fake

		Session session = openSession();
		session.getTransaction().begin();
		Query insert = session.getNamedQuery( "DestinationEntity.insert" );
		insert.setParameter( "generatedId", id );
		insert.setParameter( "fromId", fromEntity.id );
		insert.setParameter( "fullName", fullName );
		int executeUpdate = insert.executeUpdate();
		assertEquals( 1, executeUpdate );
		session.getTransaction().commit();
		session.close();

		session = openSession();
		DestinationEntity get = (DestinationEntity) session.get( DestinationEntity.class, id );
		session.close();

		assertEquals( fromEntity, get.from );
		assertEquals( fullName, get.fullNameFrom );
	}

	@Test
	@SkipForDialects( {
		@SkipForDialect( value = MySQLDialect.class, comment = "MySQL appears to have trouble with fe.id selected twice in one statement"),
		@SkipForDialect( value = SQLServerDialect.class, comment = "SQL Server does not support the || operator.")
	} )
	// TODO: Re-form DestinationEntity.insertSelect to something more supported?
	public void testInsertMultipleValues() {
		final String name = "Name";
		final String lastName = "LastName";
		final List<Integer> ids = new ArrayList<Integer>();
		final int quantity = 10;
		final List<FromEntity> froms = new ArrayList<FromEntity>();
		for ( int i = 0; i < quantity; i++ ) {
			FromEntity fe = createFrom( name + i, lastName + i );
			froms.add( fe );
			ids.add( fe.id );
		}

		Session session = openSession();
		session.getTransaction().begin();
		Query insertSelect = session.getNamedQuery( "DestinationEntity.insertSelect" );
		insertSelect.setParameterList( "ids", ids );
		int executeUpdate = insertSelect.executeUpdate();
		assertEquals( quantity, executeUpdate );

		session.getTransaction().commit();
		session.close();

		List<DestinationEntity> list = findDestinationByIds( ids );
		assertEquals( quantity, list.size() );

		for ( int i = 0; i < quantity; i++ ) {
			DestinationEntity de = (DestinationEntity) list.get( i );
			FromEntity from = froms.get( i );
			assertEquals( from, de.from );
			assertEquals( from.name + from.lastName, de.fullNameFrom );
		}
	}

	@Test
	public void testUpdateSingleValue() {
		final String name = "Name";
		final String lastName = "LastName";
		final String fullName = name + " " + lastName;

		final FromEntity fromEntity = createFrom( name, lastName );
		final DestinationEntity destinationEntity = createDestination( fromEntity, fullName );

		final String inverseFullName = lastName + " " + name;
		final FromEntity anotherFrom = createFrom( lastName, name );

		Session session = openSession();
		session.getTransaction().begin();
		Query update = session.getNamedQuery( "DestinationEntity.update" );
		update.setParameter( "idFrom", anotherFrom.id );
		update.setParameter( "fullName", inverseFullName );
		update.setParameterList( "ids", Collections.singletonList( destinationEntity.id ) );

		int executeUpdate = update.executeUpdate();
		assertEquals( 1, executeUpdate );

		session.getTransaction().commit();
		session.close();

		session = openSession();
		session.beginTransaction();
		DestinationEntity get = (DestinationEntity) session.get( DestinationEntity.class, destinationEntity.id );

		assertEquals( anotherFrom, get.from );
		assertEquals( inverseFullName, get.fullNameFrom );
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testUpdateMultipleValues() {
		final String name = "Name";
		final String lastName = "LastName";
		final List<Integer> ids = new ArrayList<Integer>();
		final int quantity = 10;
		final List<DestinationEntity> destinations = new ArrayList<DestinationEntity>();
		for ( int i = 0; i < quantity; i++ ) {
			FromEntity fe = createFrom( name + i, lastName + i );
			DestinationEntity destination = createDestination( fe, fe.name + fe.lastName );
			destinations.add( destination );
			ids.add( destination.id );
		}

		final String inverseFullName = lastName + " " + name;
		final FromEntity anotherFrom = createFrom( lastName, name );

		Session session = openSession();
		session.getTransaction().begin();
		Query update = session.getNamedQuery( "DestinationEntity.update" );
		update.setParameter( "idFrom", anotherFrom.id );
		update.setParameter( "fullName", inverseFullName );
		update.setParameterList( "ids", ids );

		int executeUpdate = update.executeUpdate();
		assertEquals( quantity, executeUpdate );

		session.getTransaction().commit();
		session.close();

		List<DestinationEntity> list = findDestinationByIds( ids );
		assertEquals( quantity, list.size() );

		for ( int i = 0; i < quantity; i++ ) {
			DestinationEntity updated = (DestinationEntity) list.get( i );

			assertEquals( anotherFrom, updated.from );
			assertEquals( inverseFullName, updated.fullNameFrom );
		}
	}

	@Test
	public void testDeleteSingleValue() {
		final String name = "Name";
		final String lastName = "LastName";
		final String fullName = name + " " + lastName;

		final FromEntity fromEntity = createFrom( name, lastName );
		final DestinationEntity destinationEntity = createDestination( fromEntity, fullName );

		Session session = openSession();
		session.getTransaction().begin();
		Query delete = session.getNamedQuery( "DestinationEntity.delete" );
		delete.setParameterList( "ids", Collections.singletonList( destinationEntity.id ) );

		int executeUpdate = delete.executeUpdate();
		assertEquals( 1, executeUpdate );

		session.getTransaction().commit();
		session.close();

		session = openSession();
		DestinationEntity get = (DestinationEntity) session.get( DestinationEntity.class, destinationEntity.id );
		session.close();

		assertNull( get );
	}

	@Test
	public void testDeleteMultipleValues() {
		final String name = "Name";
		final String lastName = "LastName";
		final List<Integer> ids = new ArrayList<Integer>();
		final int quantity = 10;
		final List<DestinationEntity> destinations = new ArrayList<DestinationEntity>();
		for ( int i = 0; i < quantity; i++ ) {
			FromEntity fe = createFrom( name + i, lastName + i );
			DestinationEntity destination = createDestination( fe, fe.name + fe.lastName );
			destinations.add( destination );
			ids.add( destination.id );
		}

		Session session = openSession();
		session.getTransaction().begin();
		Query delete = session.getNamedQuery( "DestinationEntity.delete" );
		delete.setParameterList( "ids", ids );

		int executeUpdate = delete.executeUpdate();
		assertEquals( quantity, executeUpdate );

		session.getTransaction().commit();
		session.close();

		List<DestinationEntity> list = findDestinationByIds( ids );
		assertTrue( list.isEmpty() );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { FromEntity.class, DestinationEntity.class };
	}
}
