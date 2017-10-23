package org.hibernate.test.hbm.query;

import java.io.StringReader;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.jdbc.ReaderInputStream;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

/**
 * @author Koen Aers
 */
@TestForIssue( jiraKey = "HHH-10223" )
public class NamedQueryTest extends BaseUnitTestCase {
	
	private static String NAMED_QUERY_HBM_XML =
		"<hibernate-mapping package='org.hibernate.test.hbm.query'> "+
	    "	<class name='NamedQueryTest$Bar'>                   "+
		"		<id name='id'>                                  "+
	    "			<generator class='sequence'/>               "+
	    "		</id>                                           "+
	    "		<query name='findByFoo'>                       "+
	    "			<query-param name='foo' type='string'/>     "+
	    "			from NamedQueryTest$Bar where foo like :foo "+
	    "		</query>                                        "+    	
		"	</class>                                            "+
		"</hibernate-mapping>                                   ";

	@Test
	public void testQuery() {
		Configuration cfg = new Configuration();		
		cfg.setProperty("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
		cfg.addInputStream(new ReaderInputStream(new StringReader(NAMED_QUERY_HBM_XML)));
		SessionFactory sessionFactory = cfg.buildSessionFactory();
		sessionFactory.close();
	}
	
	public class Bar {
		private Integer id;		
		private String foo;
		public Integer getId() { return id; }		
		public void setId(Integer id) { this.id = id; }
		public String getFoo() { return foo; }		
		public void setFoo(String foo) { this.foo = foo; }
	}

}
