/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.persister.entity.internal;

import java.util.List;
import java.util.Optional;

import org.hibernate.persister.common.spi.AbstractSingularAttribute;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.SingularOrmAttribute;
import org.hibernate.persister.entity.spi.EntityHierarchy;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.persister.entity.spi.IdentifierDescriptor;
import org.hibernate.sqm.domain.EntityReference;
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 */
public class IdentifierDescriptorSimple
		extends AbstractSingularAttribute<BasicType>
		implements IdentifierDescriptor, SingularOrmAttribute {
	private final List<Column> columns;

	public IdentifierDescriptorSimple(
			EntityHierarchy hierarchy,
			String attributeName,
			BasicType ormType,
			List<Column> columns) {
		super( hierarchy.getRootEntityPersister(), attributeName, ormType, false );
		this.columns = columns;
	}

	@Override
	public EntityPersister getAttributeContainer() {
		return (EntityPersister) super.getAttributeContainer();
	}

	@Override
	public BasicType getIdType() {
		return getOrmType();
	}

	@Override
	public boolean hasSingleIdAttribute() {
		return true;
	}

	@Override
	public List<Column> getColumns() {
		return columns;
	}

	@Override
	public SingularOrmAttribute getIdAttribute() {
		return this;
	}

	@Override
	public SingularAttributeClassification getAttributeTypeClassification() {
		return SingularAttributeClassification.BASIC;
	}

	@Override
	public String asLoggableText() {
		return "IdentifierSimple(" + getLeftHandSide().asLoggableText() + ")";
	}

	@Override
	public Optional<EntityReference> toEntityReference() {
		return Optional.empty();
	}
}
