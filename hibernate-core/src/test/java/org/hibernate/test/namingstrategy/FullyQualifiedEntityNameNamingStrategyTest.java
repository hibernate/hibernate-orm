/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
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
package org.hibernate.test.namingstrategy;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.EJB3NamingStrategy;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import org.junit.Test;

public class FullyQualifiedEntityNameNamingStrategyTest extends BaseCoreFunctionalTestCase {
	@Override
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setNamingStrategy( new MyNamingStrategy() );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Category.class, Item.class, Workflow.class };
	}

	@Test
	@TestForIssue(jiraKey = "HHH-4312")
	@FailureExpected(jiraKey = "HHH-4312")
	public void testEntityTable() throws Exception {
		final PersistentClass classMapping = configuration().getClassMapping( Workflow.class.getName() );
		final String expectedTableName = transformEntityName( Workflow.class.getName() );
		assertEquals( expectedTableName, classMapping.getTable().getName() );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9327")
	public void testElementCollectionTable() {
		final Collection collectionMapping = configuration().getCollectionMapping(
				Workflow.class.getName() + ".localized"
		);
		final String expectedTableName = transformEntityName( Workflow.class.getName() ) + "_localized";
		assertEquals( expectedTableName, collectionMapping.getCollectionTable().getName() );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9327")
	public void testManyToManyCollectionTable() {
		final Collection collectionMapping = configuration().getCollectionMapping(
				Category.class.getName() + "." + "items"
		);
		final String expectedTableName = transformEntityName( Category.class.getName() ) + "_" + transformEntityName( Item.class.getName() );
		assertEquals( expectedTableName, collectionMapping.getCollectionTable().getName() );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9327")
	public void testManyToManyForeignKeys() {
		final Collection ownerCollectionMapping = configuration().getCollectionMapping(
				Category.class.getName() + "." + "items"
		);
		final String expectedOwnerFK = transformEntityName( Category.class.getName() ) + "_id";
		final String expectedInverseFK = transformEntityName( Item.class.getName() ) + "_items_id";

		boolean ownerFKFound = false;
		boolean inverseFKFound = false;
		for ( Iterator it = ownerCollectionMapping.getCollectionTable().getForeignKeyIterator(); it.hasNext(); ) {
			final String fkColumnName = ( (ForeignKey) it.next() ).getColumn( 0 ).getName();
			if ( expectedOwnerFK.equals( fkColumnName ) ) {
				ownerFKFound = true;
			}
			else if ( expectedInverseFK.equals( fkColumnName ) ) {
				inverseFKFound = true;
			}
		}
		assertTrue( ownerFKFound );
		assertTrue( inverseFKFound );
	}

	static String transformEntityName(String entityName) {
		return entityName.replaceAll( "\\.", "_" );
	}

	public static class MyNamingStrategy extends EJB3NamingStrategy {

		private static final long serialVersionUID = -5713413771290957530L;

		@Override
		public String classToTableName(String className) {
			return transformEntityName( className );
		}

		@Override
		public String collectionTableName(String ownerEntity, String ownerEntityTable, String associatedEntity, String associatedEntityTable, String propertyName) {
			return transformEntityName( ownerEntity ) + "_" +
					( associatedEntityTable != null ?
						transformEntityName( associatedEntity ) :
						StringHelper.unqualify( propertyName )
					);
		}

		@Override
		public String foreignKeyColumnName(String propertyName, String propertyEntityName, String propertyTableName, String referencedColumnName) {
			if ( propertyName == null ) {
				return columnName( transformEntityName( propertyEntityName ) + "_" + referencedColumnName );
			}
			else {
				return columnName( transformEntityName( propertyEntityName ) + "_" + propertyName + "_" + referencedColumnName );
			}
		}
	}
}
