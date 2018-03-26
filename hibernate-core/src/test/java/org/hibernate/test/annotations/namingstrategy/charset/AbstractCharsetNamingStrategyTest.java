/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

// $Id$
package org.hibernate.test.annotations.namingstrategy.charset;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.testing.ServiceRegistryBuilder;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.test.annotations.namingstrategy.LongIdentifierNamingStrategy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue( jiraKey = "HHH-12357" )
public abstract class AbstractCharsetNamingStrategyTest extends BaseUnitTestCase {

	protected ServiceRegistry serviceRegistry;

	@Before
    public void setUp() {
		Map<Object, Object> properties = new HashMap<>( Environment.getProperties() );
		properties.put( AvailableSettings.HBM2DDL_CHARSET_NAME, charsetName() );
		serviceRegistry = ServiceRegistryBuilder.buildServiceRegistry( properties );
	}

	protected abstract String charsetName();

	@After
    public void tearDown() {
        if ( serviceRegistry != null ) {
			ServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}
    @Test
	public void testWithCustomNamingStrategy() throws Exception {
		Metadata metadata = new MetadataSources( serviceRegistry )
				.addAnnotatedClass(Address.class)
				.addAnnotatedClass(Person.class)
				.getMetadataBuilder()
				.applyImplicitNamingStrategy( new LongIdentifierNamingStrategy() )
				.build();

		UniqueKey uniqueKey = metadata.getEntityBinding( Address.class.getName()).getTable().getUniqueKeyIterator().next();
		assertEquals( expectedUniqueKeyName(), uniqueKey.getName() );

		org.hibernate.mapping.ForeignKey foreignKey =
				(org.hibernate.mapping.ForeignKey) metadata.getEntityBinding( Address.class.getName()).getTable().getForeignKeyIterator().next();
		assertEquals( expectedForeignKeyName(), foreignKey.getName() );

		org.hibernate.mapping.Index index = metadata.getEntityBinding( Address.class.getName()).getTable().getIndexIterator().next();
		assertEquals( expectedIndexName(), index.getName() );
	}

	protected abstract String expectedUniqueKeyName();

	protected abstract String expectedForeignKeyName();

	protected abstract String expectedIndexName();

	@Entity(name = "Address")
	@Table(uniqueConstraints = @UniqueConstraint(
			columnNames = {
					"city", "stradă"
			}),
			indexes = @Index( columnList = "city, stradă")
	)
	public class Address {

		@Id
		private Long id;

		private String city;

		private String stradă;

		@ManyToOne
		private Person personă;
	}

	@Entity(name = "Person")
	public class Person {

		@Id
		private long id;

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}
	}
}
