/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@DomainModel(
		annotatedClasses = {
				LazyLoadingAndInheritanceTest.Containing.class,
				LazyLoadingAndInheritanceTest.Contained.class,
				LazyLoadingAndInheritanceTest.ContainedExtended.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "false" ),
				@Setting( name = AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, value = "true" ),
		}
)
@SessionFactory
@BytecodeEnhanced
@JiraKey("HHH-15090")
public class LazyLoadingAndInheritanceTest {

	private Long containingID;

	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			Containing containing = new Containing();
			ContainedExtended contained = new ContainedExtended( "George" );
			containing.contained = contained;
			s.persist( contained );
			s.persist( containing );
			containingID = containing.id;
		} );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			Containing containing = s.getReference( Containing.class, containingID );
			Contained contained = containing.contained;
			assertThat( contained ).isNotNull();
			assertThat( Hibernate.isPropertyInitialized( contained, "name" ) ).isFalse();
			assertThat( contained.name ).isNotNull();
		} );
	}

	@Entity(name = "Containing")
	static class Containing {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		public Long id;

		@ManyToOne(fetch = FetchType.LAZY, optional = false)
		public Contained contained;
	}

	@Entity(name = "Contained")
	static class Contained {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		public Long id;

		public String name;

		Contained() {
		}

		Contained(String name) {
			this.name = name;
		}
	}

	@Entity(name = "ContainedExtended")
	static class ContainedExtended extends Contained {

		ContainedExtended() {
		}

		ContainedExtended(String name) {
			this.name = name;
		}

	}
}
