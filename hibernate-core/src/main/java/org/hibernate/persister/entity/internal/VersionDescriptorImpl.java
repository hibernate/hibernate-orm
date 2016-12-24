/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.entity.internal;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.hibernate.persister.common.spi.AbstractSingularAttribute;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.entity.spi.EntityHierarchy;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.persister.entity.spi.VersionDescriptor;
import org.hibernate.sqm.domain.EntityReference;
import org.hibernate.type.spi.Type;

/**
 * @author Steve Ebersole
 */
public class VersionDescriptorImpl extends AbstractSingularAttribute implements VersionDescriptor {
	private final Column column;
	private final String unsavedValue;

	public VersionDescriptorImpl(
			EntityHierarchy hierarchy,
			Column column,
			String name,
			Type ormType,
			boolean nullable,
			String unsavedValue) {
		super( hierarchy.getRootEntityPersister(), name, ormType, nullable );
		this.column = column;
		this.unsavedValue = unsavedValue;
	}

	@Override
	public EntityPersister getAttributeContainer() {
		return (EntityPersister) super.getAttributeContainer();
	}

	@Override
	public String getUnsavedValue() {
		return unsavedValue;
	}

	@Override
	public List<Column> getColumns() {
		return Collections.singletonList( column );
	}

	@Override
	public SingularAttributeClassification getAttributeTypeClassification() {
		return SingularAttributeClassification.BASIC;
	}

	@Override
	public String asLoggableText() {
		return "VersionDescriptor[" + getAttributeContainer().getEntityName() + "]";
	}

	@Override
	public Optional<EntityReference> toEntityReference() {
		return Optional.empty();
	}
}
