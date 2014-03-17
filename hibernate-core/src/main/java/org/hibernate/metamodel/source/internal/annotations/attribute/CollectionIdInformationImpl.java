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
package org.hibernate.metamodel.source.internal.annotations.attribute;

import java.util.List;

import org.hibernate.metamodel.source.internal.annotations.attribute.type.AttributeTypeResolver;
import org.hibernate.metamodel.spi.binding.IdentifierGeneratorDefinition;

/**
 * @author Steve Ebersole
 */
public class CollectionIdInformationImpl implements CollectionIdInformation {
	private final List<Column> columns;
	private final AttributeTypeResolver typeResolver;
	private final IdentifierGeneratorDefinition generatorDefinition;

	public CollectionIdInformationImpl(
			List<Column> columns,
			AttributeTypeResolver typeResolver,
			IdentifierGeneratorDefinition generatorDefinition) {
		this.columns = columns;
		this.typeResolver = typeResolver;
		this.generatorDefinition = generatorDefinition;
	}

	@Override
	public List<Column> getColumns() {
		return columns;
	}

	@Override
	public AttributeTypeResolver getTypeResolver() {
		return typeResolver;
	}

	@Override
	public IdentifierGeneratorDefinition getGeneratorDefinition() {
		return generatorDefinition;
	}
}
