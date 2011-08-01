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
package org.hibernate.metamodel.source.annotations.attribute;

import java.util.Map;

import org.hibernate.AssertionFailure;
import org.hibernate.metamodel.binding.IdGenerator;
import org.hibernate.metamodel.source.binder.SimpleIdentifierSource;
import org.hibernate.metamodel.source.binder.SingularAttributeSource;

/**
 * @author Hardy Ferentschik
 */
public class SimpleIdentifierSourceImpl implements SimpleIdentifierSource {
	private final BasicAttribute attribute;
	private final Map<String, AttributeOverride> attributeOverrideMap;

	public SimpleIdentifierSourceImpl(BasicAttribute attribute, Map<String, AttributeOverride> attributeOverrideMap) {
		if ( !attribute.isId() ) {
			throw new AssertionFailure(
					String.format(
							"A non id attribute was passed to SimpleIdentifierSourceImpl: %s",
							attribute.toString()
					)
			);
		}
		this.attribute = attribute;
		this.attributeOverrideMap = attributeOverrideMap;
	}

	@Override
	public Nature getNature() {
		return Nature.SIMPLE;
	}

	@Override
	public SingularAttributeSource getIdentifierAttributeSource() {
		return new SingularAttributeSourceImpl( attribute );
	}

	@Override
	public IdGenerator getIdentifierGeneratorDescriptor() {
		return attribute.getIdGenerator();
	}
}


