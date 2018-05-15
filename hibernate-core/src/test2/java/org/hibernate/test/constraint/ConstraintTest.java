/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.constraint;

import java.util.Iterator;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.UniqueKey;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Brett Meyer
 */
public class ConstraintTest extends BaseNonConfigCoreFunctionalTestCase {
	
	private static final int MAX_NAME_LENGTH = 30;
	
	private static final String EXPLICIT_FK_NAME_NATIVE = "fk_explicit_native";
	
	private static final String EXPLICIT_FK_NAME_JPA = "fk_explicit_jpa";
	
	private static final String EXPLICIT_UK_NAME = "uk_explicit";
	
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { DataPoint.class, DataPoint2.class };
	}
	
	@Test
	@TestForIssue( jiraKey = "HHH-7797" )
	public void testUniqueConstraints() {
		Column column = (Column) metadata().getEntityBinding( DataPoint.class.getName() )
				.getProperty( "foo1" ).getColumnIterator().next();
		assertFalse( column.isNullable() );
		assertTrue( column.isUnique() );

		column = (Column) metadata().getEntityBinding( DataPoint.class.getName() )
				.getProperty( "foo2" ).getColumnIterator().next();
		assertTrue( column.isNullable() );
		assertTrue( column.isUnique() );

		column = (Column) metadata().getEntityBinding( DataPoint.class.getName() )
				.getProperty( "id" ).getColumnIterator().next();
		assertFalse( column.isNullable() );
		assertTrue( column.isUnique() );
	}
	
	@Test
	@TestForIssue( jiraKey = "HHH-1904" )
	public void testConstraintNameLength() {
		int foundCount = 0;
		for ( Namespace namespace : metadata().getDatabase().getNamespaces() ) {
			for ( org.hibernate.mapping.Table table : namespace.getTables() ) {
				Iterator fkItr = table.getForeignKeyIterator();
				while (fkItr.hasNext()) {
					ForeignKey fk = (ForeignKey) fkItr.next();
					assertTrue( fk.getName().length() <= MAX_NAME_LENGTH );

					// ensure the randomly generated constraint name doesn't
					// happen if explicitly given
					Column column = fk.getColumn( 0 );
					if ( column.getName().equals( "explicit_native" ) ) {
						foundCount++;
						assertEquals( fk.getName(), EXPLICIT_FK_NAME_NATIVE );
					}
					else if ( column.getName().equals( "explicit_jpa" ) ) {
						foundCount++;
						assertEquals( fk.getName(), EXPLICIT_FK_NAME_JPA );
					}
				}

				Iterator ukItr = table.getUniqueKeyIterator();
				while (ukItr.hasNext()) {
					UniqueKey uk = (UniqueKey) ukItr.next();
					assertTrue( uk.getName().length() <= MAX_NAME_LENGTH );

					// ensure the randomly generated constraint name doesn't
					// happen if explicitly given
					Column column = uk.getColumn( 0 );
					if ( column.getName().equals( "explicit" ) ) {
						foundCount++;
						assertEquals( uk.getName(), EXPLICIT_UK_NAME );
					}
				}
			}

		}
		
		assertEquals("Could not find the necessary columns.", 3, foundCount);
	}
	
	@Entity
	@Table( name = "DataPoint", uniqueConstraints = {
			@UniqueConstraint( name = EXPLICIT_UK_NAME, columnNames = { "explicit" } )
	} )
	public static class DataPoint {
		@Id
		@GeneratedValue
		@javax.persistence.Column( nullable = false, unique = true)
		public long id;
		
		@javax.persistence.Column( nullable = false, unique = true)
		public String foo1;
		
		@javax.persistence.Column( nullable = true, unique = true)
		public String foo2;
		
		public String explicit;
	}
	
	@Entity
	@Table( name = "DataPoint2" )
	public static class DataPoint2 {
		@Id
		@GeneratedValue
		public long id;
		
		@OneToOne
		public DataPoint dp;
		
		@OneToOne
		@org.hibernate.annotations.ForeignKey(name = EXPLICIT_FK_NAME_NATIVE)
		@JoinColumn(name = "explicit_native")
		public DataPoint explicit_native;
		
		@OneToOne
		@JoinColumn(name = "explicit_jpa", foreignKey = @javax.persistence.ForeignKey(name = EXPLICIT_FK_NAME_JPA))
		public DataPoint explicit_jpa;
	}
}
