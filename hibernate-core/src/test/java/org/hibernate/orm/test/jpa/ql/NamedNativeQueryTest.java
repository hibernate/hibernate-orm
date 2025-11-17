/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.ql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.query.Query;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.SQLServerDialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Janario Oliveira
 */
@DomainModel(annotatedClasses = { FromEntity.class, DestinationEntity.class })
@SessionFactory
public class NamedNativeQueryTest {

	private FromEntity createFrom(SessionFactoryScope scope, String name, String lastName) {
		FromEntity fromEntity = new FromEntity( name, lastName );

		scope.inTransaction( session -> session.persist( fromEntity ) );

		return fromEntity;
	}

	private DestinationEntity createDestination(SessionFactoryScope scope, FromEntity fromEntity, String fullName) {
		final DestinationEntity destinationEntity = new DestinationEntity( fromEntity, fullName );

		scope.inTransaction( session -> session.persist( destinationEntity ) );

		return destinationEntity;
	}

	private List<DestinationEntity> findDestinationByIds(SessionFactoryScope scope, List<Integer> ids) {
		return scope.fromSession(
				session -> session
						.createQuery( "from DestinationEntity de where de.id in (:ids) order by id", DestinationEntity.class )
						.setParameterList( "ids", ids ).list() );
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.inTransaction( session -> scope.dropData() );
	}

	@Test
	public void testSingleSelect(SessionFactoryScope scope) {
		final String name = "Name";
		final String lastName = "LastName";
		final String fullName = name + " " + lastName;
		final DestinationEntity destination = createDestination( scope, createFrom( scope, name, lastName ), fullName );

		scope.inSession( session -> {
			Query select = session.getNamedQuery( "DestinationEntity.selectIds" );
			select.setParameterList( "ids", Collections.singletonList( destination.id ) );
			Object[] unique = (Object[]) select.uniqueResult();
			// Compare the Strings, not the actual IDs.  Can come back as, for ex,
			// a BigDecimal in Oracle.
			assertEquals( destination.id + "", unique[0] + "" );
			assertEquals( destination.from.id + "", unique[1] + "" );
			assertEquals( destination.fullNameFrom, unique[2] );
		} );
	}

	@Test
	public void testMultipleSelect(SessionFactoryScope scope) {
		final String name = "Name";
		final String lastName = "LastName";
		final List<Integer> ids = new ArrayList<>();
		final int quantity = 10;
		final List<DestinationEntity> destinations = new ArrayList<>();
		for ( int i = 0; i < quantity; i++ ) {
			DestinationEntity createDestination = createDestination( scope, createFrom( scope, name + i, lastName + i ), name + i
					+ lastName + i );
			ids.add( createDestination.id );
			destinations.add( createDestination );
		}

		scope.inSession( session -> {
			Query select = session.getNamedQuery( "DestinationEntity.selectIds" );
			select.setParameterList( "ids", ids );
			List list = select.list();

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
		} );
	}

	@Test
	public void testInsertSingleValue(SessionFactoryScope scope) {
		final String name = "Name";
		final String lastName = "LastName";
		final String fullName = name + " " + lastName;
		final FromEntity fromEntity = createFrom( scope, name, lastName );
		final int id = 10000;// id fake

		scope.inTransaction( session -> {
			Query insert = session.getNamedQuery( "DestinationEntity.insert" );
			insert.setParameter( "generatedId", id );
			insert.setParameter( "fromId", fromEntity.id );
			insert.setParameter( "fullName", fullName );
			int executeUpdate = insert.executeUpdate();
			assertEquals( 1, executeUpdate );
		} );

		scope.inSession( session -> {
			DestinationEntity de = session.find( DestinationEntity.class, id );
			assertEquals( fromEntity, de.from );
			assertEquals( fullName, de.fullNameFrom );
		} );
	}

	@Test
	@SkipForDialect( dialectClass = MySQLDialect.class, matchSubTypes = true, reason = "MySQL appears to have trouble with fe.id selected twice in one statement")
	@SkipForDialect( dialectClass = SQLServerDialect.class, reason = "SQL Server does not support the || operator.")
	// TODO: Re-form DestinationEntity.insertSelect to something more supported?
	public void testInsertMultipleValues(SessionFactoryScope scope) {
		final String name = "Name";
		final String lastName = "LastName";
		final List<Integer> ids = new ArrayList<>();
		final int quantity = 10;
		final List<FromEntity> froms = new ArrayList<>();
		for ( int i = 0; i < quantity; i++ ) {
			FromEntity fe = createFrom( scope, name + i, lastName + i );
			froms.add( fe );
			ids.add( fe.id );
		}

		scope.inTransaction( session -> {
			Query insertSelect = session.getNamedQuery( "DestinationEntity.insertSelect" );
			insertSelect.setParameterList( "ids", ids );
			int executeUpdate = insertSelect.executeUpdate();
			assertEquals( quantity, executeUpdate );
		} );

		List<DestinationEntity> list = findDestinationByIds( scope, ids );
		assertEquals( quantity, list.size() );

		for ( int i = 0; i < quantity; i++ ) {
			DestinationEntity de = list.get( i );
			FromEntity from = froms.get( i );
			assertEquals( from, de.from );
			assertEquals( from.name + from.lastName, de.fullNameFrom );
		}
	}

	@Test
	public void testUpdateSingleValue(SessionFactoryScope scope) {
		final String name = "Name";
		final String lastName = "LastName";
		final String fullName = name + " " + lastName;

		final FromEntity fromEntity = createFrom( scope, name, lastName );
		final DestinationEntity destinationEntity = createDestination( scope, fromEntity, fullName );

		final String inverseFullName = lastName + " " + name;
		final FromEntity anotherFrom = createFrom( scope, lastName, name );

		scope.inTransaction( session -> {
			Query update = session.getNamedQuery( "DestinationEntity.update" );
			update.setParameter( "idFrom", anotherFrom.id );
			update.setParameter( "fullName", inverseFullName );
			update.setParameterList( "ids", Collections.singletonList( destinationEntity.id ) );

			int executeUpdate = update.executeUpdate();
			assertEquals( 1, executeUpdate );
		} );

		scope.inTransaction( session -> {
			DestinationEntity de = session.find( DestinationEntity.class, destinationEntity.id );

			assertEquals( anotherFrom, de.from );
			assertEquals( inverseFullName, de.fullNameFrom );
		} );
	}

	@Test
	public void testUpdateMultipleValues(SessionFactoryScope scope) {
		final String name = "Name";
		final String lastName = "LastName";
		final List<Integer> ids = new ArrayList<>();
		final int quantity = 10;
		for ( int i = 0; i < quantity; i++ ) {
			FromEntity fe = createFrom( scope, name + i, lastName + i );
			DestinationEntity destination = createDestination( scope, fe, fe.name + fe.lastName );
			ids.add( destination.id );
		}

		final String inverseFullName = lastName + " " + name;
		final FromEntity anotherFrom = createFrom( scope, lastName, name );

		scope.inTransaction( session -> {
			Query update = session.getNamedQuery( "DestinationEntity.update" );
			update.setParameter( "idFrom", anotherFrom.id );
			update.setParameter( "fullName", inverseFullName );
			update.setParameterList( "ids", ids );

			int executeUpdate = update.executeUpdate();
			assertEquals( quantity, executeUpdate );
		} );

		List<DestinationEntity> list = findDestinationByIds( scope, ids );
		assertEquals( quantity, list.size() );

		for ( int i = 0; i < quantity; i++ ) {
			DestinationEntity updated = list.get( i );

			assertEquals( anotherFrom, updated.from );
			assertEquals( inverseFullName, updated.fullNameFrom );
		}
	}

	@Test
	public void testDeleteSingleValue(SessionFactoryScope scope) {
		final String name = "Name";
		final String lastName = "LastName";
		final String fullName = name + " " + lastName;

		final FromEntity fromEntity = createFrom( scope, name, lastName );
		final DestinationEntity destinationEntity = createDestination( scope, fromEntity, fullName );

		scope.inTransaction( session -> {
			Query delete = session.getNamedQuery( "DestinationEntity.delete" );
			delete.setParameterList( "ids", Collections.singletonList( destinationEntity.id ) );

			int executeUpdate = delete.executeUpdate();
			assertEquals( 1, executeUpdate );
		} );

		scope.inSession( session -> {
			DestinationEntity get = session.find( DestinationEntity.class, destinationEntity.id );
			assertNull( get );
		} );
	}

	@Test
	public void testDeleteMultipleValues(SessionFactoryScope scope) {
		final String name = "Name";
		final String lastName = "LastName";
		final List<Integer> ids = new ArrayList<>();
		final int quantity = 10;
		for ( int i = 0; i < quantity; i++ ) {
			FromEntity fe = createFrom( scope, name + i, lastName + i );
			DestinationEntity destination = createDestination( scope, fe, fe.name + fe.lastName );
			ids.add( destination.id );
		}

		scope.inTransaction( session -> {
			Query delete = session.getNamedQuery( "DestinationEntity.delete" );
			delete.setParameterList( "ids", ids );

			int executeUpdate = delete.executeUpdate();
			assertEquals( quantity, executeUpdate );
		} );

		List<DestinationEntity> list = findDestinationByIds( scope, ids );
		assertTrue( list.isEmpty() );
	}

}
