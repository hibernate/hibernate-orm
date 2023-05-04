package org.hibernate.orm.test.schemaupdate;

import java.sql.Types;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.jpa.boot.spi.Bootstrap;

import org.hibernate.testing.orm.jpa.PersistenceUnitDescriptorAdapter;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.Table;

import static jakarta.persistence.DiscriminatorType.CHAR;
import static jakarta.persistence.InheritanceType.SINGLE_TABLE;
import static org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@JiraKey("HHH-16551")
public class CreateCharDiscriminatorTest {

	@Test
	@JiraKey("HHH-16551")
	public void testCreateDiscriminatorCharColumnSize() {
		final PersistenceUnitDescriptorAdapter pu = new PersistenceUnitDescriptorAdapter() {
			@Override
			public List<String> getManagedClassNames() {
				return List.of( Parent.class.getName() );
			}
		};


		final var settings = Map.of( HBM2DDL_AUTO, "create-drop" );

		try (var factory = Bootstrap.getEntityManagerFactoryBuilder( pu, settings ).build();
			 var manager = factory.createEntityManager()) {
			manager.unwrap( Session.class ).doWork( conn -> {
				try (var rs = conn.getMetaData().getColumns( null, null, null, null )) {
					if ( rs.next() ) {
						do {
							if ( "parent".equalsIgnoreCase( rs.getString( "TABLE_NAME" ) ) &&
									"discr".equalsIgnoreCase( rs.getString( "COLUMN_NAME" ) ) ) {
								assertEquals( Types.CHAR, rs.getInt( "DATA_TYPE" ) );
								assertEquals( 1, rs.getInt( "COLUMN_SIZE" ) );
							}
						} while ( rs.next() );
					}
					else {
						fail( "Table and/or columns has not been created, why?!?" );
					}
				}
			} );
		}
	}

	@Entity
	@Table(name = "parent")
	@Inheritance(strategy = SINGLE_TABLE)
	@DiscriminatorColumn(name = "discr", discriminatorType = CHAR)
	@DiscriminatorValue("*")
	public static class Parent {

		@Id
		private Integer id;

		@Column
		private String name;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
