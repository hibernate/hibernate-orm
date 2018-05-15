/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.converter;

import java.net.MalformedURLException;
import java.util.Date;
import javax.persistence.AttributeConverter;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.Session;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.converter.AttributeConverterTypeAdapter;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import org.joda.time.LocalDate;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-8842" )
public class BasicJodaTimeConversionTest extends BaseNonConfigCoreFunctionalTestCase {
	static boolean convertToDatabaseColumnCalled = false;
	static boolean convertToEntityAttributeCalled = false;

	private void resetFlags() {
		convertToDatabaseColumnCalled = false;
		convertToEntityAttributeCalled = false;
	}

	public static class JodaLocalDateConverter implements AttributeConverter<LocalDate, Date> {
		public Date convertToDatabaseColumn(LocalDate localDate) {
			convertToDatabaseColumnCalled = true;
			return localDate.toDate();
		}

		public LocalDate convertToEntityAttribute(Date date) {
			convertToEntityAttributeCalled = true;
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

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { TheEntity.class };
	}

	@Test
	public void testSimpleConvertUsage() throws MalformedURLException {
		final EntityPersister ep = sessionFactory().getEntityPersister( TheEntity.class.getName() );
		final Type theDatePropertyType = ep.getPropertyType( "theDate" );
		final AttributeConverterTypeAdapter type = assertTyping( AttributeConverterTypeAdapter.class, theDatePropertyType );
		assertTrue( JodaLocalDateConverter.class.isAssignableFrom( type.getAttributeConverter().getConverterJavaTypeDescriptor().getJavaType() ) );

		resetFlags();

		Session session = openSession();
		session.getTransaction().begin();
		session.persist( new TheEntity( 1, new LocalDate() ) );
		session.getTransaction().commit();
		session.close();

		assertTrue( convertToDatabaseColumnCalled );
		resetFlags();

		session = openSession();
		session.getTransaction().begin();
		session.get( TheEntity.class, 1 );
		session.getTransaction().commit();
		session.close();

		assertTrue( convertToEntityAttributeCalled );
		resetFlags();

		session = openSession();
		session.getTransaction().begin();
		session.createQuery( "delete TheEntity" ).executeUpdate();
		session.getTransaction().commit();
		session.close();
	}
}
