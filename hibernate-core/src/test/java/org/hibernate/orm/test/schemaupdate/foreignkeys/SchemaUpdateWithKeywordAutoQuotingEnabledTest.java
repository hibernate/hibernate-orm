/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.schemaupdate.foreignkeys;

import java.util.EnumSet;
import java.util.Map;
import java.util.TreeMap;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.hibernate.tool.schema.TargetType;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Andrea Boriero
 */
@JiraKey(value = "HHH-11061")
public class SchemaUpdateWithKeywordAutoQuotingEnabledTest extends BaseUnitTestCase {
	private StandardServiceRegistry ssr;
	private MetadataImplementor metadata;

	@Before
	public void setUp() {
		ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( org.hibernate.cfg.AvailableSettings.KEYWORD_AUTO_QUOTING_ENABLED, "true" )
				.build();

		final MetadataSources metadataSources = new MetadataSources( ssr );
		metadataSources.addAnnotatedClass( Match.class );
		metadata = (MetadataImplementor) metadataSources.buildMetadata();
		metadata.orderColumns( false );
		metadata.validate();
		try {
			createSchema();
		}
		catch (Exception e) {
			tearDown();
			throw e;
		}
	}

	@After
	public void tearDown() {
		dropSchema();
		StandardServiceRegistryBuilder.destroy( ssr );
	}

	@Test
	public void testUpdate() {
		new SchemaUpdate().setHaltOnError( true )
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

	private void createSchema() {
		dropSchema();
		new SchemaExport().setHaltOnError( true )
				.createOnly( EnumSet.of( TargetType.DATABASE ), metadata );
	}

	private void dropSchema() {
		new SchemaExport()
				.drop( EnumSet.of( TargetType.DATABASE ), metadata );
	}
}
