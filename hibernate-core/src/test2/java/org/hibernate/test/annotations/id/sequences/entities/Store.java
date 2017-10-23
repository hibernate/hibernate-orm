/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id: Store.java 14760 2008-06-11 07:33:15Z hardy.ferentschik $
package org.hibernate.test.annotations.id.sequences.entities;
import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * Sample of class generator
 *
 * @author Emmanuel Bernard
 */
@Entity
@javax.persistence.SequenceGenerator(
		name = "SEQ_STORE",
		sequenceName = "my_sequence"
)
@SuppressWarnings("serial")
public class Store implements Serializable {
	private Long id;

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_STORE")
	public Long getId() {
		return id;
	}

	public void setId(Long long1) {
		id = long1;
	}
}
