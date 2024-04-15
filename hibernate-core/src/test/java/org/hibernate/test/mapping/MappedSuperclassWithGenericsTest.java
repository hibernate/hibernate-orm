/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.mapping;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.MappedSuperclass;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

@TestForIssue(jiraKey = "HHH-14499")
public class MappedSuperclassWithGenericsTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				IntermediateAbstractMapped.class,
				BaseEntity.class,
				AbstractGenericMappedSuperType.class,
		};
	}

	@Test
	public void testIt() {

	}

	@MappedSuperclass
	public static abstract class AbstractGenericMappedSuperType<T> {

		private T whateverType;

	}

	@MappedSuperclass
	@IdClass(PK.class)
	public static abstract class IntermediateAbstractMapped<T> extends AbstractGenericMappedSuperType<T> {

		@Id
		private String keyOne;
		@Id
		private String keyTwo;
		@Id
		private String keyThree;
	}

	@SuppressWarnings("UnusedDeclaration")
	public static class PK implements Serializable {

		private String keyOne;
		private String keyTwo;
		private String keyThree;

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			PK pk = (PK) o;
			return Objects.equals( keyOne, pk.keyOne ) &&
					Objects.equals( keyTwo, pk.keyTwo ) &&
					Objects.equals( keyThree, pk.keyThree );
		}

		@Override
		public int hashCode() {
			return Objects.hash( keyOne, keyTwo, keyThree );
		}
	}

	@Entity(name = "BaseEntity")
	public static class BaseEntity<T> extends IntermediateAbstractMapped<byte[]> {

		String aString;

	}

}
