/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.id;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.fail;


/**
 * @author Jan Schatteman
 */
@TestForIssue(jiraKey = "HHH-15561")
@RequiresDialect( value = { H2Dialect.class } )
public class IdentityIdEntityTest extends BaseUnitTestCase {

	@Test
	public void testIdentityEntityWithDisabledGetGeneratedKeys() {
		StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.HBM2DDL_AUTO, "create-drop" )
				.applySetting( AvailableSettings.USE_GET_GENERATED_KEYS, "false" )
				.build();

		Metadata metadata = new MetadataSources( serviceRegistry )
				.addAnnotatedClass( IdentityEntity.class )
				.buildMetadata();

		SessionFactory sessionFactory = metadata.getSessionFactoryBuilder()
				.build();

		doInHibernate(
				() -> sessionFactory,
				session -> {
					try {
						IdentityEntity ie = new IdentityEntity();
						ie.setTimestamp( new Date() );
						session.persist( ie );
					}
					catch (Exception e) {
						fail( "Creation of an IDENTITY-id-based entity failed when \"hibernate.jdbc.use_get_generated_keys\" was set to false (" + e.getMessage() + ")" );
					}
				}
		);
	}

	@Test
	public void testIdentityEntityWithDisabledJdbcMetadataDefaults() {
		StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
				.applySetting( AvailableSettings.HBM2DDL_AUTO, "create-drop" )
				.applySetting( "use_jdbc_metadata_defaults", "false" )
				.build();

		Metadata metadata = new MetadataSources( serviceRegistry )
				.addAnnotatedClass( IdentityEntity.class )
				.buildMetadata();

		SessionFactory sessionFactory = metadata.getSessionFactoryBuilder()
				.build();

		doInHibernate(
				() -> sessionFactory,
				session -> {
					try {
						IdentityEntity ie = new IdentityEntity();
						ie.setTimestamp( new Date() );
						session.persist( ie );
					}
					catch (Exception e) {
						fail( "Creation of an IDENTITY-id-based entity failed when \"use_jdbc_metadata_defaults\" was set to false (" + e.getMessage() + ")" );
					}
				}
		);
	}

	@Entity(name = "id_entity")
	public static class IdentityEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private int id;

		private Date timestamp;

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public Date getTimestamp() {
			return timestamp;
		}

		public void setTimestamp(Date timestamp) {
			this.timestamp = timestamp;
		}
	}

}
