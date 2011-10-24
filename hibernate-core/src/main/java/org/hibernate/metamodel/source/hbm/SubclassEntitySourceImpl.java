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
package org.hibernate.metamodel.source.hbm;

import org.hibernate.internal.jaxb.mapping.hbm.EntityElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbJoinedSubclassElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbSubclassElement;
import org.hibernate.internal.jaxb.mapping.hbm.JaxbUnionSubclassElement;
import org.hibernate.metamodel.source.binder.SubclassEntitySource;
import org.hibernate.metamodel.source.binder.TableSource;

/**
 * @author Steve Ebersole
 */
public class SubclassEntitySourceImpl extends AbstractEntitySourceImpl implements SubclassEntitySource {
	protected SubclassEntitySourceImpl(MappingDocument sourceMappingDocument, EntityElement entityElement) {
		super( sourceMappingDocument, entityElement );
	}

	@Override
	public TableSource getPrimaryTable() {
		if ( JaxbJoinedSubclassElement.class.isInstance( entityElement() ) ) {
			return new TableSource() {
				@Override
				public String getExplicitSchemaName() {
					return ( (JaxbJoinedSubclassElement) entityElement() ).getSchema();
				}

				@Override
				public String getExplicitCatalogName() {
					return ( (JaxbJoinedSubclassElement) entityElement() ).getCatalog();
				}

				@Override
				public String getExplicitTableName() {
					return ( (JaxbJoinedSubclassElement) entityElement() ).getTable();
				}

				@Override
				public String getLogicalName() {
					// logical name for the primary table is null
					return null;
				}
			};
		}
		else if ( JaxbUnionSubclassElement.class.isInstance( entityElement() ) ) {
			return new TableSource() {
				@Override
				public String getExplicitSchemaName() {
					return ( (JaxbUnionSubclassElement) entityElement() ).getSchema();
				}

				@Override
				public String getExplicitCatalogName() {
					return ( (JaxbUnionSubclassElement) entityElement() ).getCatalog();
				}

				@Override
				public String getExplicitTableName() {
					return ( (JaxbUnionSubclassElement) entityElement() ).getTable();
				}

				@Override
				public String getLogicalName() {
					// logical name for the primary table is null
					return null;
				}
			};
		}
		return null;
	}

	@Override
	public String getDiscriminatorMatchValue() {
		return JaxbSubclassElement.class.isInstance( entityElement() )
				? ( (JaxbSubclassElement) entityElement() ).getDiscriminatorValue()
				: null;
	}
}
