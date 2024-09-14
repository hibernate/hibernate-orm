/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.schemaupdate.foreignkeys;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;

/**
 * @author Andrea Boriero
 */
@Entity
@Table(name = "GROUP")
public class Group implements Serializable {
	@Id
	@Column(name = "GROUP_ID")
	private Long id;
}
