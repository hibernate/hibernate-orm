/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.namingstrategy;

import org.hibernate.boot.MetadataBuilder;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Emmanuel Bernard
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class NamingStrategyTest extends BaseNonConfigCoreFunctionalTestCase {
	@Override
	protected void configureMetadataBuilder(MetadataBuilder metadataBuilder) {
		metadataBuilder.applyImplicitNamingStrategy( (ImplicitNamingStrategy) TestNamingStrategy.INSTANCE );
		metadataBuilder.applyPhysicalNamingStrategy( (PhysicalNamingStrategy) TestNamingStrategy.INSTANCE );
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
		PersistentClass classMapping = metadata().getEntityBinding( Customers.class.getName() );
		Column stateColumn = (Column) classMapping.getProperty( "specified_column" ).getColumnIterator().next();
		assertEquals( "CN_specified_column", stateColumn.getName() );
	}

    @Test
    @TestForIssue(jiraKey = "HHH-5848")
    public void testDatabaseTableNames() {
        PersistentClass classMapping = metadata().getEntityBinding( Item.class.getName() );
        Column secTabColumn = (Column) classMapping.getProperty( "specialPrice" ).getColumnIterator().next();
        assertEquals( "TAB_ITEMS_SEC", secTabColumn.getValue().getTable().getName() );
        Column tabColumn = (Column) classMapping.getProperty( "price" ).getColumnIterator().next();
        assertEquals( "TAB_ITEMS", tabColumn.getValue().getTable().getName() );
    }
}
