/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.derivedidentities.e4.b;
import java.util.Date;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class FinancialHistory {
	@Id
	String id; // overriding not allowed ... // default join column name is overridden @MapsId
	@Temporal(TemporalType.DATE)
	Date lastupdate;

	@JoinColumn(name = "FK")
	@MapsId
	@ManyToOne
	Person patient;

}
