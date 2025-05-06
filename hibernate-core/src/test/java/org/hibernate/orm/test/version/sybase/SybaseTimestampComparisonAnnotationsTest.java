/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.version.sybase;

import org.hibernate.Session;
import org.hibernate.annotations.Generated;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.generator.EventType;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;
import org.hibernate.type.descriptor.jdbc.VarbinaryJdbcType;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
@RequiresDialect( SybaseASEDialect.class )
public class SybaseTimestampComparisonAnnotationsTest extends BaseCoreFunctionalTestCase {

	@Test
	@JiraKey( value = "HHH-10413" )
	public void testComparableTimestamps() {
		final BasicType<?> versionType = sessionFactory()
				.getMappingMetamodel()
				.getEntityDescriptor(Thing.class.getName()).getVersionType();
		assertTrue( versionType.getJavaTypeDescriptor() instanceof PrimitiveByteArrayJavaType );
		assertTrue( versionType.getJdbcType() instanceof VarbinaryJdbcType );

		Session s = openSession();
		s.getTransaction().begin();
		Thing thing = new Thing();
		thing.name = "n";
		s.persist( thing );
		s.getTransaction().commit();
		s.close();

		byte[] previousVersion = thing.version;
		for ( int i = 0 ; i < 20 ; i++ ) {
			try {
				Thread.sleep(1000);                 //1000 milliseconds is one second.
			} catch(InterruptedException ex) {
				Thread.currentThread().interrupt();
			}

			s = openSession();
			s.getTransaction().begin();
			thing.name = "n" + i;
			thing = (Thing) s.merge( thing );
			s.getTransaction().commit();
			s.close();

			assertTrue( versionType.compare( previousVersion, thing.version ) < 0 );
			previousVersion = thing.version;
		}

		s = openSession();
		s.getTransaction().begin();
		s.remove( thing );
		s.getTransaction().commit();
		s.close();
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { Thing.class };
	}

	@Entity
	@Table(name="thing")
	public static class Thing {
		@Id
		private long id;

		@Version
		@Generated(event = { EventType.INSERT,EventType.UPDATE})
		@Column(name = "ver", columnDefinition = "timestamp")
		private byte[] version;

		private String name;

	}

}
