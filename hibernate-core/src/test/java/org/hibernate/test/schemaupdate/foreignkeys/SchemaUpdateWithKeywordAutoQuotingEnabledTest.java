/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.schemaupdate.foreignkeys;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;
import java.util.TreeMap;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-11061")
public class SchemaUpdateWithKeywordAutoQuotingEnabledTest extends BaseUnitTestCase {
	private StandardServiceRegistry ssr;
	private MetadataImplementor metadata;

	@Before
	public void setUp() throws IOException {
		final StandardServiceRegistryBuilder standardServiceRegistryBuilder = new StandardServiceRegistryBuilder();
		standardServiceRegistryBuilder.applySetting(
				org.hibernate.cfg.AvailableSettings.KEYWORD_AUTO_QUOTING_ENABLED,
				"true"
		);
		ssr = standardServiceRegistryBuilder.build();
		final MetadataSources metadataSources = new MetadataSources( ssr );

		metadataSources.addAnnotatedClass( Match.class );
		metadata = (MetadataImplementor) metadataSources.buildMetadata();
		metadata.validate();

		new SchemaExport().setHaltOnError( true )
				.setFormat( false )
				.createOnly( EnumSet.of( TargetType.DATABASE ), metadata );
	}

	@After
	public void tearsDown() {
		new SchemaExport().setHaltOnError( true )
				.setFormat( false )
				.drop( EnumSet.of( TargetType.DATABASE ), metadata );
		StandardServiceRegistryBuilder.destroy( ssr );
	}

	@Test
	public void testUpdate() {
		new SchemaUpdate().setHaltOnError( true )
				.setFormat( false )
				.execute( EnumSet.of( TargetType.DATABASE ), metadata );
	}

	@Entity(name = "Match")
	@Table(name = "MATCH")
	public static class Match {
		@Id
		private Long id;

		@ElementCollection(fetch = FetchType.EAGER)
		@CollectionTable
		private Map<Integer, Integer> timeline = new TreeMap<>();
	}
}
