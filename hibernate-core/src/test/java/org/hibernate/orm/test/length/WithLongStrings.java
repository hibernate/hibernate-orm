/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.length;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import static org.hibernate.Length.*;

@Entity
public class WithLongStrings {
	@Id
	@GeneratedValue
	public int id;

	@Column(length = LONG)
	public String longish;

	@Column(length = LONG16)
	public String long16;

	@Column(length = LONG32)
	public String long32;

	@Column(length = LOB_DEFAULT+1)
	public String clob;
}
