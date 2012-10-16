/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.annotations.beanvalidation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.PrimaryKey;
import org.hibernate.test.util.SchemaUtil;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * Test verifying that DDL constraints get applied when Bean Validation / Hibernate Validator are enabled.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class DDLTest extends BaseCoreFunctionalTestCase {
	@Test
	public void testBasicDDL() {
		Column stateColumn = SchemaUtil.getColumn( Address.class, "state", metadata() );
		assertEquals( stateColumn.getSize().getLength(), 3 );
		Column zipColumn = SchemaUtil.getColumn( Address.class, "zip", metadata() );
		assertEquals( zipColumn.getSize().getLength(), 5 );
		assertFalse( zipColumn.isNullable() );
	}

	@Test
	public void testApplyOnIdColumn() throws Exception {
		PrimaryKey id = SchemaUtil.getPrimaryKey( Tv.class, metadata() );
		assertEquals( "Validator annotation not applied on ids", 2,
				id.getColumns().get( 0 ).getSize().getLength() );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-5281" )
	public void testLengthConstraint() throws Exception {
		Column column = SchemaUtil.getColumn( Tv.class, "model", metadata() );
		assertEquals( column.getSize().getLength(), 5 );
	}

	@FailureExpectedWithNewMetamodel
	@Test
	public void testApplyOnManyToOne() throws Exception {
		Column column = SchemaUtil.getColumn( TvOwner.class, "tv", metadata() );
		assertEquals( "Validator annotations not applied on associations", false, column.isNullable() );
	}

	@Test
	public void testSingleTableAvoidNotNull() throws Exception {
		Column column = SchemaUtil.getColumn( Rock.class, "bit", metadata() );
		assertTrue( "Notnull should not be applied on single tables", column.isNullable() );
	}

	@FailureExpectedWithNewMetamodel
	@Test
	public void testNotNullOnlyAppliedIfEmbeddedIsNotNullItself() throws Exception {
		Column column = SchemaUtil.getColumn( Tv.class, "tuner.frequency", metadata() );
		assertEquals(
				"Validator annotations are applied on tuner as it is @NotNull", false, column.isNullable()
		);

		column = SchemaUtil.getColumn( Tv.class, "recorder.time", metadata() );
		assertEquals(
				"Validator annotations are applied on tuner as it is @NotNull", true, column.isNullable()
		);
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
