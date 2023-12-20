package org.hibernate.orm.test.annotations.namingstrategy;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.cfg.Environment;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Selectable;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.junit.Assert.assertEquals;

/**
 * Test harness for HHH-17310.
 *
 * @author Anilabha Baral
 */
public class CamelCaseToUnderscoresNamingStrategyTest extends BaseUnitTestCase {

	private ServiceRegistry serviceRegistry;

	@Before
	public void setUp() {
		serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( Environment.getProperties() );
	}

	@After
	public void tearDown() {
		if ( serviceRegistry != null ) {
			ServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@Test
	public void testWithWordWithDigitNamingStrategy() throws Exception {
		Metadata metadata = new MetadataSources( serviceRegistry )
				.addAnnotatedClass( B.class )
				.getMetadataBuilder()
				.applyPhysicalNamingStrategy( new CamelCaseToUnderscoresNamingStrategy() )
				.build();

		PersistentClass entityBinding = metadata.getEntityBinding( B.class.getName() );
		assertEquals(
				"word_with_digit_d1",
				( (Selectable) entityBinding.getProperty( "wordWithDigitD1" ).getSelectables().get( 0 ) ).getText()
		);
		assertEquals(
				"abcd_efgh_i21",
				( (Selectable) entityBinding.getProperty( "AbcdEfghI21" ).getSelectables().get( 0 ) ).getText()
		);
	}

	@Entity
	@Table(name = "ABCD")
	class B implements java.io.Serializable {
		@Id
		protected String AbcdEfghI21;
		protected String wordWithDigitD1;

		@ElementCollection
		protected Set<AddressEntry> address = new HashSet();

		public B() {
		}

		public B(String AbcdEfghI21, String wordWithDigitD1) {
			this.AbcdEfghI21 = AbcdEfghI21;
			this.wordWithDigitD1 = wordWithDigitD1;
		}

		// Default to table A_AddressEntry
		public Set<AddressEntry> getAddress() {
			return address;
		}

		public void setAddress(Set<AddressEntry> addr) {
			this.address = addr;
		}

		public String getId() {
			return AbcdEfghI21;
		}

		public void setId(String AbcdEfghI21) {
			this.AbcdEfghI21 = AbcdEfghI21;
		}

		public String getName() {
			return wordWithDigitD1;
		}

		public void setName(String wordWithDigitD1) {
			this.wordWithDigitD1 = wordWithDigitD1;
		}

	}
}
