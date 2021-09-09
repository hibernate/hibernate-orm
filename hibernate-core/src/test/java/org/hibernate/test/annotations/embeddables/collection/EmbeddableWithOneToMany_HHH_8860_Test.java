/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.embeddables.collection;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToMany;
import javax.persistence.Version;

import org.hibernate.AnnotationException;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-8860")
public class EmbeddableWithOneToMany_HHH_8860_Test
		extends BaseCoreFunctionalTestCase {

	// Add your entities here.
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Data.class,
		};
	}

	protected void buildSessionFactory() {
		try {
			super.buildSessionFactory();
			fail( "Should throw AnnotationException!" );
		}
		catch ( AnnotationException expected ) {
			assertTrue( expected.getMessage().startsWith(
					"@OneToMany, @ManyToMany or @ElementCollection cannot be used inside an @Embeddable that is also contained within an @ElementCollection"
			) );
		}
	}

	@Test
	public void test() {
	}

	@Entity(name = "Data")
	public static class Data {

		@Id
		private String id;

		@Version
		private Integer version;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		@ElementCollection(fetch = FetchType.LAZY)
		@MapKeyColumn(name = "key")
		private Map<String, GenericStringList> stringlist = new TreeMap<>();

	}

	@Embeddable
	public static class GenericStringList {

		@OneToMany(fetch = FetchType.LAZY)
		public List<String> stringList = new LinkedList<>();
	}
}
