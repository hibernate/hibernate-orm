/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.mapping.mutability.converted;

import org.hibernate.annotations.Mutability;
import org.hibernate.type.descriptor.java.Immutability;

/**
 * @author Steve Ebersole
 */
@Mutability(Immutability.class)
public class ImmutabilityDateConverter extends DateConverter {
}
