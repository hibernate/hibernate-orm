/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.version.sybase;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.Generated;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.generator.EventType;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;
import org.hibernate.type.descriptor.jdbc.VarbinaryJdbcType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Gail Badner
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@RequiresDialect( SybaseASEDialect.class )
@DomainModel(annotatedClasses = SybaseTimestampComparisonAnnotationsTest.Thing.class)
@SessionFactory
public class SybaseTimestampComparisonAnnotationsTest {
	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	@JiraKey( value = "HHH-10413" )
	public void testComparableTimestamps(SessionFactoryScope factoryScope) {
		final BasicType<?> versionType = factoryScope
				.getSessionFactory()
				.getMappingMetamodel()
				.getEntityDescriptor(Thing.class.getName()).getVersionType();
		Assertions.assertInstanceOf( PrimitiveByteArrayJavaType.class, versionType.getJavaTypeDescriptor() );
		Assertions.assertInstanceOf( VarbinaryJdbcType.class, versionType.getJdbcType() );

		final var created = factoryScope.fromTransaction( (s) -> {
			var t = new Thing();
			t.name = "n";
			s.persist( t );
			return t;
		} );

		byte[] previousVersion = created.version;

		try {
			// 2 seconds
			Thread.sleep(2000);
		}
		catch(InterruptedException ex) {
			Thread.currentThread().interrupt();
		}

		var merged = factoryScope.fromTransaction( (s) -> {
			created.name = "x";
			return s.merge( created );
		} );

		Assertions.assertTrue( versionType.compare( previousVersion, merged.version ) < 0 );
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
