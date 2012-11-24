package org.hibernate.test.schemaupdate;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.test.onetomany.Node;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public class SchemaGenerationTest extends BaseCoreFunctionalTestCase {
	@Override
	protected String[] getMappings() {
		return new String[] { "onetomany/Node.hbm.xml" };
	}

	@Test
	@TestForIssue( jiraKey = "HHH-7612" )
	@RequiresDialect( H2Dialect.class )
	public void testSqlCreatePrimaryAndForeignKeyOrder() {
		final Mapping mappings = configuration().buildMapping();
		final PersistentClass persistentClass = configuration().getClassMapping( Node.class.getName() );
		final String sqlCreate = persistentClass.getTable().sqlCreateString( getDialect(), mappings, null, null );
		Assert.assertEquals(
				"PK and FK columns should appear first in CREATE TABLE statement.",
				"create table Node (id integer not null, node_id integer, description varchar(255), idx integer, primary key (id))",
				sqlCreate
		);
	}
}
