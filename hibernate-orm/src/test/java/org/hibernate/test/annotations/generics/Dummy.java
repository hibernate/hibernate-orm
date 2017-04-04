/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$
package org.hibernate.test.annotations.generics;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Dummy<K> {

	@Id
	private Long id;

	@Transient
	transient private K dummyField;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public K getDummyField() {
		return dummyField;
	}

	public void setDummyField(K dummyField) {
		this.dummyField = dummyField;
	}

} 
