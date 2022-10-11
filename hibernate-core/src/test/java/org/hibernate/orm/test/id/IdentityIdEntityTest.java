/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.id;

import java.util.Date;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Jan Schatteman
 */
@TestForIssue(jiraKey = "HHH-15561")
@RequiresDialect( value = H2Dialect.class )
public class IdentityIdEntityTest {

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> session.createMutationQuery( "delete from id_entity" ).executeUpdate()
		);
	}

	@Test
	@ServiceRegistry(
			settings = { @Setting( name = AvailableSettings.USE_GET_GENERATED_KEYS, value = "false") }
	)
	@DomainModel( annotatedClasses = { IdentityEntity.class } )
	@SessionFactory
	public void testIdentityEntityWithDisabledGetGeneratedKeys(SessionFactoryScope scope) {
		scope.inTransaction(
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
	@ServiceRegistry(
			settings = { @Setting( name = "use_jdbc_metadata_defaults", value = "false") }
	)
	@DomainModel( annotatedClasses = { IdentityEntity.class } )
	@SessionFactory
	public void testIdentityEntityWithDisabledJdbcMetadataDefaults(SessionFactoryScope scope) {
		scope.inTransaction(
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
