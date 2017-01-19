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
import org.hibernate.persister.common.spi.JoinColumnMapping;
import org.hibernate.persister.common.spi.Navigable;
import org.hibernate.persister.common.spi.NavigableSource;
import org.hibernate.persister.common.spi.PersistentAttribute;
import org.hibernate.persister.common.spi.SingularPersistentAttribute;
import org.hibernate.persister.embedded.spi.EmbeddedPersister;
import org.hibernate.persister.entity.spi.EntityHierarchy;
import org.hibernate.persister.entity.spi.IdentifierDescriptor;
import org.hibernate.type.spi.EmbeddedType;

/**
 * @author Steve Ebersole
 */
public class IdentifierDescriptorCompositeAggregated<O,J>
		extends AbstractSingularPersistentAttribute<O,J,EmbeddedType<J>>
		implements IdentifierDescriptor<O,J>, SingularPersistentAttribute<O,J>, NavigableSource<J> {
	private final EmbeddedPersister embeddablePersister;

	@SuppressWarnings("unchecked")
	public IdentifierDescriptorCompositeAggregated(
			EntityHierarchy entityHierarchy,
			Property idAttribute,
			EmbeddedPersister embeddablePersister) {
		super(
				entityHierarchy.getRootEntityPersister(),
				idAttribute.getName(),
				PersisterHelper.resolvePropertyAccess( entityHierarchy.getRootEntityPersister(), idAttribute ),
				embeddablePersister.getOrmType(),
				Disposition.ID,
				false
		);
		this.embeddablePersister = embeddablePersister;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// NavigableSource (embedded)

	@Override
	public Navigable findNavigable(String navigableName) {
		return embeddablePersister.findNavigable( navigableName );
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// IdentifierDescriptor

	@Override
	public EmbeddedType getIdType() {
		return getOrmType();
	}

	@Override
	public boolean hasSingleIdAttribute() {
		return true;
	}

	@Override
	public SingularPersistentAttribute getIdAttribute() {
		return this;
	}

	@Override
	public List<Column> getColumns() {
		return embeddablePersister.collectColumns();
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SingularAttribute


	@Override
	public PersistentAttributeType getPersistentAttributeType() {
		return PersistentAttributeType.EMBEDDED;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.EMBEDDABLE;
	}

	@Override
	public SingularAttributeClassification getAttributeTypeClassification() {
		return SingularAttributeClassification.EMBEDDED;
	}

	@Override
	public String asLoggableText() {
		return "IdentifierCompositeAggregated(" + embeddablePersister.asLoggableText() + ")";
	}

	@Override
	public List<JoinColumnMapping> resolveJoinColumnMappings(PersistentAttribute persistentAttribute) {
		return getSource().resolveJoinColumnMappings( persistentAttribute );
	}
}
