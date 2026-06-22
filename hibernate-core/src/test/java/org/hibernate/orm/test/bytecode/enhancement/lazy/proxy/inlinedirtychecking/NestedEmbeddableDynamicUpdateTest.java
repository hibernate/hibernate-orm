/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy.inlinedirtychecking;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.bytecode.internal.BytecodeProviderInitiator;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.bytecode.enhancement.CustomEnhancementContext;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@DomainModel(
		annotatedClasses = NestedEmbeddableDynamicUpdateTest.Account.class
)
@SessionFactory
@BytecodeEnhanced
@CustomEnhancementContext({ NoDirtyCheckEnhancementContext.class, DirtyCheckEnhancementContext.class })
public class NestedEmbeddableDynamicUpdateTest {
	@BeforeAll
	static void beforeAll() {
		String byteCodeProvider = Environment.getProperties().getProperty( AvailableSettings.BYTECODE_PROVIDER );
		assumeFalse( byteCodeProvider != null && !BytecodeProviderInitiator.BYTECODE_PROVIDER_NAME_BYTEBUDDY.equals(
				byteCodeProvider ) );
	}

	@BeforeEach
	void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Account account = new Account();
			account.setId( 1L );
			account.setAaa( "aaa" );
			account.setZzz( "zzz" );
			account.setCoords( new Coords( "label", new Inner( "note" ) ) );
			session.persist( account );
		} );
	}

	@Test
	public void nestedEmbeddableDirtyNameDoesNotHideLaterDirtyScalar(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Account account = session.find( Account.class, 1L );
			account.setAaa( "aaa updated" );
			account.getCoords().setLabel( "label updated" );
			account.getCoords().getInner().setNote( "note updated" );
			account.setZzz( "zzz updated" );
		} );

		scope.inTransaction( session -> {
			final Account account = session.find( Account.class, 1L );
			assertThat( account.getAaa(), is( "aaa updated" ) );
			assertThat( account.getCoords().getLabel(), is( "label updated" ) );
			assertThat( account.getCoords().getInner().getNote(), is( "note updated" ) );
			assertThat( account.getZzz(), is( "zzz updated" ) );
		} );
	}

	@Entity(name = "Account")
	@Table(name = "account")
	@DynamicUpdate
	public static class Account {
		@Id
		private Long id;

		private String aaa;

		@Embedded
		private Coords coords;

		private String zzz;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getAaa() {
			return aaa;
		}

		public void setAaa(String aaa) {
			this.aaa = aaa;
		}

		public Coords getCoords() {
			return coords;
		}

		public void setCoords(Coords coords) {
			this.coords = coords;
		}

		public String getZzz() {
			return zzz;
		}

		public void setZzz(String zzz) {
			this.zzz = zzz;
		}
	}

	@Embeddable
	public static class Coords {
		private String label;

		@Embedded
		private Inner inner;

		public Coords() {
		}

		public Coords(String label, Inner inner) {
			this.label = label;
			this.inner = inner;
		}

		public String getLabel() {
			return label;
		}

		public void setLabel(String label) {
			this.label = label;
		}

		public Inner getInner() {
			return inner;
		}

		public void setInner(Inner inner) {
			this.inner = inner;
		}
	}

	@Embeddable
	public static class Inner {
		private String note;

		public Inner() {
		}

		public Inner(String note) {
			this.note = note;
		}

		public String getNote() {
			return note;
		}

		public void setNote(String note) {
			this.note = note;
		}
	}
}
