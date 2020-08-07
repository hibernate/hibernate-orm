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
import java.util.function.Consumer;
import javax.persistence.metamodel.Attribute;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.internal.SubGraphImpl;
import org.hibernate.graph.spi.SubGraphImplementor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.metamodel.model.domain.AbstractManagedType;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.JpaMetamodel;
import org.hibernate.metamodel.spi.EmbeddableRepresentationStrategy;
import org.hibernate.metamodel.spi.ManagedTypeRepresentationStrategy;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.property.access.internal.PropertyAccessStrategyMixedImpl;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.sql.ast.Clause;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

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

	private final EmbeddableRepresentationStrategy representationStrategy;

	public EmbeddableTypeImpl(
			JavaTypeDescriptor<J> javaTypeDescriptor,
			EmbeddableRepresentationStrategy representationStrategy,
			JpaMetamodel domainMetamodel) {
		super( javaTypeDescriptor.getJavaType().getName(), javaTypeDescriptor, null, domainMetamodel );
		this.representationStrategy = representationStrategy;
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
	public void visitJdbcValues(
			Object value, Clause clause, JdbcValuesConsumer valuesConsumer, SharedSessionContractImplementor session) {
		Object[] disassemble = (Object[]) disassemble( value, session );

		List<JdbcMapping> jdbcMappings = getJdbcMappings( session.getFactory().getTypeConfiguration() );
		for ( int i = 0; i < disassemble.length; i++ ) {
			valuesConsumer.consume( disassemble[i], jdbcMappings.get( i ) );
		}
	}

	@Override
	public void visitJdbcTypes(
			Consumer<JdbcMapping> action,
			Clause clause,
			TypeConfiguration typeConfiguration) {
		Set<Attribute<? super J, ?>> attributes = getAttributes();

		for ( Attribute attribute : attributes ) {
			if ( attribute instanceof SingularAttributeImpl ) {
				if ( attribute.isAssociation() ) {
					EntityTypeImpl entityType = (EntityTypeImpl) ( (SingularAttributeImpl) attribute ).getValueGraphType();

					BasicType basicType = jpaMetamodel().getTypeConfiguration()
							.getBasicTypeForJavaType( entityType.findIdAttribute().getJavaType() );
					action.accept( basicType.getJdbcMapping() );
				}
				else {
					BasicType basicType = jpaMetamodel().getTypeConfiguration()
							.getBasicTypeForJavaType( attribute.getJavaType() );
					action.accept( basicType.getJdbcMapping() );
				}
			}
		}
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
