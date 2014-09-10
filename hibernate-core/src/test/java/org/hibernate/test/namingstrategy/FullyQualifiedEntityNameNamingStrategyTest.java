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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.EJB3NamingStrategy;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeBinding;
import org.hibernate.metamodel.spi.binding.PluralAttributeKeyBinding;
import org.hibernate.metamodel.spi.relational.ForeignKey;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
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
	@FailureExpectedWithNewMetamodel
	public void testEntityTable() throws Exception {
		final EntityBinding entityBinding = metadata().getEntityBinding( Workflow.class.getName() );
		final String expectedTableName = transformEntityName( Workflow.class.getName() );
		assertEquals( expectedTableName, entityBinding.getPrimaryTableName() );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9327")
	@FailureExpectedWithNewMetamodel
	public void testElementCollectionTable() {
		final EntityBinding entityBinding = metadata().getEntityBinding( Workflow.class.getName() );
		final PluralAttributeBinding collectionBinding = (PluralAttributeBinding) entityBinding.locateAttributeBinding( "localized" );
		final PluralAttributeKeyBinding keyBinding = collectionBinding.getPluralAttributeKeyBinding();
		final String expectedTableName = transformEntityName( Workflow.class.getName() ) + "_localized";
		assertEquals(
				expectedTableName,
				keyBinding.getCollectionTable().getLogicalName().getText()
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9327")
	@FailureExpectedWithNewMetamodel
	public void testManyToManyCollectionTable() {
		final EntityBinding entityBinding = metadata().getEntityBinding( Category.class.getName() );
		final PluralAttributeBinding collectionBinding = (PluralAttributeBinding) entityBinding.locateAttributeBinding( "items" );
		final PluralAttributeKeyBinding keyBinding = collectionBinding.getPluralAttributeKeyBinding();
		final String expectedTableName = transformEntityName( Category.class.getName() ) + "_" + transformEntityName( Item.class.getName() );
		assertEquals( expectedTableName, keyBinding.getCollectionTable().getLogicalName().getText() );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9327")
	@FailureExpectedWithNewMetamodel
	public void testManyToManyForeignKeys() {
		final EntityBinding entityBinding = metadata().getEntityBinding( Category.class.getName() );
		final PluralAttributeBinding collectionBinding = (PluralAttributeBinding) entityBinding.locateAttributeBinding( "items" );
		final PluralAttributeKeyBinding keyBinding = collectionBinding.getPluralAttributeKeyBinding();

		final String expectedOwnerFK = transformEntityName( Category.class.getName() ) + "_id";
		final String expectedInverseFK = transformEntityName( Item.class.getName() ) + "_items_id";

		boolean ownerFKFound = false;
		boolean inverseFKFound = false;
		for ( ForeignKey fk : keyBinding.getCollectionTable().getForeignKeys() ) {
			final String fkColumnName = fk.getColumnMappings().iterator().next().getSourceColumn().getColumnName().getText();
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
