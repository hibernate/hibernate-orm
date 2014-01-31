package org.hibernate.test.annotations.override;

import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.mapping.Table;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.test.util.SchemaUtil;
import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
@RequiresDialect({ H2Dialect.class })
@TestForIssue(jiraKey = "HHH-6662")
@FailureExpectedWithNewMetamodel
public class AssociationOverrideSchemaTest extends BaseCoreFunctionalTestCase {
	public static final String SCHEMA_NAME = "OTHER_SCHEMA";
	public static final String TABLE_NAME = "BLOG_TAGS";
	public static final String ID_COLUMN_NAME = "BLOG_ID";
	public static final String VALUE_COLUMN_NAME = "BLOG_TAG";

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Entry.class, BlogEntry.class };
	}

	@Override
	protected String createSecondSchema() {
		return SCHEMA_NAME;
	}

	@Test
	public void testJoinTableSchemaName() {
		TableSpecification table = SchemaUtil.getTable( TABLE_NAME, metadata() );
		Assert.assertNotNull( table );
		Assert.assertEquals( SCHEMA_NAME, table.getSchema().getName().getSchema().getText());
	}

	@Test
	public void testJoinTableJoinColumnName() {
		Assert.assertTrue( SchemaUtil.isColumnPresent( TABLE_NAME, ID_COLUMN_NAME, metadata() ) );
	}

	@Test
	public void testJoinTableColumnName() {
		Assert.assertTrue( SchemaUtil.isColumnPresent( TABLE_NAME, VALUE_COLUMN_NAME, metadata() ) );
	}
}
