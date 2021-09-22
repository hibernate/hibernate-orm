/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.derivedidentities.e3.b;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name="`Dependent`")
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
