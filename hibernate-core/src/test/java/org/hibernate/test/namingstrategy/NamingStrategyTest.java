/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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

import org.junit.Test;

import org.hibernate.cfg.Configuration;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.spi.binding.AttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.RelationalValueBinding;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.relational.Table;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Emmanuel Bernard
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class NamingStrategyTest extends BaseCoreFunctionalTestCase {
	@Override
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setNamingStrategy( new TestNamingStrategy() );
	}

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class<?>[] {
                Item.class
        };
    }

	@Override
	public String[] getMappings() {
		return new String[] {
				"namingstrategy/Customers.hbm.xml"
		};
	}

	@Test
	public void testDatabaseColumnNames() {
		if ( isMetadataUsed() ) {
			EntityBinding entityBinding = metadata().getEntityBinding( Customers.class.getName() );
			assertNotNull( entityBinding );
			AttributeBinding attributeBinding = entityBinding.locateAttributeBinding( "specified_column" );
			assertNotNull( attributeBinding );
			assertTrue( SingularAttributeBinding.class.isInstance( attributeBinding ) );
			SingularAttributeBinding singularAttributeBinding = (SingularAttributeBinding) attributeBinding;
			assertEquals( 1, singularAttributeBinding.getRelationalValueBindings().size() );
			RelationalValueBinding valueBinding = singularAttributeBinding.getRelationalValueBindings().get( 0 );
			assertFalse( valueBinding.isDerived() );
			org.hibernate.metamodel.spi.relational.Column column = (org.hibernate.metamodel.spi.relational.Column) valueBinding
					.getValue();
			assertEquals( "CN_specified_column", column.getColumnName().getText() );
		}
		else {
			PersistentClass classMapping = configuration().getClassMapping( Customers.class.getName() );
			Column stateColumn = (Column) classMapping.getProperty( "specified_column" ).getColumnIterator().next();
			assertEquals( "CN_specified_column", stateColumn.getName() );
		}
	}

    @Test
    @TestForIssue(jiraKey = "HHH-5848")
    public void testDatabaseTableNames() {
		if ( isMetadataUsed() ) {
			EntityBinding entityBinding = metadata().getEntityBinding( Item.class.getName() );
			assertNotNull( entityBinding );
			AttributeBinding attributeBinding = entityBinding.locateAttributeBinding( "specialPrice" );
			assertNotNull( attributeBinding );
			assertTrue( SingularAttributeBinding.class.isInstance( attributeBinding ) );
			SingularAttributeBinding singularAttributeBinding = (SingularAttributeBinding) attributeBinding;
			assertEquals( 1, singularAttributeBinding.getRelationalValueBindings().size() );
			RelationalValueBinding valueBinding = singularAttributeBinding.getRelationalValueBindings().get( 0 );
			assertFalse( valueBinding.isDerived() );
			Table table = (Table)valueBinding.getTable();

			assertEquals( "TAB_ITEMS_SEC", table.getPhysicalName().getText() );

			singularAttributeBinding = (SingularAttributeBinding)entityBinding.locateAttributeBinding( "price" );
			assertEquals( 1, singularAttributeBinding.getRelationalValueBindings().size() );
			valueBinding = singularAttributeBinding.getRelationalValueBindings().get( 0 );
			assertFalse( valueBinding.isDerived() );
			table = (Table)valueBinding.getTable();
			assertEquals( "TAB_ITEMS",  table.getPhysicalName().getText());

		}
		else {
			PersistentClass classMapping = configuration().getClassMapping( Item.class.getName() );
			Column secTabColumn = (Column) classMapping.getProperty( "specialPrice" ).getColumnIterator().next();
			assertEquals( "TAB_ITEMS_SEC", secTabColumn.getValue().getTable().getName() );
			Column tabColumn = (Column) classMapping.getProperty( "price" ).getColumnIterator().next();
			assertEquals( "TAB_ITEMS", tabColumn.getValue().getTable().getName() );
		}
    }
}
