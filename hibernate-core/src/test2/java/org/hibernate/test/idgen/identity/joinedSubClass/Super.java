/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.idgen.identity.joinedSubClass;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;

import static javax.persistence.GenerationType.IDENTITY;
import static javax.persistence.InheritanceType.JOINED;

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
