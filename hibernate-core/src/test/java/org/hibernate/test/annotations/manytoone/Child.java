/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.manytoone;
import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * TODO: change this sample with an Address -> Country relation. This is more accurate
 *
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "tbl_child")
public class Child implements Serializable {
	@Id
	@GeneratedValue
	public Integer id;

	@ManyToOne()
	@JoinColumns({
	@JoinColumn(name = "parentCivility", referencedColumnName = "isMale"),
	@JoinColumn(name = "parentLastName", referencedColumnName = "lastName"),
	@JoinColumn(name = "parentFirstName", referencedColumnName = "firstName")
			})
	public Parent parent;
}
