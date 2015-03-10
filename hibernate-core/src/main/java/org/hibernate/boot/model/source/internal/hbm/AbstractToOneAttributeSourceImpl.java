/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
