/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.fetch;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.Assertions.assertThat;

public class OneToOneOwnerByContainedEagerCyclesTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Containing.class, Contained.class };
	}

	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty( AvailableSettings.MAX_FETCH_DEPTH, 2 );
	}

	@Test
	public void test() {
		inTransaction( session -> {
			Containing containing1 = new Containing();
			containing1.setId( 1 );

			Containing containing2 = new Containing();
			containing2.setId( 2 );

			containing1.setChild( containing2 );
			containing2.setParent( containing1 );

			Containing containing3 = new Containing();
			containing3.setId( 3 );

			containing2.setChild( containing3 );
			containing3.setParent( containing2 );

			Containing containing4 = new Containing();
			containing4.setId( 4 );

			containing3.setChild( containing4 );
			containing4.setParent( containing3 );

			Contained contained1 = new Contained();
			contained1.setId( 5 );
			contained1.setText( "initialValue" );
			containing2.setContained( contained1 );
			contained1.setContaining( containing2 );

			Contained contained2 = new Contained();
			contained2.setId( 6 );
			contained2.setText( "initialOutOfScopeValue" );
			containing3.setContained( contained2 );
			contained2.setContaining( containing3 );

			session.persist( contained1 );
			session.persist( contained2 );
			session.persist( containing3 );
			session.persist( containing4 );
			session.persist( containing2 );
			session.persist( containing1 );
		} );

		// Test updating the value
		inTransaction( session -> {
			Contained contained = session.get( Contained.class, 5 );
			assertThat( contained ).isNotNull();
			final Containing containing2 = contained.containing;
			assertThat( containing2 ).isNotNull();
			final Containing containing3 = containing2.child;
			assertThat( containing3 ).isNotNull();
			assertThat( containing3.parent ).isEqualTo( containing2 );
			assertThat( containing3.child ).isNotNull();
		} );
	}

	@Entity(name = "Containing")
	public static class Containing {

		@Id
		private Integer id;

		@OneToOne
		private Containing parent;

		@OneToOne(mappedBy = "parent")
		private Containing child;

		@OneToOne(mappedBy = "containing")
		private Contained contained;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Containing getParent() {
			return parent;
		}

		public void setParent(Containing parent) {
			this.parent = parent;
		}

		public Containing getChild() {
			return child;
		}

		public void setChild(Containing child) {
			this.child = child;
		}

		public Contained getContained() {
			return contained;
		}

		public void setContained(Contained contained) {
			this.contained = contained;
		}
	}

	@Entity(name = "Contained")
	public static class Contained {
		@Id
		private Integer id;

		@OneToOne
		private Containing containing;

		private String text;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Containing getContaining() {
			return containing;
		}

		public void setContaining(Containing containing) {
			this.containing = containing;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}
}
