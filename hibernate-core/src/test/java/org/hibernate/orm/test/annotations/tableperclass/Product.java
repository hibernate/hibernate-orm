/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.tableperclass;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table( name = "xPM_Product", uniqueConstraints = {@UniqueConstraint( columnNames = {
		"manufacturerPartNumber", "manufacturerId"} )} )
public class Product extends Component {
}
