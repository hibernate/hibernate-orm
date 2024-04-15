/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.jpa.ql;

import java.util.Properties;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.persister.collection.QueryableCollection;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.test.jpa.MapContent;
import org.hibernate.test.jpa.MapOwner;
import org.hibernate.test.jpa.Relationship;
import org.junit.Assert;
import org.junit.Test;

@RequiresDialect(H2Dialect.class)
public class MapKeySubquerySchemaTest extends BaseCoreFunctionalTestCase {

	private static final String CUSTOM_SCHEMA = "CUSTOM_SCHEMA";

	@Test
	@TestForIssue( jiraKey = "HHH-15523")
	public void testMapKeyLoad() {
		final QueryableCollection collectionPersister = (QueryableCollection) sessionFactory().getMetamodel()
				.collectionPersister( MapOwner.class.getName() + ".contents" );
		Assert.assertTrue(
				"Index SQL does not contain the schema name",
				collectionPersister.getIndexFormulas()[0].contains( CUSTOM_SCHEMA + ".MapContent " )

		);
	}

	@Override
	protected void configure(Configuration configuration) {
		final Properties properties = new Properties();
		properties.put( AvailableSettings.DEFAULT_SCHEMA, CUSTOM_SCHEMA );
		configuration.addProperties( properties );
	}

	@Override
	protected String createSecondSchema() {
		return CUSTOM_SCHEMA;
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { MapOwner.class, MapContent.class, Relationship.class};
	}
}
