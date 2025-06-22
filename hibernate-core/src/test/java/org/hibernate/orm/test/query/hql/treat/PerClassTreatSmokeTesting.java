/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql.treat;

import java.util.List;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.query.sqm.StrictJpaComplianceViolation;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry(
		settings = @Setting( name = AvailableSettings.JPA_QUERY_COMPLIANCE, value = "true" )
)
@DomainModel( annotatedClasses = { PerClassTreatSmokeTesting.Volume.class, PerClassTreatSmokeTesting.ExtendedVolume.class } )
@SessionFactory
public class PerClassTreatSmokeTesting {

	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		final Volume volume = new Volume( 1, "abc" );
		final ExtendedVolume extendedVolume = new ExtendedVolume( 2, "def", "noneya" );

		scope.inTransaction(
				(session) -> {
					session.persist( volume );
					session.persist( extendedVolume );
				}
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}
	@Test
	public void simpleImplicitTreatTest(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final String qryString = "select v " +
							"from Volume v " +
							"where v.strategy = 'noneya'";
					try {
						session.createQuery( qryString, Volume.class ).list();
						fail( "This should fail with strict compliance enabled" );
					}
					catch (IllegalArgumentException e) {
						assertThat( e.getCause() ).isInstanceOf( StrictJpaComplianceViolation.class );
						final StrictJpaComplianceViolation violation = (StrictJpaComplianceViolation) e.getCause();
						assertThat( violation.getType() ).isEqualTo( StrictJpaComplianceViolation.Type.IMPLICIT_TREAT );
					}
				}
		);
	}

	@Test
	public void simpleTreatedRootTest(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> {
					final List<Volume> results = session
							.createQuery( "select v from Volume v where treat(v as ExtendedVolume).strategy = 'noneya'", Volume.class )
							.list();
					assertThat( results ).hasSize( 1 );
				}
		);
	}

	@Entity( name = "Volume" )
	@Table( name = "treated_volume" )
	@Inheritance( strategy = InheritanceType.TABLE_PER_CLASS )
	public static class Volume {
		@Id
		private Integer id;
		private String label;

		public Volume() {
		}

		public Volume(Integer id, String label) {
			this.id = id;
			this.label = label;
		}
	}

	@Entity( name = "ExtendedVolume" )
	@Table( name = "treated_extended_volume" )
	public static class ExtendedVolume extends Volume {
		private String strategy;

		public ExtendedVolume() {
		}

		public ExtendedVolume(Integer id, String label, String strategy) {
			super( id, label );
			this.strategy = strategy;
		}
	}


}
