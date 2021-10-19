/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.embeddable;

import jakarta.persistence.Basic;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

public class MappedByEmbeddableTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Containing.class, Embed.class, Contained.class };
	}

	@Test
	public void test() {

	}

	@Entity(name = "containing")
	public static class Containing {

		@Id
		private Integer id;

		@Embedded
		private Embed embed = new Embed();

	}

	@Embeddable
	public static class Embed {

		@OneToOne
		private Contained contained;

	}

	@Entity(name = "contained")
	public static class Contained {

		@Id
		private Integer id;

		@Basic
		private String data;

		@OneToOne(mappedBy = "embed.contained")
		private Containing containing;

	}

}
