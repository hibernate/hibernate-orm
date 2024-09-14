/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.models.categorize.spi;

/**
 * Metadata about a {@linkplain jakarta.persistence.metamodel.MappedSuperclassType mapped-superclass}
 *
 * @author Steve Ebersole
 */
public interface MappedSuperclassTypeMetadata extends IdentifiableTypeMetadata {
	@Override
	default Kind getManagedTypeKind() {
		return Kind.MAPPED_SUPER;
	}
}
