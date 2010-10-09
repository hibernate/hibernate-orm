package org.hibernate.test.annotations.beanvalidation;

import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Property;
import org.hibernate.test.annotations.TestCase;

/**
 * Test verifying that DDL constraints get applied when Bean Validation / Hibernate Validator are enabled.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class DDLTest extends TestCase {

	public void testBasicDDL() {
		PersistentClass classMapping = getCfg().getClassMapping( Address.class.getName() );
		Column stateColumn = (Column) classMapping.getProperty( "state" ).getColumnIterator().next();
		assertEquals( stateColumn.getLength(), 3 );
		Column zipColumn = (Column) classMapping.getProperty( "zip" ).getColumnIterator().next();
		assertEquals( zipColumn.getLength(), 5 );
		assertFalse( zipColumn.isNullable() );
	}

	public void testApplyOnIdColumn() throws Exception {
		PersistentClass classMapping = getCfg().getClassMapping( Tv.class.getName() );
		Column serialColumn = (Column) classMapping.getIdentifierProperty().getColumnIterator().next();
		assertEquals( "Validator annotation not applied on ids", 2, serialColumn.getLength() );
	}

	/**
	 * HHH-5281
	 *
	 * @throws Exception in case the test fails
	 */
	public void testLengthConstraint() throws Exception {
		PersistentClass classMapping = getCfg().getClassMapping( Tv.class.getName() );
		Column modelColumn = (Column) classMapping.getProperty( "model" ).getColumnIterator().next();
		assertEquals( modelColumn.getLength(), 5 );
	}	

	public void testApplyOnManyToOne() throws Exception {
		PersistentClass classMapping = getCfg().getClassMapping( TvOwner.class.getName() );
		Column serialColumn = (Column) classMapping.getProperty( "tv" ).getColumnIterator().next();
		assertEquals( "Validator annotations not applied on associations", false, serialColumn.isNullable() );
	}

	public void testSingleTableAvoidNotNull() throws Exception {
		PersistentClass classMapping = getCfg().getClassMapping( Rock.class.getName() );
		Column serialColumn = (Column) classMapping.getProperty( "bit" ).getColumnIterator().next();
		assertTrue( "Notnull should not be applised on single tables", serialColumn.isNullable() );
	}

	public void testNotNullOnlyAppliedIfEmbeddedIsNotNullItself() throws Exception {
		PersistentClass classMapping = getCfg().getClassMapping( Tv.class.getName() );
		Property property = classMapping.getProperty( "tuner.frequency" );
		Column serialColumn = (Column) property.getColumnIterator().next();
		assertEquals( "Validator annotations are applied on tunner as it is @NotNull", false, serialColumn.isNullable() );

		property = classMapping.getProperty( "recorder.time" );
		serialColumn = (Column) property.getColumnIterator().next();
		assertEquals( "Validator annotations are applied on tunner as it is @NotNull", true, serialColumn.isNullable() );
	}

	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Address.class,
				Tv.class,
				TvOwner.class,
				Rock.class
		};
	}
}
