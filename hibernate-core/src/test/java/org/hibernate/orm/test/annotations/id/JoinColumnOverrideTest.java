/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.id;

import java.util.List;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.tool.schema.internal.SchemaCreatorImpl;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;

import org.hibernate.orm.test.annotations.id.entities.Bunny;
import org.hibernate.orm.test.annotations.id.entities.PointyTooth;
import org.hibernate.orm.test.annotations.id.entities.TwinkleToes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for JIRA issue ANN-748.
 *
 * @author Hardy Ferentschik
 */
@BaseUnitTest
public class JoinColumnOverrideTest {

	private static final String expectedSqlPointyTooth = "create table PointyTooth (bunny_id numeric(128,0), " +
			"id numeric(128,0) not null, primary key (id))";
	private static final String expectedSqlTwinkleToes = "create table TwinkleToes (bunny_id numeric(128,0), " +
			"id numeric(128,0) not null, primary key (id))";

	@Test
	@JiraKey(value = "ANN-748")
	public void testBlownPrecision() {
		StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder()
				.applySetting( AvailableSettings.DIALECT, "SQLServer" )
				.build();

		try {
			MetadataImplementor metadata = (MetadataImplementor) new MetadataSources( ssr )
					.addAnnotatedClass( Bunny.class )
					.addAnnotatedClass( PointyTooth.class )
					.addAnnotatedClass( TwinkleToes.class )
					.buildMetadata();
			metadata.orderColumns( true );
			metadata.validate();

			boolean foundPointyToothCreate = false;
			boolean foundTwinkleToesCreate = false;

			List<String> commands = new SchemaCreatorImpl( ssr ).generateCreationCommands( metadata, false );
			for ( String command : commands ) {

				if ( expectedSqlPointyTooth.equals( command ) ) {
					foundPointyToothCreate = true;
				}
				else if ( expectedSqlTwinkleToes.equals( command ) ) {
					foundTwinkleToesCreate = true;
				}
			}

			assertTrue( foundPointyToothCreate, "Expected create table command for PointyTooth entity not found" );
			assertTrue( foundTwinkleToesCreate, "Expected create table command for TwinkleToes entity not found" );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}
}
