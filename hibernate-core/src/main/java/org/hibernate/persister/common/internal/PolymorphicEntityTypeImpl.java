/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.common.internal;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.cfg.NotYetImplementedException;
import org.hibernate.persister.common.spi.AbstractAttributeImpl;
import org.hibernate.persister.entity.internal.ImprovedEntityPersisterImpl;
import org.hibernate.persister.entity.spi.ImprovedEntityPersister;
import org.hibernate.sqm.domain.DomainMetamodel;
import org.hibernate.sqm.domain.EntityReference;
import org.hibernate.sqm.domain.PolymorphicEntityReference;

/**
 * @author Steve Ebersole
 */
public class PolymorphicEntityTypeImpl implements PolymorphicEntityReference {
	private final String name;
	private final List<ImprovedEntityPersister> implementors;

	private final Map<String,AbstractAttributeImpl> attributeDescriptorMap = new HashMap<String, AbstractAttributeImpl>();

	public PolymorphicEntityTypeImpl(
			DomainMetamodel modelMetadata,
			String name,
			List<ImprovedEntityPersister> implementors) {
		this.name = name;
		this.implementors = implementors;

		ImprovedEntityPersisterImpl firstImplementor = (ImprovedEntityPersisterImpl) implementors.get( 0 );
		attr_loop: for ( AbstractAttributeImpl attributeDescriptor : firstImplementor.getAttributeMap().values() ) {
			for ( EntityReference implementor : implementors ) {
				if ( implementor.findAttribute(  attributeDescriptor.getName() ) == null ) {
					break attr_loop;
				}
			}

			// if we get here, every implementor defined that attribute...
			attributeDescriptorMap.put( attributeDescriptor.getName(), attributeDescriptor );
		}
	}

	@Override
	public Set<EntityReference> getImplementors() {
		return implementors;
	}

}
