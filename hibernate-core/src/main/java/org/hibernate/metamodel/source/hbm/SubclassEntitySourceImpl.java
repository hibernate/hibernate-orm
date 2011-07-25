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

import org.hibernate.metamodel.source.binder.SubclassEntitySource;
import org.hibernate.metamodel.source.binder.TableSource;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.EntityElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLJoinedSubclassElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLSubclassElement;
import org.hibernate.metamodel.source.hbm.jaxb.mapping.XMLUnionSubclassElement;

/**
 * @author Steve Ebersole
 */
public class SubclassEntitySourceImpl extends AbstractEntitySourceImpl implements SubclassEntitySource {
	protected SubclassEntitySourceImpl(MappingDocument sourceMappingDocument, EntityElement entityElement) {
		super( sourceMappingDocument, entityElement );
	}

	@Override
	public TableSource getPrimaryTable() {
		if ( XMLJoinedSubclassElement.class.isInstance( entityElement() ) ) {
			return new TableSource() {
				@Override
				public String getExplicitSchemaName() {
					return ( (XMLJoinedSubclassElement) entityElement() ).getSchema();
				}

				@Override
				public String getExplicitCatalogName() {
					return ( (XMLJoinedSubclassElement) entityElement() ).getCatalog();
				}

				@Override
				public String getExplicitTableName() {
					return ( (XMLJoinedSubclassElement) entityElement() ).getTable();
				}

				@Override
				public String getLogicalName() {
					// logical name for the primary table is null
					return null;
				}
			};
		}
		else if ( XMLUnionSubclassElement.class.isInstance( entityElement() ) ) {
			return new TableSource() {
				@Override
				public String getExplicitSchemaName() {
					return ( (XMLUnionSubclassElement) entityElement() ).getSchema();
				}

				@Override
				public String getExplicitCatalogName() {
					return ( (XMLUnionSubclassElement) entityElement() ).getCatalog();
				}

				@Override
				public String getExplicitTableName() {
					return ( (XMLUnionSubclassElement) entityElement() ).getTable();
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
		return XMLSubclassElement.class.isInstance( entityElement() )
				? ( (XMLSubclassElement) entityElement() ).getDiscriminatorValue()
				: null;
	}
}
