/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.test.constraint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.metamodel.spi.relational.ForeignKey;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.metamodel.spi.relational.UniqueKey;
import org.hibernate.test.util.SchemaUtil;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.junit.Test;

/**
 * @author Brett Meyer
 */
public class ConstraintTest extends BaseCoreFunctionalTestCase {

	private static final int MAX_NAME_LENGTH = 30;

	private static final String EXPLICIT_UK_NAME = "EXPLICIT_UK_NAME";

	private static final String EXPLICIT_COLUMN_NAME_O2O = "EXPLICIT_COLUMN_NAME_O2O";
	private static final String EXPLICIT_FK_NAME_O2O = "EXPLICIT_FK_NAME_O2O";
	private static final String EXPLICIT_COLUMN_NAME_M2O = "EXPLICIT_COLUMN_NAME_M2O";
	private static final String EXPLICIT_FK_NAME_M2O = "EXPLICIT_FK_NAME_M2O";
	private static final String EXPLICIT_JOINTABLE_NAME_M2M = "EXPLICIT_JOINTABLE_NAME_M2M";
	private static final String EXPLICIT_COLUMN_NAME_M2M = "EXPLICIT_COLUMN_NAME_M2M";
	private static final String EXPLICIT_FK_NAME_M2M = "EXPLICIT_FK_NAME_M2M";
	private static final String EXPLICIT_COLLECTIONTABLE_NAME_ELEMENT = "EXPLICIT_COLLECTIONTABLE_NAME_ELEMENT";
	private static final String EXPLICIT_COLUMN_NAME_ELEMENT = "EXPLICIT_COLUMN_NAME_ELEMENT";
	private static final String EXPLICIT_FK_NAME_ELEMENT = "EXPLICIT_FK_NAME_ELEMENT";
	private static final String INDEX_1 = "INDEX_1";
	private static final String INDEX_2 = "INDEX_2";
	private static final String INDEX_3 = "INDEX_3";
	private static final String INDEX_4 = "INDEX_4";
	private static final String INDEX_5 = "INDEX_5";
	private static final String INDEX_6 = "INDEX_6";

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				DataPoint.class, DataPoint2.class
		};
	}

	@Test
	public void testConstraints() {
		String[] sqlActions = new SchemaExport( metadata() ).getCreateSQL();
		// TODO: This should cover most dialects, but may need better tied to them otherwise.
		
		assertEquals( 1, findActions( ".*constraint.*" + INDEX_1 + ".*unique.*foo2.*", sqlActions ) );
		
		int count = 0;
		// Yep, this is stupid.  But, need to ensure that the UC is created only once.  A 1 shot regex would be hell.
		count += findActions( ".*constraint.*" + INDEX_2 + ".*unique.*foo3.*foo4.*", sqlActions );
		count += findActions( ".*constraint.*" + INDEX_2 + ".*unique.*foo4.*foo3.*", sqlActions );
		count += findActions( ".*constraint.*" + INDEX_3 + ".*unique.*foo3.*foo4.*", sqlActions );
		count += findActions( ".*constraint.*" + INDEX_3 + ".*unique.*foo4.*foo3.*", sqlActions );
		assertEquals( 1, count );
		
		// Ensure no UCs were created for these.
		assertEquals( 0, findActions( ".*constraint.*unique.*foo5.*", sqlActions ) );
		assertEquals( 0, findActions( ".*constraint.*unique.*foo6.*", sqlActions ) );
		
		// Check indexes
		assertEquals( 1, findActions( ".*index.*" + INDEX_4 + ".*foo5.*foo6.*", sqlActions ) );
		assertEquals( 1, findActions( ".*index.*" + INDEX_5 + ".*foo6.*foo5.*", sqlActions ) );
		assertEquals( 1, findActions( ".*index.*" + INDEX_6 + ".*foo5.*", sqlActions ) );
	}
	
	private int findActions(String regex, String[] sqlActions) {
		int count = 0;
		for (String sqlAction : sqlActions) {
			count += sqlAction.matches( regex ) ? 1 : 0;
		}
		return count;
	}

	@Test
	public void testConstraintNames() {
		TableSpecification table1 = SchemaUtil.getTable( DataPoint.class, metadata() );
		assertTrue( SchemaUtil.hasUniqueKey( table1, EXPLICIT_UK_NAME, "explicit" ) );

		TableSpecification table2 = SchemaUtil.getTable( DataPoint2.class, metadata() );
		TableSpecification joinTable = SchemaUtil.getTable( EXPLICIT_JOINTABLE_NAME_M2M, metadata() );
		TableSpecification collectionTable = SchemaUtil.getTable( EXPLICIT_COLLECTIONTABLE_NAME_ELEMENT, metadata() );
		assertTrue( SchemaUtil.hasForeignKey( table2, EXPLICIT_FK_NAME_O2O, EXPLICIT_COLUMN_NAME_O2O ) );
		assertTrue( SchemaUtil.hasForeignKey( table2, EXPLICIT_FK_NAME_M2O, EXPLICIT_COLUMN_NAME_M2O ) );
		assertTrue( SchemaUtil.hasForeignKey( joinTable, EXPLICIT_FK_NAME_M2M, EXPLICIT_COLUMN_NAME_M2M ) );
		assertTrue( SchemaUtil.hasForeignKey( collectionTable, EXPLICIT_FK_NAME_ELEMENT, EXPLICIT_COLUMN_NAME_ELEMENT ) );
		
		testConstraintLength( table1 );
		testConstraintLength( table2 );
	}
	
	private void testConstraintLength(TableSpecification table) {
		for (UniqueKey uk : table.getUniqueKeys()) {
			assertTrue(uk.getName().toString().length() <= MAX_NAME_LENGTH);
		}
		for (ForeignKey fk : table.getForeignKeys()) {
			assertTrue(fk.getName().toString().length() <= MAX_NAME_LENGTH);
		}
	}

	@Entity
	@Table( name = "DataPoint",
			uniqueConstraints = { @UniqueConstraint( name = EXPLICIT_UK_NAME, columnNames = { "explicit" } ) },
			indexes = {
					@Index(columnList = "foo2", unique = true, name = INDEX_1),
					@Index(columnList = "foo3,foo4", unique = true, name = INDEX_2),
					@Index(columnList = "foo4,foo3", unique = true, name = INDEX_3),
					@Index(columnList = "foo5,foo6", name = INDEX_4),
					@Index(columnList = "foo6,foo5", name = INDEX_5),
					@Index(columnList = "foo5", name = INDEX_6) } )
	private static class DataPoint {
		@Id
		@GeneratedValue
		public long id;

		public String explicit;

		@javax.persistence.Column(unique = true)
		public String foo1;
		
		public String foo2;
		
		public String foo3;
		
		public String foo4;
		
		public String foo5;
		
		public String foo6;
	}

	@Entity
	@Table( name = "DataPoint2" )
	private static class DataPoint2 {
		@Id
		@GeneratedValue
		public long id;

		@OneToOne
		public DataPoint dp;

		@OneToOne
		@JoinColumn(name = EXPLICIT_COLUMN_NAME_O2O,
				foreignKey = @javax.persistence.ForeignKey(name = EXPLICIT_FK_NAME_O2O))
		public DataPoint explicit_o2o;

		@ManyToOne
		@JoinColumn(name = EXPLICIT_COLUMN_NAME_M2O,
				foreignKey = @javax.persistence.ForeignKey(name = EXPLICIT_FK_NAME_M2O))
		public DataPoint explicit_m2o;

		@ManyToMany
		@JoinTable(name = EXPLICIT_JOINTABLE_NAME_M2M,
				joinColumns = @JoinColumn(name = EXPLICIT_COLUMN_NAME_M2M),
				foreignKey = @javax.persistence.ForeignKey(name = EXPLICIT_FK_NAME_M2M))
		public Set<DataPoint> explicit_m2m;

		@ElementCollection
		@CollectionTable(name = EXPLICIT_COLLECTIONTABLE_NAME_ELEMENT,
				joinColumns =  @JoinColumn(name = EXPLICIT_COLUMN_NAME_ELEMENT),
				foreignKey = @javax.persistence.ForeignKey(name = EXPLICIT_FK_NAME_ELEMENT))
		public Set<String> explicit_element;
	}

	public static enum SimpleEnum {
		FOO1, FOO2, FOO3;
	}
}