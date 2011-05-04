/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.persister.collection;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.Mapping;
import org.hibernate.persister.entity.AbstractPropertyMapping;
import org.hibernate.type.CompositeType;
import org.hibernate.type.Type;

/**
 * @author Gavin King
 */
public class CompositeElementPropertyMapping extends AbstractPropertyMapping {

	private final CompositeType compositeType;
	
	public CompositeElementPropertyMapping(
			String[] elementColumns,
			String[] elementColumnReaders,
			String[] elementColumnReaderTemplates, 
			String[] elementFormulaTemplates, 
			CompositeType compositeType,
			Mapping factory)
	throws MappingException {

		this.compositeType = compositeType;

		initComponentPropertyPaths(null, compositeType, elementColumns, elementColumnReaders,
				elementColumnReaderTemplates, elementFormulaTemplates, factory);

	}

	public Type getType() {
		return compositeType;
	}

	protected String getEntityName() {
		return compositeType.getName();
	}

}
