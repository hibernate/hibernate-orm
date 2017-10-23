/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.embedded;

import java.io.Serializable;
import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "PersonEmbed")
public class Person implements Serializable {
	@Id
	@GeneratedValue
	Integer id;

	String name;

	@Embedded
	Address address;

	@Embedded
	@AttributeOverrides( {
			@AttributeOverride(name = "iso2", column = @Column(name = "bornIso2")),
			@AttributeOverride(name = "name", column = @Column(name = "bornCountryName"))
	})
	Country bornIn;
}
