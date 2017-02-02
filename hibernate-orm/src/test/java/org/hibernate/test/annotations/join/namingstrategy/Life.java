/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.join.namingstrategy;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SecondaryTable;
import java.io.Serializable;

/**
 * @author Sergey Vasilyev
 */
@Entity
@SecondaryTable(name = "ExtendedLife")
public class Life implements Serializable {
	@Id
	@GeneratedValue
	public Integer id;

	public int duration;
	@Column(table = "ExtendedLife")
	public String fullDescription;

	@ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
	@JoinColumn(name = "CAT_ID", table = "ExtendedLife")
	public SimpleCat owner;

}
