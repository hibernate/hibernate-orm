/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.type.descriptor.converter.spi.BasicValueConverter;
import org.hibernate.type.descriptor.java.JavaType;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Steve Ebersole
 * @author Gavin King
 */
public abstract class DiscriminatorConverter<O,R> implements BasicValueConverter<O,R> {

	private final String discriminatorName;
	private final JavaType<O> domainJavaType;
	private final JavaType<R> relationalJavaType;

	public DiscriminatorConverter(
			String discriminatorName,
			JavaType<O> domainJavaType,
			JavaType<R> relationalJavaType) {
		this.discriminatorName = discriminatorName;
		this.domainJavaType = domainJavaType;
		this.relationalJavaType = relationalJavaType;
	}

	public String getDiscriminatorName() {
		return discriminatorName;
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

	public abstract DiscriminatorValueDetails getDetailsForDiscriminatorValue(Object relationalForm);

	public abstract DiscriminatorValueDetails getDetailsForEntityName(String entityName);

	@Override
	public String toString() {
		return "DiscriminatorConverter(" + discriminatorName + ")";
	}

	public abstract void forEachValueDetail(Consumer<DiscriminatorValueDetails> consumer);

	public abstract <X> X fromValueDetails(Function<DiscriminatorValueDetails,X> handler);
}
