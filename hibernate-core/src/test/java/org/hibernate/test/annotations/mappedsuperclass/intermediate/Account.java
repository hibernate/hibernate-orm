/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.mappedsuperclass.intermediate;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

/**
 * The intermediate entity in the hierarchy
 *
 * @author Saša Obradović
 */
@Entity
@Table(name = "`ACCOUNT`")
@Inheritance(strategy = InheritanceType.JOINED)
public class Account extends AccountBase {
	public Account() {
	}

	public Account(String accountNumber) {
		super( accountNumber );
	}
}
