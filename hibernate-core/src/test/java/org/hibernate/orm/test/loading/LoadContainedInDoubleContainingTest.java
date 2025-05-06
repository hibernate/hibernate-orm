/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.loading;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				LoadContainedInDoubleContainingTest.Containing.class,
				LoadContainedInDoubleContainingTest.OtherContained.class,
				LoadContainedInDoubleContainingTest.Contained.class
		}
)
@SessionFactory
public class LoadContainedInDoubleContainingTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Containing containing = new Containing( 1, "initial" );
			OtherContained otherContained = new OtherContained( 2, "initial" );
			containing.setOtherContained( otherContained );
			otherContained.setContaining( containing );
			session.persist( containing );
			session.persist( otherContained );
		} );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			OtherContained otherContained = session.getReference( OtherContained.class, 2 );
			String text = otherContained.getText();
			assertThat( text ).isEqualTo( "initial" );
		} );
	}

	@Entity(name = "Containing")
	public static class Containing {

		@Id
		private Integer id;
		private String text;

		@OneToOne(mappedBy = "containing")
		private Contained contained;

		// In order to reproduce the error it is strictly necessary that
		// the name of this field has a lexical order grater than the one above:
		// `otherContained` >> `contained`.
		@OneToOne(mappedBy = "containing")
		private OtherContained otherContained;

		public Containing() {
		}

		public Containing(Integer id, String text) {
			this.id = id;
			this.text = text;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		public Contained getContained() {
			return contained;
		}

		public void setContained(Contained contained) {
			this.contained = contained;
		}

		public OtherContained getOtherContained() {
			return otherContained;
		}

		public void setOtherContained(OtherContained otherContained) {
			this.otherContained = otherContained;
		}
	}

	@Entity(name = "Contained")
	public static class Contained {

		@Id
		private Integer id;
		private String text;

		@OneToOne
		private Containing containing;

		public Contained() {
		}

		public Contained(Integer id, String text) {
			this.id = id;
			this.text = text;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		public Containing getContaining() {
			return containing;
		}

		public void setContaining(Containing containing) {
			this.containing = containing;
		}
	}

	@Entity(name = "OtherContained")
	public static class OtherContained {

		@Id
		private Integer id;
		private String text;

		@OneToOne
		private Containing containing;

		public OtherContained() {
		}

		public OtherContained(Integer id, String text) {
			this.id = id;
			this.text = text;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		public Containing getContaining() {
			return containing;
		}

		public void setContaining(Containing containing) {
			this.containing = containing;
		}
	}
}
