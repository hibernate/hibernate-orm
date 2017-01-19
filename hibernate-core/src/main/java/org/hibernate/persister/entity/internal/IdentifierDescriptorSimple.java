/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.persister.entity.internal;

import java.util.List;

import org.hibernate.mapping.Property;
import org.hibernate.persister.common.internal.PersisterHelper;
import org.hibernate.persister.common.spi.AbstractSingularPersistentAttribute;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.SingularPersistentAttribute;
import org.hibernate.persister.entity.spi.EntityHierarchy;
import org.hibernate.persister.entity.spi.IdentifiableTypeImplementor;
import org.hibernate.persister.entity.spi.IdentifierDescriptor;
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 */
public class IdentifierDescriptorSimple<O,J>
		extends AbstractSingularPersistentAttribute<O,J,BasicType<J>>
		implements IdentifierDescriptor<O,J>, SingularPersistentAttribute<O,J> {
	private final List<Column> columns;

	public IdentifierDescriptorSimple(
			EntityHierarchy hierarchy,
			IdentifiableTypeImplementor declarer,
			Property property,
			BasicType<J> ormType,
			List<Column> columns) {
		super(
				hierarchy.getRootEntityPersister(),
				property.getName(),
				PersisterHelper.resolvePropertyAccess( declarer, property ),
				ormType,
				Disposition.ID,
				false
		);
		this.columns = columns;
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
	public SingularPersistentAttribute<O,J> getIdAttribute() {
		return this;
	}

	@Override
	public SingularAttributeClassification getAttributeTypeClassification() {
		return SingularAttributeClassification.BASIC;
	}

	@Override
	public String asLoggableText() {
		return "IdentifierSimple(" + getSource().asLoggableText() + ")";
	}

	@Override
	public PersistentAttributeType getPersistentAttributeType() {
		return PersistentAttributeType.BASIC;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}
}
