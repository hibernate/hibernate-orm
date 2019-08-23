/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.embeddable.generics;

import java.util.Set;

import javax.persistence.Embeddable;

/**
 * @author Chris Cranford
 */
@Embeddable
public class ParentEmbeddable<MyType extends MyTypeInterface> {
	private Set<MyType> fields;

	public Set<MyType> getFields() {
		return fields;
	}

	public void setFields(Set<MyType> fields) {
		this.fields = fields;
	}
}
