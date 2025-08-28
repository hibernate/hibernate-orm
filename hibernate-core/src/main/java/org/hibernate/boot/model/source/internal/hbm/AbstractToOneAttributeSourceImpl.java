/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.source.internal.hbm;

import org.hibernate.boot.model.source.spi.AttributeSource;
import org.hibernate.boot.model.source.spi.NaturalIdMutability;
import org.hibernate.boot.model.source.spi.SingularAttributeSourceToOne;
import org.hibernate.tuple.GenerationTiming;

/**
 * @author Steve Ebersole
 * @author Gail Badner
 */
public abstract class AbstractToOneAttributeSourceImpl
		extends AbstractHbmSourceNode
		implements SingularAttributeSourceToOne {
	private final NaturalIdMutability naturalIdMutability;

	AbstractToOneAttributeSourceImpl(
			MappingDocument sourceMappingDocument,
			NaturalIdMutability naturalIdMutability) {
		super( sourceMappingDocument );
		this.naturalIdMutability = naturalIdMutability;
	}

	@Override
	public NaturalIdMutability getNaturalIdMutability() {
		return naturalIdMutability;
	}

	@Override
	public boolean isSingular() {
		return true;
	}

	@Override
	public boolean isVirtualAttribute() {
		return false;
	}

	@Override
	public GenerationTiming getGenerationTiming() {
		return GenerationTiming.NEVER;
	}

	@Override
	public boolean isIgnoreNotFound() {
		return false;
	}

	@Override
	public boolean isMappedBy() {
		// only applies to annotations
		return false;
	}

	@Override
	public AttributeSource getAttributeSource() {
		return this;
	}

	@Override
	public boolean createForeignKeyConstraint() {
		// TODO: Can HBM do something like JPA's @ForeignKey(NO_CONSTRAINT)?
		return true;
	}

}
