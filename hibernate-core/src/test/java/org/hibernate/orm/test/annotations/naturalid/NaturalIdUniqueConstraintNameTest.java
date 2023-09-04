package org.hibernate.orm.test.annotations.naturalid;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.NaturalId;
import org.hibernate.boot.Metadata;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.testing.orm.jpa.PersistenceUnitDescriptorAdapter;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@JiraKey("HHH-17132")
@RequiresDialect(H2Dialect.class)
public class NaturalIdUniqueConstraintNameTest {

//	@Override
//	protected Class<?>[] getAnnotatedClasses() {
//		return new Class<?>[] { City.class };
//	}

	@Test
	public void testNaturalIdUsesUniqueConstraintName(@TempDir Path tempDir) throws Exception {
		PersistenceUnitDescriptor puDescriptor = new PersistenceUnitDescriptorAdapter() {
			@Override
			public List<String> getManagedClassNames() {
				return List.of( City.class.getName() );
			}
		};

		Map<Object, Object> settings = Map.of( AvailableSettings.HBM2DDL_AUTO, "create" );
		EntityManagerFactoryBuilder emfBuilder = Bootstrap.getEntityManagerFactoryBuilder( puDescriptor, settings );

		Path ddlScript = tempDir.resolve( "ddl.sql" );

		try (EntityManagerFactory entityManagerFactory = emfBuilder.build()) {
			// we do not need the entityManagerFactory, but we need to build it in order to demonstrate the issue (HHH-17065)

			Metadata metadata = emfBuilder.metadata();

			new SchemaExport()
					.setHaltOnError( true )
					.setOutputFile( ddlScript.toString() )
					.setFormat( true )
					.create( EnumSet.of( TargetType.SCRIPT ), metadata );
		}

		String ddl = Files.readString( ddlScript );
		assertThat( ddl ).contains( "(zipCode, city)" );

//		Metadata metadata = new MetadataSources( serviceRegistry() )
//				.addAnnotatedClasses( getAnnotatedClasses() )
//				.buildMetadata();
//
//		Map<String, UniqueKey> uniqueKeys = metadata.getEntityBinding( City.class.getName() )
//				.getTable()
//				.getUniqueKeys();
//
//		// The unique key should not be duplicated for NaturalID + UniqueConstraint.
//		assertEquals( 1, uniqueKeys.size() );
//
//		// The unique key should use the name specified in UniqueConstraint.
//		UniqueKey uniqueKey = uniqueKeys.values().iterator().next();
//		assertEquals( "UK_zipCode_city", uniqueKey.getName() );
	}

	@Entity(name = "City")
	@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "zipCode", "city" }))
	public static class City {

		@Id
		private Long id;

		@NaturalId
		private String zipCode;
		@NaturalId
		private String city;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getZipCode() {
			return zipCode;
		}

		public void setZipCode(String zipCode) {
			this.zipCode = zipCode;
		}

		public String getCity() {
			return city;
		}

		public void setCity(String city) {
			this.city = city;
		}
	}
}
