package org.hibernate.test.hbm.query;

import java.io.StringReader;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.engine.jdbc.ReaderInputStream;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryReturn;
import org.hibernate.engine.query.spi.sql.NativeSQLQueryRootReturn;
import org.hibernate.engine.spi.NamedSQLQueryDefinition;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Assert;
import org.junit.Test;


/**
 * @author Koen Aers
 */
@TestForIssue( jiraKey = "HHH-10405" )
public class QueryReturnTest extends BaseUnitTestCase {
	
	private static String QUERY_RETURN_HBM_XML =
			"<hibernate-mapping package='org.hibernate.test.hbm.query'>              "+
		    "    <class name='QueryReturnTest$Bar'>                                  "+
			"        <id name='id'>                                                  "+
		    "            <generator class='sequence'/>                               "+
		    "        </id>                                                           "+
		    "        <property name='foo' type='string'/>                            "+
			"    </class>                                                            "+
		    "    <sql-query name='myQuery'>                                          "+
			"        <synchronize table='myTable'/>                                  "+ 
		    "        <return                                                         "+
			"                alias='e'                                               "+
		    "                class='org.hibernate.test.hbm.query.QueryReturnTest$Bar'"+
			"        />                                                              "+
		    "        <![CDATA[from elephant as {e} where {e.age} > 50]]>             "+
		    "    </sql-query>                                                        "+    	
			"</hibernate-mapping>                                                    ";

	@Test
	public void testQueryReturn() {
		StandardServiceRegistryBuilder serviceRegistryBuilder = new StandardServiceRegistryBuilder()
			.applySetting("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
		StandardServiceRegistry standardServiceRegistry = serviceRegistryBuilder.build();
		MetadataSources metadataSources = new MetadataSources(standardServiceRegistry);
		try {
			metadataSources.addInputStream(new ReaderInputStream(new StringReader(QUERY_RETURN_HBM_XML)));
			Metadata metadata = metadataSources.buildMetadata();
			NamedSQLQueryDefinition myQuery = metadata.getNamedNativeQueryDefinition("myQuery");
			Assert.assertNotNull(myQuery);
			NativeSQLQueryReturn[] myQueryReturns = myQuery.getQueryReturns();
			Assert.assertNotNull(myQueryReturns);
			Assert.assertEquals(1, myQueryReturns.length);
			Assert.assertTrue(NativeSQLQueryRootReturn.class.isInstance(myQueryReturns[0]));
			NativeSQLQueryRootReturn myQueryRootReturn = (NativeSQLQueryRootReturn)myQueryReturns[0];
			Assert.assertEquals("e", myQueryRootReturn.getAlias());
			Assert.assertEquals("org.hibernate.test.hbm.query.QueryReturnTest$Bar", myQueryRootReturn.getReturnEntityName());
		}
		finally {
			if ( standardServiceRegistry instanceof StandardServiceRegistryImpl ) {
				( (StandardServiceRegistryImpl) standardServiceRegistry ).destroy();
			}
		}
	}

	public class Bar {
		public Integer id;		
		public String foo;
	}

}
