/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
