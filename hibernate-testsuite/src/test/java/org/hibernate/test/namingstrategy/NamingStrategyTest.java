package org.hibernate.test.namingstrategy;

import org.hibernate.cfg.Configuration;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;

/**
 * @author Emmanuel Bernard
 */
public class NamingStrategyTest extends FunctionalTestCase {
	public void testCorrectDatabase() {
		PersistentClass classMapping = getCfg().getClassMapping( Customers.class.getName() );
		Column stateColumn = (Column) classMapping.getProperty( "specified_column" ).getColumnIterator().next();
		assertEquals( "CN_specified_column", stateColumn.getName() );
	}

	@Override
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setNamingStrategy( new TestNamingStrategy() );
	}

	public NamingStrategyTest(String string) {
		super( string );
	}

	public String[] getMappings() {
		return new String[] {
				"namingstrategy/Customers.hbm.xml"
		};
	}
}
