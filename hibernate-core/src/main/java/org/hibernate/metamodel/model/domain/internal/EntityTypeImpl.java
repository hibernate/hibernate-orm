/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Collection;

import jakarta.persistence.metamodel.EntityType;

import org.hibernate.graph.internal.SubGraphImpl;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.UnsupportedMappingException;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.model.domain.AbstractIdentifiableType;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.IdentifiableDomainType;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.model.domain.PersistentAttribute;
import org.hibernate.metamodel.model.domain.SingularPersistentAttribute;
import org.hibernate.metamodel.model.domain.spi.JpaMetamodelImplementor;
import org.hibernate.persister.entity.DiscriminatorMetadata;
import org.hibernate.persister.entity.Queryable;
import org.hibernate.query.sqm.SqmPathSource;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.JavaType;

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
	private final SqmPathSource<?> discriminatorPathSource;

	public EntityTypeImpl(
			String entityName,
			String jpaEntityName,
			boolean hasIdClass,
			boolean hasIdProperty,
			boolean hasVersion,
			JavaType<J> javaType,
			IdentifiableDomainType<? super J> superType,
			JpaMetamodelImplementor jpaMetamodel) {
		super(
				entityName,
				javaType,
				superType,
				hasIdClass,
				hasIdProperty,
				hasVersion,
				jpaMetamodel
		);

		this.jpaEntityName = jpaEntityName;

		final Queryable entityDescriptor = (Queryable)
				jpaMetamodel.getMappingMetamodel()
						.getEntityDescriptor( getHibernateEntityName() );
		final DiscriminatorMetadata discriminatorMetadata = entityDescriptor.getTypeDiscriminatorMetadata();
		final DomainType discriminatorType;
		if ( discriminatorMetadata != null ) {
			discriminatorType = (DomainType) discriminatorMetadata.getResolutionType();
		}
		else {
			discriminatorType = jpaMetamodel.getTypeConfiguration()
					.getBasicTypeRegistry()
					.resolve( StandardBasicTypes.STRING );
		}

		this.discriminatorPathSource = discriminatorType == null ? null : new DiscriminatorSqmPathSource(
				discriminatorType,
				this,
				entityDescriptor
		);
	}

	public EntityTypeImpl(
			JavaType<J> javaType,
			IdentifiableDomainType<? super J> superType,
			PersistentClass persistentClass,
			JpaMetamodelImplementor jpaMetamodel) {
		this(
				persistentClass.getEntityName(),
				persistentClass.getJpaEntityName(),
				persistentClass.getDeclaredIdentifierMapper() != null
						|| superType != null && superType.hasIdClass(),
				persistentClass.hasIdentifierProperty(),
				persistentClass.isVersioned(),
				javaType,
				superType,
				jpaMetamodel
		);
	}

	public EntityTypeImpl(JavaType<J> javaTypeDescriptor, JpaMetamodelImplementor jpaMetamodel) {
		super(
				javaTypeDescriptor.getJavaTypeClass().getName(),
				javaTypeDescriptor,
				null,
				false,
				false,
				false,
				jpaMetamodel
		);

		this.jpaEntityName = javaTypeDescriptor.getJavaTypeClass().getName();
		this.discriminatorPathSource = null;
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
			if ( hasIdClass() ) {
				return getIdentifierDescriptor();
			}
		}

		if ( EntityDiscriminatorMapping.matchesRoleName( name ) ) {
			return discriminatorPathSource;
		}

		return null;
	}

	@Override
	public PersistentAttribute<? super J, ?> findAttribute(String name) {
		final PersistentAttribute<? super J, ?> attribute = super.findAttribute( name );
		if ( attribute != null ) {
			return attribute;
		}

		if ( "id".equalsIgnoreCase( name ) || EntityIdentifierMapping.ROLE_LOCAL_NAME.equals( name ) ) {
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
	public PersistenceType getPersistenceType() {
		return PersistenceType.ENTITY;
	}

	@Override
	public IdentifiableDomainType<? super J> getSuperType() {
		return super.getSuperType();
	}

	@Override
	public Collection<? extends EntityDomainType<? extends J>> getSubTypes() {
		//noinspection unchecked
		return (Collection<? extends EntityDomainType<? extends J>>) super.getSubTypes();
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
	public SqmPath<J> createSqmPath(SqmPath<?> lhs, SqmPathSource<?> intermediatePathSource) {
		throw new UnsupportedMappingException(
				"EntityType cannot be used to create an SqmPath - that would be an SqmFrom which are created directly"
		);
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Serialization

	protected Object writeReplace() throws ObjectStreamException {
		return new SerialForm( jpaMetamodel(), getHibernateEntityName() );
	}

	private static class SerialForm implements Serializable {
		private final JpaMetamodel jpaMetamodel;
		private final String hibernateEntityName;

		public SerialForm(JpaMetamodel jpaMetamodel, String hibernateEntityName) {
			this.jpaMetamodel = jpaMetamodel;
			this.hibernateEntityName = hibernateEntityName;
		}

		private Object readResolve() {
			return jpaMetamodel.entity( hibernateEntityName );
		}

	}
}
