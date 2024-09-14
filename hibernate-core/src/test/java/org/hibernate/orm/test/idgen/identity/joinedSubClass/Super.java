/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.idgen.identity.joinedSubClass;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;

import static jakarta.persistence.GenerationType.IDENTITY;
import static jakarta.persistence.InheritanceType.JOINED;

/**
 * @author Andrey Vlasov
 * @author Steve Ebersole
 */
@Entity
@Inheritance(strategy = JOINED)
public class Super {
	@Id
	@GeneratedValue(strategy = IDENTITY)
	private Long id;

	@Column(name="`value`")
	private Long value;
}
