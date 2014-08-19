/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.hibernate.test.namingstrategy.regression;

import static org.junit.Assert.assertEquals;

import org.hibernate.cfg.Configuration;
import org.hibernate.mapping.Map;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

public class NamingStrategyTest extends BaseCoreFunctionalTestCase {

	@Override
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setNamingStrategy( new MyNamingStrategy() );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Workflow.class };
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9327")
	public void testNamingStrategyRegression() throws Exception {
		String expectedName = Workflow.class.getName().replaceAll( "\\.", "_" ).toUpperCase();
		expectedName += "_LOCALIZED";

		PersistentClass classMapping = configuration().getClassMapping( Workflow.class.getName() );
		Property property = classMapping.getProperty( "localized" );
		Map map = (Map) property.getValue();
		assertEquals( expectedName, map.getCollectionTable().getName() );
	}
}
