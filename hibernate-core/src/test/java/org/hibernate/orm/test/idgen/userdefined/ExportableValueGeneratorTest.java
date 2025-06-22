/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.idgen.userdefined;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.annotations.ValueGenerationType;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.ExportableProducer;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.generator.OnExecutionGenerator;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.EnumSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Jpa(annotatedClasses = ExportableValueGeneratorTest.WithExportableGenerator.class)
@SkipForDialect( dialectClass = SybaseASEDialect.class )
@SkipForDialect( dialectClass = MySQLDialect.class)
public class ExportableValueGeneratorTest {

	@Test void test(EntityManagerFactoryScope scope) {
		final EntityManagerFactory entityManagerFactory = scope.getEntityManagerFactory();
		final WithExportableGenerator first = new WithExportableGenerator();
		entityManagerFactory.runInTransaction( entityManager -> entityManager.persist( first ) );
		int firstSequenceVal = entityManagerFactory.callInTransaction( entityManager ->
				entityManager.find( WithExportableGenerator.class, first.uuid ).sequenceVal );
		assertEquals( 1, firstSequenceVal );
		final WithExportableGenerator second = new WithExportableGenerator();
		entityManagerFactory.runInTransaction( entityManager -> entityManager.persist( second ) );
		int secondSequenceVal = entityManagerFactory.callInTransaction( entityManager ->
				entityManager.find( WithExportableGenerator.class, second.uuid ).sequenceVal );
		assertEquals( 2, secondSequenceVal );
	}

	@ValueGenerationType(generatedBy = OnExecutionSequenceGenerator.class)
	@Retention(RetentionPolicy.RUNTIME)
	@interface OnExecutionSequence {
		String sequenceName();
	}

	public static class OnExecutionSequenceGenerator implements OnExecutionGenerator, ExportableProducer {

		final String sequenceName;

		public OnExecutionSequenceGenerator(OnExecutionSequence annotation) {
			sequenceName = annotation.sequenceName();
		}

		@Override
		public void registerExportables(Database database) {
			Identifier testseq = Identifier.toIdentifier( sequenceName );
			database.getDefaultNamespace()
					.registerSequence( testseq,
							new Sequence( "OnExecutionSequenceGenerator", null, null, testseq ) );
		}

		@Override
		public boolean referenceColumnsInSql(Dialect dialect) {
			return true;
		}

		@Override
		public boolean writePropertyValue() {
			return false;
		}

		@Override
		public String[] getReferencedColumnValues(Dialect dialect) {
			return new String[] { dialect.getSequenceSupport().getSelectSequenceNextValString( sequenceName ) };
		}

		@Override
		public EnumSet<EventType> getEventTypes() {
			return EventTypeSets.INSERT_ONLY;
		}
	}

	@Entity(name ="WithExportableGenerator")
	static class WithExportableGenerator {
		@Id @GeneratedValue
		UUID uuid;
		@OnExecutionSequence(sequenceName = "exported_sequence")
		int sequenceVal;
	}
}
