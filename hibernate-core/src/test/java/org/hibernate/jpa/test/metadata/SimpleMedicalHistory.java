/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.metadata;
import java.io.Serializable;
import java.util.Date;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class SimpleMedicalHistory implements Serializable {

	@Temporal(TemporalType.DATE)
	Date lastupdate;

	@Id
	@JoinColumn(name = "FK")
	@OneToOne
	SimplePerson patient;
}
