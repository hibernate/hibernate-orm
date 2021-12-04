/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.loading;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.Assertions.assertThat;

public class LoadContainedInDoubleContainingTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Containing.class, OtherContained.class, Contained.class };
	}

	@Test
	public void test() {
		inTransaction( session -> {
			Containing containing = new Containing( 1, "initial" );
			OtherContained otherContained = new OtherContained( 2, "initial" );
			containing.setOtherContained( otherContained );
			otherContained.setContaining( containing );
			session.persist( containing );
			session.persist( otherContained );
		} );

		inTransaction( session -> {
			OtherContained entity = session.load( OtherContained.class, 2 );
			String text = entity.getText();
			assertThat( text ).isEqualTo( "initial" );
		} );
	}

	@Entity(name = Containing.NAME)
	public static class Containing {

		static final String NAME = "Containing";

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

	@Entity(name = Contained.NAME)
	public static class Contained {

		static final String NAME = "Contained";

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

	@Entity(name = OtherContained.NAME)
	public static class OtherContained {

		static final String NAME = "OtherContained";

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
