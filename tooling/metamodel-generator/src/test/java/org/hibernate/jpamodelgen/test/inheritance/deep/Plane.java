/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.inheritance.deep;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

/**
 * A base entity that defines an id attribute. Default access level should be
 * resolved from this class instead of continuing to {@link PersistenceBase}.
 *
 * @author Igor Vaynberg
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "planetype", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("Plane")
public class Plane extends PersistenceBase {
	@GeneratedValue
	@Id
	private Long id;
}
