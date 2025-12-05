/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hbm.query;

import java.io.StringReader;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.internal.util.ReaderInputStream;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

/**
 * @author Koen Aers
 */
@JiraKey( value = "HHH-10223" )
@RequiresDialect( H2Dialect.class )
@BaseUnitTest
public class NamedQueryTest {

	private static final String NAMED_QUERY_HBM_XML =
		"<hibernate-mapping package='org.hibernate.orm.test.hbm.query'> "+
		"	<class name='NamedQueryTest$Bar'>                   "+
		"		<id name='id'>                                  "+
		"			<generator class='sequence'/>               "+
		"		</id>                                           "+
		"       <property name='foo'/>                          "+
		"		<query name='findByFoo'>                        "+
		"			<query-param name='foo' type='string'/>     "+
		"			from NamedQueryTest$Bar where foo like :foo "+
		"		</query>                                        "+
		"	</class>                                            "+
		"</hibernate-mapping>                                   ";

	@Test
	public void testQuery() {
		Configuration cfg = new Configuration();
		cfg.setProperty( "hibernate.temp.use_jdbc_metadata_defaults", false );
		cfg.addInputStream( new ReaderInputStream( new StringReader( NAMED_QUERY_HBM_XML ) ) );
		ServiceRegistryUtil.applySettings( cfg.getStandardServiceRegistryBuilder() );
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
