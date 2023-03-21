/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.metamodel.mapping;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.mapping.internal.DiscriminatorValueDetailsImpl;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.JavaType;

import static org.hibernate.persister.entity.DiscriminatorHelper.NOT_NULL_DISCRIMINATOR;
import static org.hibernate.persister.entity.DiscriminatorHelper.NULL_DISCRIMINATOR;

/**
 * Conversion of discriminator values between the entity name/Class domain form and
 * its generally CHARACTER or INTEGER based relational form
 *
 * @param <O> The domain type - either <ul>
 *     <li>
 *         the {@linkplain EntityMappingType#getMappedJavaType() entity Class} for unnamed entities
 *     </li>
 *     <li>
 *         the {@linkplain EntityMappingType#getEntityName() entity name} for named entities
 *     </li>
 * </ul>
 * @param <R> The Java type of the relational form of the discriminator
 *
 * @author Steve Ebersole
 */
public class DiscriminatorConverter<O,R> implements BasicValueConverter<O,R> {
	public static <O,R> DiscriminatorConverter<O,R> fromValueMappings(
			NavigableRole role,
			JavaType<O> domainJavaType,
			BasicType<R> underlyingJdbcMapping,
			Map<Object,String> valueMappings,
			SessionFactoryImplementor sessionFactory) {
		final MappingMetamodelImplementor mappingMetamodel = sessionFactory.getMappingMetamodel();
		final List<DiscriminatorValueDetails> valueDetailsList = CollectionHelper.arrayList( valueMappings.size() );
		valueMappings.forEach( (value, entityName) -> {
			final DiscriminatorValueDetails valueDetails = new DiscriminatorValueDetailsImpl(
					value,
					mappingMetamodel.getEntityDescriptor( entityName )
			);
			valueDetailsList.add( valueDetails );
		} );

		return new DiscriminatorConverter<>(
				role,
				domainJavaType,
				underlyingJdbcMapping.getJavaTypeDescriptor(),
				valueDetailsList
		);
	}

	private final NavigableRole discriminatorRole;
	private final JavaType<O> domainJavaType;
	private final JavaType<R> relationalJavaType;

	private final Map<Object, DiscriminatorValueDetails> discriminatorValueToEntityNameMap;
	private final Map<String,DiscriminatorValueDetails> entityNameToDiscriminatorValueMap;

	public DiscriminatorConverter(
			NavigableRole discriminatorRole,
			JavaType<O> domainJavaType,
			JavaType<R> relationalJavaType,
			List<DiscriminatorValueDetails> valueMappings) {
		this.discriminatorRole = discriminatorRole;
		this.domainJavaType = domainJavaType;
		this.relationalJavaType = relationalJavaType;

		this.discriminatorValueToEntityNameMap = CollectionHelper.concurrentMap( valueMappings.size() );
		this.entityNameToDiscriminatorValueMap = CollectionHelper.concurrentMap( valueMappings.size() );
		valueMappings.forEach( (valueDetails) -> {
			discriminatorValueToEntityNameMap.put( valueDetails.getValue(), valueDetails );
			entityNameToDiscriminatorValueMap.put( valueDetails.getIndicatedEntityName(), valueDetails );
		} );
	}

	public NavigableRole getNavigableRole() {
		return discriminatorRole;
	}

	@Override
	public JavaType<O> getDomainJavaType() {
		return domainJavaType;
	}

	@Override
	public JavaType<R> getRelationalJavaType() {
		return relationalJavaType;
	}

	public DiscriminatorValueDetails getDetailsForRelationalForm(R relationalForm) {
		return getDetailsForDiscriminatorValue( relationalForm );
	}

	@Override
	public O toDomainValue(R relationalForm) {
		assert relationalForm == null || relationalJavaType.isInstance( relationalForm );

		final DiscriminatorValueDetails matchingValueDetails = getDetailsForRelationalForm( relationalForm );
		if ( matchingValueDetails == null ) {
			throw new IllegalStateException( "Could not resolve discriminator value" );
		}

		final EntityMappingType indicatedEntity = matchingValueDetails.getIndicatedEntity();
		//noinspection unchecked
		return indicatedEntity.getRepresentationStrategy().getMode() == RepresentationMode.POJO
				&& indicatedEntity.getEntityName().equals( indicatedEntity.getJavaType().getJavaTypeClass().getName() )
				? (O) indicatedEntity.getJavaType().getJavaTypeClass()
				: (O) indicatedEntity.getEntityName();
	}

	public DiscriminatorValueDetails getDetailsForEntityName(String entityName) {
		return entityNameToDiscriminatorValueMap.get( entityName );
	}

	public DiscriminatorValueDetails getDetailsForDiscriminatorValue(Object value) {
		if ( value == null ) {
			return discriminatorValueToEntityNameMap.get( NULL_DISCRIMINATOR );
		}

		final DiscriminatorValueDetails valueMatch = discriminatorValueToEntityNameMap.get( value );
		if ( valueMatch != null ) {
			return valueMatch;
		}

		return discriminatorValueToEntityNameMap.get( NOT_NULL_DISCRIMINATOR );
	}

	@Override
	public R toRelationalValue(O domainForm) {
		assert domainForm == null || domainForm instanceof String || domainForm instanceof Class;

		if ( domainForm == null ) {
			return null;
		}

		final String entityName;
		if ( domainForm instanceof Class ) {
			entityName = ( (Class<?>) domainForm ).getName();
		}
		else {
			entityName = (String) domainForm;
		}

		final DiscriminatorValueDetails discriminatorValueDetails = getDetailsForEntityName( entityName );
		//noinspection unchecked
		return (R) discriminatorValueDetails.getValue();
	}

	@Override
	public String toString() {
		return "DiscriminatorConverter(" + discriminatorRole.getFullPath() + ")";
	}

	public void forEachValueDetail(Consumer<DiscriminatorValueDetails> consumer) {
		discriminatorValueToEntityNameMap.forEach( (value, detail) -> consumer.accept( detail ) );
	}

	public <X> X fromValueDetails(Function<DiscriminatorValueDetails,X> handler) {
		for ( DiscriminatorValueDetails detail : discriminatorValueToEntityNameMap.values() ) {
			final X result = handler.apply( detail );
			if ( result != null ) {
				return result;
			}
		}
		return null;
	}
}
