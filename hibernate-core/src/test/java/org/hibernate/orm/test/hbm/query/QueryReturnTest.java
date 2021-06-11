package org.hibernate.orm.test.hbm.query;

import java.io.StringReader;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.query.NamedResultSetMappingDescriptor;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.registry.internal.StandardServiceRegistryImpl;
import org.hibernate.boot.query.NamedNativeQueryDefinition;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.jdbc.ReaderInputStream;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.internal.ResultSetMappingResolutionContext;
import org.hibernate.query.named.NamedResultSetMappingMemento;
import org.hibernate.query.results.ResultBuilderEntityValued;
import org.hibernate.query.results.ResultSetMapping;
import org.hibernate.query.results.ResultSetMappingImpl;
import org.hibernate.query.results.complete.CompleteResultBuilderEntityValued;

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
			"<hibernate-mapping package='org.hibernate.orm.test.hbm.query'>          "+
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
		    "                class='org.hibernate.orm.test.hbm.query.QueryReturnTest$Bar'"+
			"        />                                                              "+
		    "        <![CDATA[from elephant as {e} where {e.age} > 50]]>             "+
		    "    </sql-query>                                                        "+    	
			"</hibernate-mapping>                                                    ";

	@Test
	public void testQueryReturn() {
		Configuration cfg = new Configuration();
		cfg.setProperty( "hibernate.dialect", "org.hibernate.dialect.HSQLDialect" );
		cfg.setProperty( "hibernate.temp.use_jdbc_metadata_defaults", "false" );
		cfg.addInputStream( new ReaderInputStream( new StringReader( QUERY_RETURN_HBM_XML ) ) );
		SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) cfg.buildSessionFactory();
		try {
			NamedResultSetMappingMemento mappingMemento = sessionFactory.getQueryEngine()
					.getNamedObjectRepository()
					.getResultSetMappingMemento( "myQuery" );
			Assert.assertNotNull( mappingMemento );

			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// NYI

			final ResultSetMapping mapping = new ResultSetMappingImpl( "myQuery" );
			final ResultSetMappingResolutionContext resolutionContext = new ResultSetMappingResolutionContext() {
				@Override
				public SessionFactoryImplementor getSessionFactory() {
					return sessionFactory;
				}
			};

			mappingMemento.resolve( mapping, querySpace -> {}, resolutionContext );
			Assert.assertEquals( 1, mapping.getNumberOfResultBuilders() );
			mapping.visitResultBuilders(
					(i, resultBuilder) -> {
						Assert.assertTrue( resultBuilder instanceof CompleteResultBuilderEntityValued );
						CompleteResultBuilderEntityValued myQueryRootReturn = (CompleteResultBuilderEntityValued) resultBuilder;
//						Assert.assertEquals( "e", myQueryRootReturn.getTableAlias() );
						Assert.assertEquals(
								"org.hibernate.orm.test.hbm.query.QueryReturnTest$Bar",
								myQueryRootReturn.getReferencedPart().getEntityName()
						);
					}
			);
		}
		finally {
			sessionFactory.close();
		}
	}

	public static class Bar {
		public Integer id;		
		public String foo;
		public Integer getId() { return id; }
		public void setId(Integer id) { this.id = id; }
		public String getFoo() { return foo; }
		public void setFoo(String foo) { this.foo = foo; }
	}

}
