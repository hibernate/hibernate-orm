/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.entity.internal;

import java.util.List;

import org.hibernate.persister.common.spi.AbstractSingularPersistentAttribute;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.JoinColumnMapping;
import org.hibernate.persister.common.spi.Navigable;
import org.hibernate.persister.common.spi.NavigableSource;
import org.hibernate.persister.common.spi.PersistentAttribute;
import org.hibernate.persister.common.spi.SingularPersistentAttribute;
import org.hibernate.persister.common.spi.VirtualPersistentAttribute;
import org.hibernate.persister.embedded.spi.EmbeddedPersister;
import org.hibernate.persister.entity.spi.EntityHierarchy;
import org.hibernate.persister.entity.spi.IdentifierDescriptor;
import org.hibernate.property.access.internal.PropertyAccessStrategyEmbeddedImpl;
import org.hibernate.type.spi.EmbeddedType;

/**
 * @author Steve Ebersole
 */
public class IdentifierDescriptorCompositeNonAggregated<O,J>
		extends AbstractSingularPersistentAttribute<O,J,EmbeddedType<J>>
		implements IdentifierDescriptor<O,J>, SingularPersistentAttribute<O,J>, NavigableSource<J>, VirtualPersistentAttribute<O,J> {
	// todo : IdClass handling eventually

	public static final String NAVIGABLE_NAME = "{id}";

	private final EmbeddedPersister embeddablePersister;

	@SuppressWarnings("unchecked")
	public IdentifierDescriptorCompositeNonAggregated(
			EntityHierarchy entityHierarchy,

			EmbeddedPersister embeddablePersister) {
		super(
				entityHierarchy.getRootEntityPersister(),
				NAVIGABLE_NAME,
				PropertyAccessStrategyEmbeddedImpl.INSTANCE.buildPropertyAccess( null, NAVIGABLE_NAME ),
				embeddablePersister.getOrmType(),
				Disposition.ID,
				false
		);
		this.embeddablePersister = embeddablePersister;
	}

	@Override
	public List<Column> getColumns() {
		return embeddablePersister.collectColumns();
	}

	@Override
	public EmbeddedType getIdType() {
		return embeddablePersister.getOrmType();
	}

	@Override
	public boolean hasSingleIdAttribute() {
		return false;
	}

	@Override
	public PersistentAttributeType getPersistentAttributeType() {
		return PersistentAttributeType.EMBEDDED;
	}

	@Override
	public SingularPersistentAttribute getIdAttribute() {
		return this;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// SingularAttributeImplementor

	@Override
	public SingularAttributeClassification getAttributeTypeClassification() {
		return SingularAttributeClassification.EMBEDDED;
	}

	@Override
	public String getAttributeName() {
		return NAVIGABLE_NAME;
	}

	@Override
	public String asLoggableText() {
		return "IdentifierCompositeNonAggregated(" + getSource().asLoggableText() + ")";
	}

	@Override
	public Navigable findNavigable(String navigableName) {
		return embeddablePersister.findNavigable( navigableName );
	}

	@Override
	public List<JoinColumnMapping> resolveJoinColumnMappings(PersistentAttribute persistentAttribute) {
		return getSource().resolveJoinColumnMappings( persistentAttribute );
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.EMBEDDABLE;
	}
}
