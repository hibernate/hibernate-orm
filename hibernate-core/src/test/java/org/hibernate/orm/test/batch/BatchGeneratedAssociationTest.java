/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batch;

import java.io.Serializable;

import org.hibernate.annotations.Generated;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@SessionFactory
@DomainModel( annotatedClasses = {
		BatchGeneratedAssociationTest.Interpretation.class,
		BatchGeneratedAssociationTest.InterpretationData.class,
		BatchGeneratedAssociationTest.InterpretationVersion.class
} )
@ServiceRegistry( settings = @Setting( name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "10" ) )
public class BatchGeneratedAssociationTest {
	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Long interpretationVersion = 111L;

			final Interpretation interpretation = new Interpretation();
			interpretation.uuid = 1L;

			final InterpretationData interpretationData = new InterpretationData();
			interpretationData.interpretationVersion = new InterpretationVersion(
					interpretationVersion,
					interpretation.uuid
			);
			interpretationData.name = "TEST_NAME";
			session.persist( interpretationData );

			interpretation.interpretationData = interpretationData;
			interpretation.interpretationVersion = interpretationVersion;
			session.persist( interpretation );
		} );
	}

	@Entity( name = "Interpretation" )
	@Table( name = "interpretations" )
	public static class Interpretation {
		@Id
		public Long uuid;

		@Column( name = "interpretation_version" )
		public Long interpretationVersion;

		@Column( name = "id" )
		@Generated
		public Long id;

		@OneToOne( fetch = FetchType.LAZY )
		@JoinColumns( {
				@JoinColumn( name = "uuid", referencedColumnName = "interpretation_uuid", insertable = false, updatable = false ),
				@JoinColumn( name = "interpretation_version", referencedColumnName = "interpretation_version", insertable = false, updatable = false )
		} )
		public InterpretationData interpretationData;
	}

	@Embeddable
	public static class InterpretationVersion implements Serializable {
		@Column( name = "interpretation_version", nullable = false, updatable = false )
		public Long version;

		@Column( name = "interpretation_uuid", nullable = false, updatable = false )
		public Long uuid;

		public InterpretationVersion() {
		}

		public InterpretationVersion(Long version, Long uuid) {
			this.version = version;
			this.uuid = uuid;
		}
	}

	@Entity( name = "InterpretationData" )
	@Table( name = "interpretation_data" )
	public static class InterpretationData {
		@EmbeddedId
		public InterpretationVersion interpretationVersion;

		@Column( updatable = false )
		public String name;
	}
}
