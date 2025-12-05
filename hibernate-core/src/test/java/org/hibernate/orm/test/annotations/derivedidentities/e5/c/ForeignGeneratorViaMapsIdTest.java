/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.e5.c;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.orm.test.util.SchemaUtil;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author Emmanuel Bernard
 */
@SessionFactory
@DomainModel(
		annotatedClasses = {
				MedicalHistory.class,
				Person.class
		}
)
public class ForeignGeneratorViaMapsIdTest {

	@Test
	public void testForeignGenerator(SessionFactoryScope scope) {
		MetadataImplementor metadata = scope.getMetadataImplementor();

		assertThat( SchemaUtil.isColumnPresent( "MedicalHistory", "patient_id", metadata ) ).isTrue();

		Person e = new Person();
		scope.inTransaction(
				session -> {
					session.persist( e );
					MedicalHistory d = new MedicalHistory();
					d.patient = e;
					session.persist( d );
					session.flush();
					session.clear();
					d = session.find( MedicalHistory.class, e.id );
					assertThat( d.id ).isEqualTo( e.id );
					session.remove( d );
					session.remove( d.patient );
				}
		);
	}
}
