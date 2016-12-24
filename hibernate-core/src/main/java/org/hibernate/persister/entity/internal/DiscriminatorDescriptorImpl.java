/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.persister.entity.internal;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.hibernate.persister.common.spi.AttributeContainer;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.entity.spi.DiscriminatorDescriptor;
import org.hibernate.persister.entity.spi.EntityHierarchy;
import org.hibernate.sqm.domain.EntityReference;
import org.hibernate.sqm.domain.SingularAttributeReference;
import org.hibernate.type.spi.Type;

/**
 * @author Steve Ebersole
 */
public class DiscriminatorDescriptorImpl implements DiscriminatorDescriptor {

	private final EntityHierarchy hierarchy;
	private final Type ormType;
	private final Column column;

	public DiscriminatorDescriptorImpl(EntityHierarchy hierarchy, Type ormType, Column column) {
		this.hierarchy = hierarchy;
		this.ormType = ormType;
		this.column = column;
	}

	@Override
	public Type getOrmType() {
		return ormType;
	}

	@Override
	public Optional<EntityReference> toEntityReference() {
		return null;
	}

	@Override
	public String getAttributeName() {
		return "<discriminator>";
	}

	@Override
	public AttributeContainer getAttributeContainer() {
		return hierarchy.getRootEntityPersister();
	}

	@Override
	public List<Column> getColumns() {
		return Collections.singletonList( column );
	}

	@Override
	public boolean isNullable() {
		return false;
	}

	@Override
	public SingularAttributeReference.SingularAttributeClassification getAttributeTypeClassification() {
		return SingularAttributeReference.SingularAttributeClassification.BASIC;
	}

	@Override
	public String asLoggableText() {
		return "<discriminator>";
	}
}
