/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.id.usertype;

import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PersistenceException;
import javax.persistence.Table;

import org.hibernate.HibernateException;
import org.hibernate.annotations.TypeDef;
import org.hibernate.boot.spi.MetadataBuilderContributor;
import org.hibernate.dialect.PostgreSQL82Dialect;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.query.NativeQuery;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.util.ExceptionUtil;
import org.hibernate.test.id.usertype.inet.Event;
import org.hibernate.test.id.usertype.inet.Inet;
import org.hibernate.test.id.usertype.inet.InetType;
import org.hibernate.test.id.usertype.inet.PostgreSQLInetTypesOtherTest;
import org.hibernate.test.id.usertype.json.Json;
import org.hibernate.test.id.usertype.json.JsonType;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect(PostgreSQL82Dialect.class)
public class PostgreSQLMultipleTypesOtherContributorTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Event.class
		};
	}

	@Override
	protected void afterEntityManagerFactoryBuilt() {

		doInJPA( this::entityManagerFactory, entityManager -> {
			Event event = new Event();
			event.setId( 1L );
			event.setIp( "192.168.0.123/24" );
			event.setProperties( new Json( "{\"key\": \"temp\", \"value\": \"9C\"}" ) );

			entityManager.persist( event );
		} );
	}

	@Override
	protected void addConfigOptions(Map options) {
		options.put( EntityManagerFactoryBuilderImpl.METADATA_BUILDER_CONTRIBUTOR,
					 (MetadataBuilderContributor) metadataBuilder -> {
						 metadataBuilder.applyBasicType(
								 InetType.INSTANCE, InetType.INSTANCE.getName()
						 );
						 metadataBuilder.applyBasicType(
								 JsonType.INSTANCE, JsonType.INSTANCE.getName()
						 );
					 }
		);
	}

	@Test
	public void testMultipleTypeContributions() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			try {
				List<Inet> inets = entityManager.createNativeQuery(
					"select e.ip " +
					"from Event e " +
					"where e.id = :id" )
				.setParameter( "id", 1L )
				.getResultList();

				assertEquals( 1, inets.size() );
				assertEquals( "192.168.0.123/24", inets.get( 0 ).getAddress() );
			}
			catch (PersistenceException e) {
				Throwable rootException = ExceptionUtil.rootCause( e );
				assertEquals(
						"There are multiple Hibernate types: [inet, json] registered for the [1111] JDBC type code",
						rootException.getMessage()
				);
			}
		} );
	}

	@Test
	public void testMultipleTypeContributionsExplicitBinding() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			List<Inet> inets = entityManager.createNativeQuery(
				"select e.ip " +
				"from Event e " +
				"where e.id = :id" )
			.setParameter( "id", 1L )
			.unwrap( NativeQuery.class )
			.addScalar( "ip", InetType.INSTANCE )
			.getResultList();

			assertEquals( 1, inets.size() );
			assertEquals( "192.168.0.123/24", inets.get( 0 ).getAddress() );
		} );
	}

	@Entity(name = "Event")
	@Table(name = "event")
	@TypeDef(name = "ipv4", typeClass = InetType.class, defaultForType = Inet.class)
	@TypeDef(name = "json", typeClass = JsonType.class, defaultForType = Json.class)
	public class Event {

		@Id
		private Long id;

		@Column(name = "ip", columnDefinition = "inet")
		private Inet ip;

		@Column(name = "properties", columnDefinition = "jsonb")
		private Json properties;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Inet getIp() {
			return ip;
		}

		public void setIp(String address) {
			this.ip = new Inet( address );
		}

		public Json getProperties() {
			return properties;
		}

		public void setProperties(Json properties) {
			this.properties = properties;
		}
	}
}
