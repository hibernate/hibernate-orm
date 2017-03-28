/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;

/**
 * @author Luis Barreiro
 */
@Entity( name = "AdditionalDetails" )
public class AdditionalDetails {

	@Id
	private Long id;

	String details;

	@OneToOne( optional = false )
	@MapsId
	Post post;
}
