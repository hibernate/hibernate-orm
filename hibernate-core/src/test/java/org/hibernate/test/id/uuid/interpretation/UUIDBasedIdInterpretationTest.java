/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.id.uuid.interpretation;

import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MySQL5Dialect;
import org.hibernate.dialect.PostgreSQL94Dialect;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.tool.schema.Action;
import org.hibernate.type.PostgresUUIDType;
import org.hibernate.type.Type;
import org.hibernate.type.UUIDBinaryType;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;

/**
 * @author Steve Ebersole
 */
public class UUIDBasedIdInterpretationTest extends BaseUnitTestCase {

	@Test
	@TestForIssue( jiraKey = "HHH-10564")
	public void testH2() {
		StandardServiceRegistry ssr = buildStandardServiceRegistry( H2Dialect.class );
		try {
			checkUuidTypeUsed( ssr, UUIDBinaryType.class );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	private StandardServiceRegistry buildStandardServiceRegistry(Class<? extends Dialect> dialectClass) {
		return buildStandardServiceRegistry( dialectClass, false );
	}

	private StandardServiceRegistry buildStandardServiceRegistry(Class<? extends Dialect> dialectClass, boolean exportSchema) {
		final StandardServiceRegistryBuilder ssrb = new StandardServiceRegistryBuilder();
		ssrb.applySetting( AvailableSettings.DIALECT, dialectClass );
		if ( exportSchema ) {
			ssrb.applySetting( AvailableSettings.HBM2DDL_AUTO, Action.CREATE_DROP );
		}
		return ssrb.build();
	}

	private void checkUuidTypeUsed(StandardServiceRegistry ssr, Class<? extends Type> uuidTypeClass) {
		final Metadata metadata = new MetadataSources( ssr )
				.addAnnotatedClass( UuidIdEntity.class )
				.buildMetadata();
		final PersistentClass entityBinding = metadata.getEntityBinding( UuidIdEntity.class.getName() );
		final Type idPropertyType = entityBinding.getIdentifier().getType();
		assertThat( idPropertyType, instanceOf( uuidTypeClass ) );
	}

	@Test
	@TestForIssue( jiraKey = "HHH-10564")
	public void testMySQL() {
		StandardServiceRegistry ssr = buildStandardServiceRegistry( MySQL5Dialect.class );
		try {
			checkUuidTypeUsed( ssr, UUIDBinaryType.class );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-10564")
	public void testPostgreSQL() {
		StandardServiceRegistry ssr = buildStandardServiceRegistry( PostgreSQL94Dialect.class );
		try {
			checkUuidTypeUsed( ssr, PostgresUUIDType.class );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-10564")
	@RequiresDialect(H2Dialect.class)
	public void testBinaryRuntimeUsage() {
		StandardServiceRegistry ssr = buildStandardServiceRegistry( H2Dialect.class, true );
		try {
			final Metadata metadata = new MetadataSources( ssr )
					.addAnnotatedClass( UuidIdEntity.class )
					.buildMetadata();
			final SessionFactory sf = metadata.buildSessionFactory();
			try {
				Session s = sf.openSession();
				try {
					s.beginTransaction();
					s.byId( UuidIdEntity.class ).load( UUID.randomUUID() );
					s.getTransaction().commit();
				}
				finally {
					s.close();
				}
			}
			finally {
				sf.close();
			}
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Entity(name = "UuidIdEntity")
	@Table(name = "UUID_ID_ENTITY")
	public static class UuidIdEntity {
		@Id
		@GeneratedValue
		private UUID id;
	}
}
