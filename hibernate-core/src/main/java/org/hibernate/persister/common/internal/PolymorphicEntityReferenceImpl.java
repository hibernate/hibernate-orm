/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.persister.common.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.metamodel.spi.MetamodelImplementor;
import org.hibernate.persister.common.spi.Attribute;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.sqm.domain.EntityReference;
import org.hibernate.sqm.domain.PolymorphicEntityReference;

/**
 * @author Steve Ebersole
 */
public class PolymorphicEntityReferenceImpl implements PolymorphicEntityReference {
	private final String name;
	private final List<EntityPersister> implementors;

	private final Map<String,Attribute> attributeDescriptorMap = new HashMap<>();

	public PolymorphicEntityReferenceImpl(
			MetamodelImplementor metadata,
			String name,
			List<EntityPersister> implementors) {
		this.name = name;
		this.implementors = implementors;

		EntityPersister firstImplementor = implementors.get( 0 );
		attr_loop: for ( Attribute attribute : firstImplementor.getNonIdentifierAttributes() ) {
			for ( EntityPersister implementor : implementors ) {
				if ( implementor == firstImplementor ) {
					continue attr_loop;
				}
				if ( implementor.findAttribute( attribute.getAttributeName() ) == null ) {
					break attr_loop;
				}
			}

			// if we get here, every implementor defined that attribute...
			attributeDescriptorMap.put( attribute.getAttributeName(), attribute );
		}

		// todo : add identifier, etc?
	}

	@Override
	public Set<EntityReference> getImplementors() {
		return implementors.stream().collect( Collectors.toSet() );
	}

	@Override
	public String getEntityName() {
		return name;
	}

	@Override
	public String asLoggableText() {
		return "PolymorphicEntityReference(" + name + ")";
	}

	@Override
	public Optional<EntityReference> toEntityReference() {
		return Optional.empty();
	}
}
