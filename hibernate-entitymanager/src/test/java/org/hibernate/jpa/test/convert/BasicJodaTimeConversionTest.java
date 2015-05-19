/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.convert;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.AttributeConverter;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Id;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.test.PersistenceUnitDescriptorAdapter;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.converter.AttributeConverterTypeAdapter;

import org.hibernate.testing.TestForIssue;

import org.junit.Test;

import org.joda.time.LocalDate;
import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertEquals;

/**
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-8842" )
public class BasicJodaTimeConversionTest {
	static int callsToConverter = 0;

	public static class JodaLocalDateConverter implements AttributeConverter<LocalDate, Date> {
		public Date convertToDatabaseColumn(LocalDate localDate) {
			callsToConverter++;
			return localDate.toDate();
		}

		public LocalDate convertToEntityAttribute(Date date) {
			callsToConverter++;
			return LocalDate.fromDateFields( date );
		}
	}

	@Entity( name = "TheEntity" )
	public static class TheEntity {
		@Id
		public Integer id;
		@Convert( converter = JodaLocalDateConverter.class )
		public LocalDate theDate;

		public TheEntity() {
		}

		public TheEntity(Integer id, LocalDate theDate) {
			this.id = id;
			this.theDate = theDate;
		}
	}

	@Test
	public void testSimpleConvertUsage() throws MalformedURLException {
		final PersistenceUnitDescriptorAdapter pu = new PersistenceUnitDescriptorAdapter() {
			@Override
			public List<String> getManagedClassNames() {
				return Arrays.asList( TheEntity.class.getName() );
			}
		};

		final Map settings = new HashMap();
		settings.put( AvailableSettings.HBM2DDL_AUTO, "create-drop" );

		EntityManagerFactory emf = Bootstrap.getEntityManagerFactoryBuilder( pu, settings ).build();
		final EntityPersister ep = emf.unwrap( SessionFactoryImplementor.class ).getEntityPersister( TheEntity.class.getName() );
		final Type theDatePropertyType = ep.getPropertyType( "theDate" );
		final AttributeConverterTypeAdapter type = assertTyping( AttributeConverterTypeAdapter.class, theDatePropertyType );
		assertTyping( JodaLocalDateConverter.class, type.getAttributeConverter() );

		try {
			EntityManager em = emf.createEntityManager();
			em.getTransaction().begin();
			em.persist( new TheEntity( 1, new LocalDate() ) );
			em.getTransaction().commit();
			em.close();

			assertEquals( 1, callsToConverter );

			em = emf.createEntityManager();
			em.getTransaction().begin();
			em.find( TheEntity.class, 1 );
			em.getTransaction().commit();
			em.close();

			assertEquals( 2, callsToConverter );

			em = emf.createEntityManager();
			em.getTransaction().begin();
			em.createQuery( "delete TheEntity" ).executeUpdate();
			em.getTransaction().commit();
			em.close();
		}
		finally {
			emf.close();
		}
	}
}
