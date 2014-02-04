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

import static org.junit.Assert.assertTrue;

import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
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
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Brett Meyer
 */
@FailureExpectedWithNewMetamodel
public class ConstraintTest extends BaseCoreFunctionalTestCase {

	private static final int MAX_NAME_LENGTH = 30;

	private static final String EXPLICIT_UK_NAME = "EXPLICIT_UK_NAME";

	private static final String EXPLICIT_COLUMN_NAME_NATIVE = "EXPLICIT_COLUMN_NAME_NATIVE";
	private static final String EXPLICIT_FK_NAME_NATIVE = "EXPLICIT_FK_NAME_NATIVE";
	private static final String EXPLICIT_COLUMN_NAME_JPA_O2O = "EXPLICIT_COLUMN_NAME_JPA_O2O";
	private static final String EXPLICIT_FK_NAME_JPA_O2O = "EXPLICIT_FK_NAME_JPA_O2O";
	private static final String EXPLICIT_COLUMN_NAME_JPA_M2O = "EXPLICIT_COLUMN_NAME_JPA_M2O";
	private static final String EXPLICIT_FK_NAME_JPA_M2O = "EXPLICIT_FK_NAME_JPA_M2O";
	private static final String EXPLICIT_COLUMN_NAME_JPA_M2M = "EXPLICIT_COLUMN_NAME_JPA_M2M";
	private static final String EXPLICIT_FK_NAME_JPA_M2M = "EXPLICIT_FK_NAME_JPA_M2M";
	private static final String EXPLICIT_COLUMN_NAME_JPA_ELEMENT = "EXPLICIT_COLUMN_NAME_JPA_ELEMENT";
	private static final String EXPLICIT_FK_NAME_JPA_ELEMENT = "EXPLICIT_FK_NAME_JPA_ELEMENT";

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				DataPoint.class, DataPoint2.class
		};
	}

	@Test
	@TestForIssue( jiraKey = "HHH-7797" )
	public void testUniqueConstraints() {
		TableSpecification table = SchemaUtil.getTable( DataPoint.class, metadata() );
		assertTrue( SchemaUtil.hasUniqueKey( table, "foo" ) );
	}

	@Test
	public void testConstraintNames() {
		TableSpecification table1 = SchemaUtil.getTable( DataPoint.class, metadata() );
		assertTrue( SchemaUtil.hasUniqueKey( table1, EXPLICIT_UK_NAME, "explicit" ) );

		TableSpecification table2 = SchemaUtil.getTable( DataPoint.class, metadata() );
		assertTrue( SchemaUtil.hasForeignKey( table2, EXPLICIT_FK_NAME_NATIVE, EXPLICIT_COLUMN_NAME_NATIVE ) );
		assertTrue( SchemaUtil.hasForeignKey( table2, EXPLICIT_FK_NAME_JPA_O2O, EXPLICIT_COLUMN_NAME_JPA_O2O ) );
		assertTrue( SchemaUtil.hasForeignKey( table2, EXPLICIT_FK_NAME_JPA_M2O, EXPLICIT_COLUMN_NAME_JPA_M2O ) );
		assertTrue( SchemaUtil.hasForeignKey( table2, EXPLICIT_FK_NAME_JPA_M2M, EXPLICIT_COLUMN_NAME_JPA_M2M ) );
		assertTrue( SchemaUtil.hasForeignKey( table2, EXPLICIT_FK_NAME_JPA_ELEMENT, EXPLICIT_COLUMN_NAME_JPA_ELEMENT ) );
		
		testConstraintLength( table1 );
		testConstraintLength( table2 );
	}
	
	private void testConstraintLength(TableSpecification table) {
		for (UniqueKey uk : table.getUniqueKeys()) {
			assertTrue(uk.getName().length() <= MAX_NAME_LENGTH);
		}
		for (ForeignKey fk : table.getForeignKeys()) {
			assertTrue(fk.getName().length() <= MAX_NAME_LENGTH);
		}
	}

	@Entity
	@Table( name = "DataPoint", uniqueConstraints = {
			@UniqueConstraint( name = EXPLICIT_UK_NAME, columnNames = { "explicit" } )
	} )
	public static class DataPoint {
		@Id
		@GeneratedValue
		public long id;

		@javax.persistence.Column( unique = true)
		public String foo;

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
		@JoinColumn(name = EXPLICIT_COLUMN_NAME_NATIVE)
		public DataPoint explicit_native;

		@OneToOne
		@JoinColumn(name = EXPLICIT_COLUMN_NAME_JPA_O2O,
				foreignKey = @javax.persistence.ForeignKey(name = EXPLICIT_FK_NAME_JPA_O2O))
		public DataPoint explicit_jpa_o2o;

		@ManyToOne
		@JoinColumn(name = EXPLICIT_COLUMN_NAME_JPA_M2O,
				foreignKey = @javax.persistence.ForeignKey(name = EXPLICIT_FK_NAME_JPA_M2O))
		private DataPoint explicit_jpa_m2o;

		@ManyToMany
		@JoinTable(joinColumns = @JoinColumn(name = EXPLICIT_COLUMN_NAME_JPA_M2M),
				foreignKey = @javax.persistence.ForeignKey(name = EXPLICIT_FK_NAME_JPA_M2M))
		private Set<DataPoint> explicit_jpa_m2m;

		@ElementCollection
		@CollectionTable(joinColumns =  @JoinColumn(name = EXPLICIT_COLUMN_NAME_JPA_ELEMENT),
				foreignKey = @javax.persistence.ForeignKey(name = EXPLICIT_FK_NAME_JPA_ELEMENT))
		private Set<String> explicit_jpa_element;
	}

	public static enum SimpleEnum {
		FOO1, FOO2, FOO3;
	}
}