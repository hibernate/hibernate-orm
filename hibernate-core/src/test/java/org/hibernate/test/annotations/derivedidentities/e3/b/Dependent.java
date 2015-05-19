/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.derivedidentities.e3.b;
import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Dependent {
	// default column name for "name" attribute is overridden
	@AttributeOverride(name = "name", column = @Column(name = "dep_name"))
	@EmbeddedId
	DependentId id;


	@MapsId("empPK")
	@JoinColumns({
			@JoinColumn(name = "FK1", referencedColumnName = "FIRSTNAME"),
			@JoinColumn(name = "FK2", referencedColumnName = "lastName")
	})
	@ManyToOne
	Employee emp;

}
