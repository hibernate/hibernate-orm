package org.hibernate.orm.test.annotations.namingstrategy;

import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.annotations.NaturalId;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyJpaCompliantImpl;
import org.hibernate.boot.model.naming.ImplicitUniqueKeyNameSource;
import org.hibernate.mapping.UniqueKey;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.junit.Assert.assertEquals;

@JiraKey("HHH-17133")
public class NaturalIdNamingStrategyTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { City.class };
	}

	@Test
	public void testNaturalIdUsesNamingStrategy() {
		Metadata metadata = new MetadataSources( serviceRegistry() )
				.addAnnotatedClasses( getAnnotatedClasses() )
				.getMetadataBuilder()
				.applyImplicitNamingStrategy( new UniqueKeyImplicitNamingStrategy() )
				.build();

		Map<String, UniqueKey> uniqueKeys = metadata.getEntityBinding( City.class.getName() )
				.getTable()
				.getUniqueKeys();

		// The unique key should use the naming strategy.
		UniqueKey uniqueKey = uniqueKeys.values().iterator().next();
		assertEquals( "City_UK_zipCode_city", uniqueKey.getName() );
	}

	@Entity(name = "City")
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

	public static class UniqueKeyImplicitNamingStrategy extends ImplicitNamingStrategyJpaCompliantImpl {
		@Override
		public Identifier determineUniqueKeyName(ImplicitUniqueKeyNameSource source) {
			String name = source.getTableName() + "_UK_" + source.getColumnNames().stream().map( Identifier::getText )
					.collect( Collectors.joining( "_" ) );
			return toIdentifier( name, source.getBuildingContext() );
		}
	}
}
