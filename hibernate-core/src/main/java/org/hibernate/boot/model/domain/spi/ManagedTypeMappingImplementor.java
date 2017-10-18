/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.model.domain.spi;

import org.hibernate.boot.model.domain.ManagedTypeMapping;
import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.metamodel.model.domain.RepresentationMode;

/**
 * @author Steve Ebersole
 */
public interface ManagedTypeMappingImplementor extends ManagedTypeMapping {

	void addDeclaredPersistentAttribute(PersistentAttributeMapping attribute);

	void setSuperManagedType(ManagedTypeMapping superTypeMapping);

	void setExplicitRepresentationMode(RepresentationMode mode);
}
