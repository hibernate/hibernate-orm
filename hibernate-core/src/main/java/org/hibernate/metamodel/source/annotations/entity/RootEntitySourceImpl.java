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
package org.hibernate.metamodel.source.annotations.entity;

import org.hibernate.AssertionFailure;
import org.hibernate.EntityMode;
import org.hibernate.engine.OptimisticLockStyle;
import org.hibernate.metamodel.binding.Caching;
import org.hibernate.metamodel.source.annotations.attribute.SimpleAttribute;
import org.hibernate.metamodel.source.annotations.attribute.SimpleIdentifierSourceImpl;
import org.hibernate.metamodel.source.annotations.attribute.SingularAttributeSourceImpl;
import org.hibernate.metamodel.source.binder.IdentifierSource;
import org.hibernate.metamodel.source.binder.RootEntitySource;
import org.hibernate.metamodel.source.binder.SingularAttributeSource;

/**
 * @author Hardy Ferentschik
 */
public class RootEntitySourceImpl extends EntitySourceImpl implements RootEntitySource {
	public RootEntitySourceImpl(EntityClass entityClass) {
		super( entityClass );
	}

	@Override
	public IdentifierSource getIdentifierSource() {
		IdType idType = getEntityClass().getIdType();
		switch ( idType ) {
			case SIMPLE: {
				SimpleAttribute attribute = getEntityClass().getIdAttributes().iterator().next();
				return new SimpleIdentifierSourceImpl( attribute );
			}
			case COMPOSED: {
				break;
			}
			case EMBEDDED: {
				break;
			}
			default: {
				throw new AssertionFailure( "The root entity needs to specify an identifier" );
			}
		}

		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public SingularAttributeSource getVersioningAttributeSource() {
		SingularAttributeSource attributeSource = null;
		if ( getEntityClass().getVersionAttribute() != null ) {
			attributeSource = new SingularAttributeSourceImpl( getEntityClass().getVersionAttribute() );
		}
		return attributeSource;
	}

	@Override
	public SingularAttributeSource getDiscriminatorAttributeSource() {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	@Override
	public EntityMode getEntityMode() {
		return EntityMode.POJO;
	}

	@Override
	public boolean isMutable() {
		return getEntityClass().isMutable();
	}

	@Override
	public boolean isExplicitPolymorphism() {
		return getEntityClass().isExplicitPolymorphism();
	}

	@Override
	public String getWhere() {
		return getEntityClass().getWhereClause();
	}

	@Override
	public String getRowId() {
		return getEntityClass().getRowId();
	}

	@Override
	public OptimisticLockStyle getOptimisticLockStyle() {
		return getEntityClass().getOptimisticLockStyle();
	}

	@Override
	public Caching getCaching() {
		return getEntityClass().getCaching();
	}
}


