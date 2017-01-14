/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.persister.common.internal;

import java.util.Collections;
import java.util.List;

import org.hibernate.persister.common.spi.AbstractSingularAttribute;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.JoinColumnMapping;
import org.hibernate.persister.common.spi.JoinableAttribute;
import org.hibernate.persister.common.spi.ManagedTypeImplementor;
import org.hibernate.persister.common.spi.SingularAttribute;
import org.hibernate.persister.embeddable.spi.EmbeddablePersister;
import org.hibernate.persister.embeddable.spi.EmbeddableReference;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.sqm.domain.SqmNavigable;
import org.hibernate.type.spi.EmbeddedType;

/**
 * @author Steve Ebersole
 */
public class SingularAttributeEmbedded
		extends AbstractSingularAttribute<EmbeddedType>
		implements SingularAttribute, EmbeddableReference, JoinableAttribute {

	private final EmbeddablePersister embeddablePersister;

	public SingularAttributeEmbedded(
			ManagedTypeImplementor declaringType,
			String attributeName,
			PropertyAccess propertyAccess,
			Disposition disposition,
			EmbeddablePersister embeddablePersister) {
		super( declaringType, attributeName, propertyAccess, embeddablePersister.getOrmType(), disposition, true );
		this.embeddablePersister = embeddablePersister;
	}

	@Override
	public ManagedTypeImplementor getSource() {
		return super.getSource();
	}

	@Override
	public EmbeddedType getExportedDomainType() {
		return (EmbeddedType) super.getExportedDomainType();
	}

	public EmbeddablePersister getEmbeddablePersister() {
		return embeddablePersister;
	}

	@Override
	public SingularAttributeClassification getAttributeTypeClassification() {
		return SingularAttributeClassification.EMBEDDED;
	}

	@Override
	public List<Column> getColumns() {
		return embeddablePersister.collectColumns();
	}

	@Override
	public String asLoggableText() {
		return toString();
	}

	@Override
	public List<JoinColumnMapping> getJoinColumnMappings() {
		// there are no columns involved in a join to an embedded/composite attribute
		return Collections.emptyList();
	}

	@Override
	public PersistentAttributeType getPersistentAttributeType() {
		return PersistentAttributeType.EMBEDDED;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.EMBEDDABLE;
	}

	@Override
	public SqmNavigable findNavigable(String navigableName) {
		return getEmbeddablePersister().findNavigable( navigableName );
	}
}
