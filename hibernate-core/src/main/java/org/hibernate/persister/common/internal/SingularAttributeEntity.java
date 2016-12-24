/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.persister.common.internal;

import java.util.List;
import java.util.Optional;

import org.hibernate.persister.common.spi.AbstractSingularAttribute;
import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.JoinColumnMapping;
import org.hibernate.persister.common.spi.JoinableAttribute;
import org.hibernate.persister.common.spi.JoinableAttributeContainer;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.sqm.domain.EntityReference;
import org.hibernate.type.spi.EntityType;


/**
 * @author Steve Ebersole
 */
public class SingularAttributeEntity extends AbstractSingularAttribute<EntityType> implements JoinableAttribute {
	private final SingularAttributeClassification classification;
	private final EntityPersister entityPersister;
	private final List<Column> columns;

	private List<JoinColumnMapping> joinColumnMappings;

	public SingularAttributeEntity(
			JoinableAttributeContainer declaringType,
			String name,
			SingularAttributeClassification classification,
			EntityType ormType,
			EntityPersister entityPersister,
			List<Column> columns) {
		super( declaringType, name, ormType, true );
		this.classification = classification;
		this.entityPersister = entityPersister;

		// columns should be the rhs columns I believe.
		//		todo : add an assertion based on whatever this should be...
		this.columns = columns;
	}

	@Override
	public JoinableAttributeContainer getAttributeContainer() {
		return (JoinableAttributeContainer) super.getAttributeContainer();
	}

	public EntityPersister getAssociatedEntityPersister() {
		return entityPersister;
	}

	@Override
	public SingularAttributeClassification getAttributeTypeClassification() {
		return classification;
	}

	public List<Column> getColumns() {
		return columns;
	}

	@Override
	public String asLoggableText() {
		return "SingularAttributeEntity([" + getAttributeTypeClassification().name() + "] " +
				getLeftHandSide().asLoggableText() + '.' + getAttributeName() +
				")";
	}

	@Override
	public String toString() {
		return asLoggableText();
	}

	@Override
	public Optional<EntityReference> toEntityReference() {
		return Optional.of( entityPersister );
	}

	public String getEntityName() {
		return entityPersister.getEntityName();
	}

	@Override
	public List<JoinColumnMapping> getJoinColumnMappings() {
		if ( joinColumnMappings == null ) {
			this.joinColumnMappings = getAttributeContainer().resolveJoinColumnMappings( this );
		}
		return joinColumnMappings;
	}
}
