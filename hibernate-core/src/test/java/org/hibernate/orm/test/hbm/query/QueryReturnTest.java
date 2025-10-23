/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hbm.query;

import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.ReaderInputStream;
import org.hibernate.query.internal.ResultSetMappingResolutionContext;
import org.hibernate.query.named.NamedResultSetMappingMemento;
import org.hibernate.query.results.ResultSetMapping;
import org.hibernate.query.results.internal.ResultSetMappingImpl;
import org.hibernate.query.results.internal.complete.CompleteResultBuilderEntityValued;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Koen Aers
 */
@JiraKey(value = "HHH-10405")
@RequiresDialect(H2Dialect.class)
@BaseUnitTest
public class QueryReturnTest {

	private static final String QUERY_RETURN_HBM_XML =
			"<hibernate-mapping package='org.hibernate.orm.test.hbm.query'>          " +
			"    <class name='QueryReturnTest$Bar'>                                  " +
			"        <id name='id'>                                                  " +
			"            <generator class='sequence'/>                               " +
			"        </id>                                                           " +
			"        <property name='foo' type='string'/>                            " +
			"    </class>                                                            " +
			"    <sql-query name='myQuery'>                                          " +
			"        <synchronize table='myTable'/>                                  " +
			"        <return                                                         " +
			"                alias='e'                                               " +
			"                class='org.hibernate.orm.test.hbm.query.QueryReturnTest$Bar'" +
			"        />                                                              " +
			"        <![CDATA[from elephant as {e} where {e.age} > 50]]>             " +
			"    </sql-query>                                                        " +
			"</hibernate-mapping>                                                    ";

	@Test
	public void testQueryReturn() {
		Configuration cfg = new Configuration();
		cfg.setProperty( "hibernate.temp.use_jdbc_metadata_defaults", false );
		cfg.addInputStream( new ReaderInputStream( new StringReader( QUERY_RETURN_HBM_XML ) ) );
		ServiceRegistryUtil.applySettings( cfg.getStandardServiceRegistryBuilder() );
		SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor) cfg.buildSessionFactory();
		try {
			NamedResultSetMappingMemento mappingMemento = sessionFactory.getQueryEngine()
					.getNamedObjectRepository()
					.getResultSetMappingMemento( "myQuery" );
			assertThat( mappingMemento ).isNotNull();

			// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
			// NYI

			final ResultSetMapping mapping = new ResultSetMappingImpl( "myQuery" );
			final ResultSetMappingResolutionContext resolutionContext = new ResultSetMappingResolutionContext() {
				@Override
				public SessionFactoryImplementor getSessionFactory() {
					return sessionFactory;
				}
			};

			mappingMemento.resolve( mapping, querySpace -> {
			}, resolutionContext );
			assertThat( mapping.getNumberOfResultBuilders() ).isEqualTo( 1 );
			mapping.visitResultBuilders(
					(i, resultBuilder) -> {
						assertThat( resultBuilder ).isInstanceOf( CompleteResultBuilderEntityValued.class );
						CompleteResultBuilderEntityValued myQueryRootReturn = (CompleteResultBuilderEntityValued) resultBuilder;
//						Assert.assertEquals( "e", myQueryRootReturn.getTableAlias() );
						assertThat( myQueryRootReturn.getReferencedPart().getEntityName() )
								.isEqualTo( "org.hibernate.orm.test.hbm.query.QueryReturnTest$Bar" );
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

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getFoo() {
			return foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}
	}

}
