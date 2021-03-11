/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.idgen.enhanced.auto;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.id.enhanced.DatabaseStructure;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.id.factory.internal.DefaultIdentifierGeneratorFactory;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import org.hibernate.tool.schema.Action;

import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Steve Ebersole
 */
public class NewGeneratorsTests extends BaseUnitTestCase {
	@Test
	@FailureExpected(
			jiraKey = "HHH-14491",
			message = "To be implemented for 6; see https://github.com/hibernate/hibernate-orm/discussions/3809"
	)
	public void testAutoDefaults() {
		// check that `@GeneratedValue(AUTO)` with no explicit generator applies
		// various defaults:
		// 		- allocation/increment size = 50
		//		- sequence-per-entity with a suffix

		final StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder();
		ssrb.applySetting( AvailableSettings.HBM2DDL_AUTO, Action.CREATE_DROP );

		final StandardServiceRegistry ssr = ssrb.build();
		final Metadata metadata = new MetadataSources( ssr )
				.addAnnotatedClass( Entity1.class )
				.buildMetadata();

		final DefaultIdentifierGeneratorFactory generatorFactory = new DefaultIdentifierGeneratorFactory();
		generatorFactory.injectServices( (ServiceRegistryImplementor) ssr );

		final PersistentClass entityBinding = metadata.getEntityBinding( Entity1.class.getName() );
		final SequenceStyleGenerator generator = (SequenceStyleGenerator) entityBinding.getRootClass().getIdentifier().createIdentifierGenerator(
				generatorFactory,
				new H2Dialect(),
				"",
				"",
				entityBinding.getRootClass()
		);

		final DatabaseStructure databaseStructure = generator.getDatabaseStructure();

		// HHH-14491 - what we want to happen
		assertThat( databaseStructure.getName(), is( "Entity1_SEQ" ) );
		// or this depending on the discussion (Jira) about using entity name v. table name as the base
		assertThat( databaseStructure.getName(), is( "tbl_1_SEQ" ) );

		// HHH-14491 - this is what we want to have happen
		assertThat( databaseStructure.getIncrementSize(), is( 50 ) );
	}

	@Entity( name = "Entity1" )
	@Table( name = "tbl_1" )
	public static class Entity1 {
		@Id
		@GeneratedValue( strategy = GenerationType.AUTO )
		private Integer id;
	}
}
