/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hbm.comment;

import java.io.StringReader;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.internal.util.ReaderInputStream;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.Assert;
import org.junit.Test;

@RequiresDialect( H2Dialect.class )
public class ClassCommentTest extends BaseUnitTestCase {

	private static String CLASS_COMMENT_HBM_XML =
		"<hibernate-mapping package='org.hibernate.test.hbm'>                "+
		"    <class name='Foo' subselect='from foo'>                         "+
		"        <comment>This is class 'Foo' with property 'bar'.</comment> "+
		"        <id name='id' type='int'>                                   "+
		"            <generator class='sequence'/>                           "+
		"        </id>                                                       "+
		"        <property name='bar' type='string'/>                        "+
		"    </class>                                                        "+
		"</hibernate-mapping>                                                ";

	@Test
	public void testClassComment() {
		try (StandardServiceRegistry serviceRegistry = ServiceRegistryUtil.serviceRegistry()) {
			MetadataSources metadataSources = new MetadataSources( serviceRegistry );
			metadataSources.addInputStream( new ReaderInputStream( new StringReader( CLASS_COMMENT_HBM_XML ) ) );
			Metadata metadata = metadataSources.buildMetadata();
			PersistentClass pc = metadata.getEntityBinding( "org.hibernate.test.hbm.Foo" );
			Assert.assertNotNull( pc );
			Table table = pc.getTable();
			Assert.assertNotNull( table );
			Assert.assertEquals( "This is class 'Foo' with property 'bar'.", table.getComment() );
		}
	}

}
