/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.io.Serializable;
import javax.persistence.metamodel.EntityType;

import org.hibernate.graph.internal.SubGraphImpl;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.model.domain.AbstractIdentifiableType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.IdentifiableDomainType;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.metamodel.model.domain.SingularPersistentAttribute;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.hql.spi.SqmCreationState;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Defines the Hibernate implementation of the JPA {@link EntityType} contract.
 *
 * @author Steve Ebersole
 * @author Emmanuel Bernard
 */
public class EntityTypeImpl<J>
		extends AbstractIdentifiableType<J>
		implements EntityDomainType<J>, Serializable {
	private final String jpaEntityName;

	public EntityTypeImpl(
			JavaTypeDescriptor<J> javaTypeDescriptor,
			IdentifiableDomainType<? super J> superType,
			PersistentClass persistentClass,
			JpaMetamodel jpaMetamodel) {
		super(
				persistentClass.getEntityName(),
				javaTypeDescriptor,
				superType,
				persistentClass.getDeclaredIdentifierMapper() != null || ( superType != null && superType.hasIdClass() ),
				persistentClass.hasIdentifierProperty(),
				persistentClass.isVersioned(),
				jpaMetamodel
		);

		this.jpaEntityName = persistentClass.getJpaEntityName();
	}

	@Override
	public String getName() {
		return jpaEntityName;
	}

	@Override
	public String getHibernateEntityName() {
		return super.getTypeName();
	}

	@Override
	public String getPathName() {
		return getHibernateEntityName();
	}

	@Override
	public EntityDomainType<J> getSqmPathType() {
		return this;
	}

	@Override
	public SqmPathSource<?> findSubPathSource(String name) {
		final PersistentAttribute<?,?> attribute = findAttribute( name );
		if ( attribute != null ) {
			return (SqmPathSource<?>) attribute;
		}

		if ( "id".equalsIgnoreCase( name ) ) {
			// todo (6.0) : probably need special handling here for non-aggregated composite ids
		}

		return null;
	}

	@Override
	public PersistentAttribute<? super J, ?> findAttribute(String name) {
		final PersistentAttribute<? super J, ?> attribute = super.findAttribute( name );
		if ( attribute != null ) {
			return attribute;
		}

		if ( "id".equalsIgnoreCase( name ) ) {
			//noinspection unchecked
			final SingularPersistentAttribute<J, ?> idAttribute = findIdAttribute();
			//noinspection RedundantIfStatement
			if ( idAttribute != null ) {
				return idAttribute;
			}
		}

		return null;
	}

	@Override
	public BindableType getBindableType() {
		return BindableType.ENTITY_TYPE;
	}

	@Override
	public Class<J> getBindableJavaType() {
		return getJavaType();
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.ENTITY;
	}

	@Override
	public IdentifiableDomainType<? super J> getSuperType() {
		return super.getSuperType();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <S extends J> SubGraphImplementor<S> makeSubGraph(Class<S> subType) {
		if ( ! getBindableJavaType().isAssignableFrom( subType ) ) {
			throw new IllegalArgumentException(
					String.format(
							"Entity type [%s] cannot be treated as requested sub-type [%s]",
							getName(),
							subType.getName()
					)
			);
		}

		return new SubGraphImpl( this, true, jpaMetamodel() );
	}

	@Override
	public SubGraphImplementor<J> makeSubGraph() {
		return makeSubGraph( getBindableJavaType() );
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public SqmPath<J> createSqmPath(
			SqmPath<?> lhs,
			SqmCreationState creationState) {
		throw new UnsupportedOperationException(
				"EntityType cannot be used to create an SqmPath - that would be an SqmFrom which are created directly"
		);
	}
}
