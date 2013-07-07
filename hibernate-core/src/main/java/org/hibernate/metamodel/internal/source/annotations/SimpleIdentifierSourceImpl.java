/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal.source.annotations;

import java.util.Collections;

import org.hibernate.AssertionFailure;
import org.hibernate.id.EntityIdentifierNature;
import org.hibernate.metamodel.internal.source.annotations.attribute.BasicAttribute;
import org.hibernate.metamodel.internal.source.annotations.entity.RootEntityClass;
import org.hibernate.metamodel.spi.binding.IdentifierGeneratorDefinition;
import org.hibernate.metamodel.spi.source.MetaAttributeSource;
import org.hibernate.metamodel.spi.source.SimpleIdentifierSource;
import org.hibernate.metamodel.spi.source.SingularAttributeSource;

/**
 * @author Hardy Ferentschik
 */
public class SimpleIdentifierSourceImpl implements SimpleIdentifierSource {
	private final BasicAttribute attribute;
	private final RootEntityClass rootEntityClass;
	private final SingularAttributeSourceImpl source ;
	public SimpleIdentifierSourceImpl(RootEntityClass rootEntityClass, BasicAttribute attribute) {
		if ( !attribute.isId() ) {
			throw new AssertionFailure(
					String.format(
							"A non id attribute was passed to SimpleIdentifierSourceImpl: %s",
							attribute.toString()
					)
			);
		}
		this.rootEntityClass = rootEntityClass;
		this.attribute = attribute;
		this.source= new SingularAttributeSourceImpl( attribute );
		source.applyAttributeOverride( rootEntityClass.getAttributeOverrideMap() );
	}

	@Override
	public EntityIdentifierNature getNature() {
		return EntityIdentifierNature.SIMPLE;
	}

	@Override
	public SingularAttributeSource getIdentifierAttributeSource() {

		return source;
	}

	@Override
	public IdentifierGeneratorDefinition getIdentifierGeneratorDescriptor() {
		return attribute.getIdentifierGeneratorDefinition();
	}

	@Override
	public String getUnsavedValue() {
		return null;
	}

	@Override
	public Iterable<MetaAttributeSource> getMetaAttributeSources() {
		return Collections.emptySet();
	}
}


