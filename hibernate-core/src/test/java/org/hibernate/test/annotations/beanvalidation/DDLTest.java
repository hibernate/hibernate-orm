/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.beanvalidation;

import java.util.Map;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

import org.hibernate.testing.TestForIssue;
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
		Column stateColumn = (Column) classMapping.getProperty( "state" ).getColumnIterator().next();
		assertEquals( stateColumn.getLength(), 3 );
		Column zipColumn = (Column) classMapping.getProperty( "zip" ).getColumnIterator().next();
		assertEquals( zipColumn.getLength(), 5 );
		assertFalse( zipColumn.isNullable() );
	}

	@Test
	public void testApplyOnIdColumn() throws Exception {
		PersistentClass classMapping = metadata().getEntityBinding( Tv.class.getName() );
		Column serialColumn = (Column) classMapping.getIdentifierProperty().getColumnIterator().next();
		assertEquals( "Validator annotation not applied on ids", 2, serialColumn.getLength() );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-5281" )
	public void testLengthConstraint() throws Exception {
		PersistentClass classMapping = metadata().getEntityBinding( Tv.class.getName() );
		Column modelColumn = (Column) classMapping.getProperty( "model" ).getColumnIterator().next();
		assertEquals( modelColumn.getLength(), 5 );
	}

	@Test
	public void testApplyOnManyToOne() throws Exception {
		PersistentClass classMapping = metadata().getEntityBinding( TvOwner.class.getName() );
		Column serialColumn = (Column) classMapping.getProperty( "tv" ).getColumnIterator().next();
		assertEquals( "Validator annotations not applied on associations", false, serialColumn.isNullable() );
	}

	@Test
	public void testSingleTableAvoidNotNull() throws Exception {
		PersistentClass classMapping = metadata().getEntityBinding( Rock.class.getName() );
		Column serialColumn = (Column) classMapping.getProperty( "bit" ).getColumnIterator().next();
		assertTrue( "Notnull should not be applied on single tables", serialColumn.isNullable() );
	}

	@Test
	public void testNotNullOnlyAppliedIfEmbeddedIsNotNullItself() throws Exception {
		PersistentClass classMapping = metadata().getEntityBinding( Tv.class.getName() );
		Property property = classMapping.getProperty( "tuner.frequency" );
		Column serialColumn = (Column) property.getColumnIterator().next();
		assertEquals(
				"Validator annotations are applied on tuner as it is @NotNull", false, serialColumn.isNullable()
		);

		property = classMapping.getProperty( "recorder.time" );
		serialColumn = (Column) property.getColumnIterator().next();
		assertEquals(
				"Validator annotations are applied on tuner as it is @NotNull", true, serialColumn.isNullable()
		);
	}

	@Override
	protected void addSettings(Map settings) {
		settings.put( "javax.persistence.validation.mode", "ddl" );
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
