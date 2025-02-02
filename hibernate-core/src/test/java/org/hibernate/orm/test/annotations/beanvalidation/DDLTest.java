/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.beanvalidation;

import java.util.Map;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test verifying that DDL constraints get applied when Bean Validation / Hibernate Validator are enabled.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class DDLTest extends BaseNonConfigCoreFunctionalTestCase {
	@Test
	public void testBasicDDL() {
		PersistentClass classMapping = metadata().getEntityBinding( Address.class.getName() );
		Column stateColumn = classMapping.getProperty( "state" ).getColumns().get(0);
		assertEquals( stateColumn.getLength(), (Long) 3L );
		Column zipColumn = classMapping.getProperty( "zip" ).getColumns().get(0);
		assertEquals( zipColumn.getLength(), (Long) 5L );
		assertFalse( zipColumn.isNullable() );
	}

	@Test
	public void testNotNullDDL() {
		PersistentClass classMapping = metadata().getEntityBinding( Address.class.getName() );
		Column stateColumn = classMapping.getProperty( "state" ).getColumns().get(0);
		assertFalse("Validator annotations are applied on state as it is @NotNull", stateColumn.isNullable());

		Column line1Column = classMapping.getProperty( "line1" ).getColumns().get(0);
		assertFalse("Validator annotations are applied on line1 as it is @NotEmpty", line1Column.isNullable());

		Column line2Column = classMapping.getProperty( "line2" ).getColumns().get(0);
		assertFalse("Validator annotations are applied on line2 as it is @NotBlank", line2Column.isNullable());

		Column line3Column = classMapping.getProperty( "line3" ).getColumns().get(0);
		assertTrue(
				"Validator composition of type OR should result in line3 being nullable",
				line3Column.isNullable());

		Column line4Column = classMapping.getProperty( "line4" ).getColumns().get(0);
		assertFalse(
				"Validator composition of type OR should result in line4 being not-null",
				line4Column.isNullable());

		Column line5Column = classMapping.getProperty( "line5" ).getColumns().get(0);
		assertFalse(
				"Validator composition of type AND should result in line5 being not-null",
				line5Column.isNullable());

		Column line6Column = classMapping.getProperty( "line6" ).getColumns().get(0);
		assertFalse(
				"Validator composition of type AND should result in line6 being not-null",
				line6Column.isNullable());

		Column line7Column = classMapping.getProperty( "line7" ).getColumns().get(0);
		assertTrue(
				"Validator composition of type OR should result in line7 being nullable",
				line7Column.isNullable());

		Column line8Column = classMapping.getProperty( "line8" ).getColumns().get( 0 );
		assertFalse(
				"Validator should result in line8 being not-null",
				line8Column.isNullable());

		Column line9Column = classMapping.getProperty( "line9" ).getColumns().get( 0 );
		assertTrue(
				"Validator should result in line9 being nullable",
				line9Column.isNullable());
	}

	@Test
	public void testApplyOnIdColumn() {
		PersistentClass classMapping = metadata().getEntityBinding( Tv.class.getName() );
		Column serialColumn = classMapping.getIdentifierProperty().getColumns().get(0);
		assertEquals( "Validator annotation not applied on ids", (Long) 2L, serialColumn.getLength() );
	}

	@Test
	public void testLengthConstraint() {
		PersistentClass classMapping = metadata().getEntityBinding( Tv.class.getName() );
		Column modelColumn = classMapping.getProperty( "model" ).getColumns().get(0);
		assertEquals( modelColumn.getLength(), (Long) 5L );
	}

	@Test
	public void testApplyOnManyToOne() {
		PersistentClass classMapping = metadata().getEntityBinding( TvOwner.class.getName() );
		Column serialColumn = classMapping.getProperty( "tv" ).getColumns().get(0);
		assertEquals( "Validator annotations not applied on associations", false, serialColumn.isNullable() );
	}

	@Test
	public void testSingleTableAvoidNotNull() {
		PersistentClass classMapping = metadata().getEntityBinding( Rock.class.getName() );
		Column serialColumn = classMapping.getProperty( "bit" ).getColumns().get(0);
		assertTrue( "Notnull should not be applied on single tables", serialColumn.isNullable() );
	}

	@Test
	public void testNotNullOnlyAppliedIfEmbeddedIsNotNullItself() {
		PersistentClass classMapping = metadata().getEntityBinding( Tv.class.getName() );
		Property property = classMapping.getProperty( "tuner.frequency" );
		Column serialColumn = property.getColumns().get(0);
		assertEquals(
				"Validator annotations are applied on tuner as it is @NotNull", false, serialColumn.isNullable()
		);

		property = classMapping.getProperty( "recorder.time" );
		serialColumn = property.getColumns().get(0);
		assertEquals(
				"Validator annotations are applied on tuner as it is @NotNull", true, serialColumn.isNullable()
		);
	}

	@Override
	protected void addSettings(Map<String,Object> settings) {
		settings.put( "jakarta.persistence.validation.mode", "ddl" );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Address.class,
				Tv.class,
				TvOwner.class,
				Rock.class
		};
	}
}
