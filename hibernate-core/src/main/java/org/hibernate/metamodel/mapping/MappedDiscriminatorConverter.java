/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.internal.DiscriminatorValueDetailsImpl;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.CharacterJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.StringJavaType;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

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
public class MappedDiscriminatorConverter<O,R> extends DiscriminatorConverter<O,R> {

	public static <O,R> MappedDiscriminatorConverter<O,R> fromValueMappings(
			NavigableRole role,
			JavaType<O> domainJavaType,
			BasicType<R> underlyingJdbcMapping,
			Map<Object,String> valueMappings,
			MappingMetamodelImplementor mappingMetamodel) {
		final List<DiscriminatorValueDetails> valueDetailsList = CollectionHelper.arrayList( valueMappings.size() );
		valueMappings.forEach( (value, entityName) -> {
			final DiscriminatorValueDetails valueDetails = new DiscriminatorValueDetailsImpl(
					value,
					mappingMetamodel.getEntityDescriptor( entityName )
			);
			valueDetailsList.add( valueDetails );
		} );

		return new MappedDiscriminatorConverter<>(
				role,
				domainJavaType,
				underlyingJdbcMapping.getJavaTypeDescriptor(),
				valueDetailsList
		);
	}

	private final Map<Object, DiscriminatorValueDetails> discriminatorValueToEntityNameMap;
	private final Map<String,DiscriminatorValueDetails> entityNameToDiscriminatorValueMap;

	public MappedDiscriminatorConverter(
			NavigableRole discriminatorRole,
			JavaType<O> domainJavaType,
			JavaType<R> relationalJavaType,
			List<DiscriminatorValueDetails> valueMappings) {
		super( discriminatorRole.getFullPath(), domainJavaType, relationalJavaType );

		this.discriminatorValueToEntityNameMap = CollectionHelper.concurrentMap( valueMappings.size() );
		this.entityNameToDiscriminatorValueMap = CollectionHelper.concurrentMap( valueMappings.size() );
		valueMappings.forEach( (valueDetails) -> {
			discriminatorValueToEntityNameMap.put( valueDetails.getValue(), valueDetails );
			entityNameToDiscriminatorValueMap.put( valueDetails.getIndicatedEntityName(), valueDetails );
		} );
	}

	@Override
	public DiscriminatorValueDetails getDetailsForRelationalForm(R relationalForm) {
		return getDetailsForDiscriminatorValue( relationalForm );
	}

	@Override
	public DiscriminatorValueDetails getDetailsForEntityName(String entityName) {
		DiscriminatorValueDetails valueDetails = entityNameToDiscriminatorValueMap.get( entityName );
		if ( valueDetails!= null) {
			return valueDetails;
		}

		throw new AssertionFailure( "Unrecognized entity name: " + entityName );
	}

	@Override
	public DiscriminatorValueDetails getDetailsForDiscriminatorValue(Object value) {
		if ( value == null ) {
			return discriminatorValueToEntityNameMap.get( NULL_DISCRIMINATOR );
		}

		final DiscriminatorValueDetails valueMatch = discriminatorValueToEntityNameMap.get( value );
		if ( valueMatch != null ) {
			return valueMatch;
		}

		final DiscriminatorValueDetails notNullMatch = discriminatorValueToEntityNameMap.get( NOT_NULL_DISCRIMINATOR );
		if ( notNullMatch != null ) {
			return notNullMatch;
		}

		if ( value.getClass().isEnum() ) {
			final Object enumValue;
			if ( getRelationalJavaType() instanceof StringJavaType ) {
				enumValue = ( (Enum) value ).name();
			}
			else if ( getRelationalJavaType() instanceof CharacterJavaType ) {
				enumValue = ( (Enum) value ).name().charAt( 0 );
			}
			else {
				enumValue = ( (Enum) value ).ordinal();
			}
			final DiscriminatorValueDetails enumMatch = discriminatorValueToEntityNameMap.get( enumValue );
			if ( enumMatch != null ) {
				return enumMatch;
			}
		}

		throw new HibernateException( "Unrecognized discriminator value: " + value );
	}

	@Override
	public void forEachValueDetail(Consumer<DiscriminatorValueDetails> consumer) {
		discriminatorValueToEntityNameMap.forEach( (value, detail) -> consumer.accept( detail ) );
	}

	@Override
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
