package org.hibernate.test.hbm.comment;

import java.io.StringReader;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.jdbc.ReaderInputStream;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.junit.Assert;
import org.junit.Test;

public class ClassCommentTest {

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
		StandardServiceRegistryBuilder serviceRegistryBuilder = new StandardServiceRegistryBuilder()
				.applySetting("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
		MetadataSources metadataSources = new MetadataSources(serviceRegistryBuilder.build());
		metadataSources.addInputStream(new ReaderInputStream(new StringReader(CLASS_COMMENT_HBM_XML)));
		Metadata metadata = metadataSources.buildMetadata();
		PersistentClass pc = metadata.getEntityBinding("org.hibernate.test.hbm.Foo");
		Assert.assertNotNull(pc);
		Table table = pc.getTable();
		Assert.assertNotNull(table);
		Assert.assertEquals("This is class 'Foo' with property 'bar'.", table.getComment());
	}	
	
}
