/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.Type;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.internal.SubGraphImpl;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.Bindable;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.metamodel.model.domain.AbstractManagedType;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.EntityDomainType;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.spi.EmbeddableRepresentationStrategy;
import org.hibernate.metamodel.spi.ManagedTypeRepresentationStrategy;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.property.access.internal.PropertyAccessStrategyMixedImpl;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.sql.ast.Clause;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Standard Hibernate implementation of JPA's {@link javax.persistence.metamodel.EmbeddableType}
 * contract
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole`
 */
public class EmbeddableTypeImpl<J>
		extends AbstractManagedType<J>
		implements EmbeddableDomainType<J>, Serializable, MappingModelExpressable<J> {

	private final boolean isDynamic;
	private final EmbeddableRepresentationStrategy representationStrategy;

	public EmbeddableTypeImpl(
			JavaTypeDescriptor<J> javaTypeDescriptor,
			EmbeddableRepresentationStrategy representationStrategy,
			boolean isDynamic,
			JpaMetamodel domainMetamodel) {
		super( javaTypeDescriptor.getJavaType().getTypeName(), javaTypeDescriptor, null, domainMetamodel );
		this.representationStrategy = representationStrategy;
		this.isDynamic = isDynamic;
	}

	public EmbeddableTypeImpl(
			String name,
			JpaMetamodel domainMetamodel) {
		//noinspection unchecked
		super(
				name,
				(JavaTypeDescriptor) domainMetamodel.getTypeConfiguration()
						.getJavaTypeDescriptorRegistry()
						.getDescriptor( Map.class ),
				null,
				domainMetamodel
		);

		// todo (6.0) : need ManagedTypeRepresentationStrategy impls
		throw new NotYetImplementedFor6Exception( getClass() );
	}

	@Override
	public ManagedTypeRepresentationStrategy getRepresentationStrategy() {
		return representationStrategy;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.EMBEDDABLE;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <S extends J> SubGraphImplementor<S> makeSubGraph(Class<S> subType) {
		return new SubGraphImpl( this, true, jpaMetamodel() );
	}

	@Override
	public int forEachJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		final Object[] disassemble = (Object[]) disassemble( value, session );
		return forEachJdbcType(
				(i, jdbcMapping) -> {
					valuesConsumer.consume( i + offset, disassemble[i], jdbcMapping );
				}
		);
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		final MappingMetamodel metamodel = jpaMetamodel().getTypeConfiguration().getSessionFactory().getMetamodel();
		int i = 0;
		for ( Attribute<? super J, ?> attribute : getAttributes() ) {
			if ( attribute instanceof SingularAttribute<?, ?> ) {
				Type<?> type = ( (SingularAttribute<? super J, ?>) attribute ).getType();
				switch ( type.getPersistenceType() ) {
					case BASIC:
						BasicType<?> basicType = jpaMetamodel().getTypeConfiguration()
								.getBasicTypeForJavaType( attribute.getJavaType() );
						action.accept( i + offset, basicType.getJdbcMapping() );
						i++;
						break;
					case ENTITY:
						final EntityPersister entityDescriptor = metamodel.getEntityDescriptor(
								( (EntityDomainType<?>) type ).getHibernateEntityName()
						);
						i += entityDescriptor.getEntityMappingType().getIdentifierMapping().forEachJdbcType(
								i + offset,
								action
						);
						break;
					case EMBEDDABLE:
						i += ( (Bindable) type ).forEachJdbcType( i + offset, action );
						break;
					default:
						throw new IllegalArgumentException( "Unsupported type: " + type );
				}
			}
		}
		return i;
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		final Set<Attribute<? super J, ?>> attributes = getAttributes();
		final List<Object> result = new ArrayList<>();
		for ( Attribute attribute : attributes ) {
			if ( attribute instanceof SingularAttributeImpl ) {
				final PropertyAccess propertyAccess = PropertyAccessStrategyMixedImpl.INSTANCE
						.buildPropertyAccess( getJavaType(), attribute.getName() );

				final Object attributeValue = propertyAccess.getGetter().get( value );
				if ( attribute.isAssociation() ) {
					final EntityPersister entityDescriptor = session.getFactory().getMetamodel()
							.findEntityDescriptor( attribute.getJavaType().getName() );
					final Object disassembled = entityDescriptor.getIdentifierMapping().disassemble(
							attributeValue,
							session
					);
					if ( disassembled instanceof Object[] ) {
						for ( Object o : (Object[]) disassembled ) {
							result.add( o );
						}
					}
					else {
						result.add( disassembled );
					}
				}
				else {
					result.add( attributeValue );
				}
			}
		}
		return result.toArray();
	}

}
