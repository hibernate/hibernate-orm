/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.derivedidentities.e2.a;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * @author Emmanuel Bernard
 */
@Entity
@Table(name = "`Dependent`")
@IdClass(DependentId.class)
public class Dependent {
	@Id
	String name;
	
	@Id @ManyToOne
	@JoinColumns({
			@JoinColumn(name="FK1", referencedColumnName="firstName"),
			@JoinColumn(name="FK2", referencedColumnName="lastName")
	})
	Employee emp;
}
