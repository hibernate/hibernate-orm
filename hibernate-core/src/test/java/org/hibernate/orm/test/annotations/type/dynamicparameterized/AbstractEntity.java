/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.type.dynamicparameterized;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 * @author Daniel Gredler
 */
@MappedSuperclass
public abstract class AbstractEntity {

	@Id
	@Temporal(TemporalType.DATE)
	@Column(name = "ID")
	Date id;

}
