/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.metamodel.RepresentationMode;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.BasicType;
import org.hibernate.type.MetaType;
import org.hibernate.type.descriptor.java.ClassJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

public class AnyDiscriminatorConverter implements BasicValueConverter<Class, Object> {
	private final MetaType modelPart;
	private BasicType discriminatorBasicType;
	private TypeConfiguration typeConfiguration;

	public AnyDiscriminatorConverter(
			MetaType modelPart, BasicType discriminatorBasicType,
			TypeConfiguration typeConfiguration) {
		this.modelPart = modelPart;
		this.discriminatorBasicType = discriminatorBasicType;
		this.typeConfiguration = typeConfiguration;
	}

	@Override
	public Class toDomainValue(Object discriminatorValue) {
		if ( discriminatorValue == null ) {
			return null;
		}

		final String entityName = modelPart.getDiscriminatorValuesToEntityNameMap().get( discriminatorValue );
		final EntityPersister entityDescriptor = typeConfiguration.getSessionFactory()
				.getRuntimeMetamodels()
				.getMappingMetamodel()
				.getEntityDescriptor( entityName );
		assert entityDescriptor.getRepresentationStrategy().getMode() == RepresentationMode.POJO;
		return entityDescriptor.getJavaType().getJavaTypeClass();
	}

	@Override
	public Object toRelationalValue(Class domainForm) {
		if ( domainForm == null ) {
			return null;
		}

		return modelPart.getEntityNameToDiscriminatorValueMap().get( ( domainForm ).getName() );
	}

	@Override
	public JavaType<Class> getDomainJavaType() {
		return getExpressibleJavaType();
	}

	@Override
	public JavaType<Object> getRelationalJavaType() {
		return discriminatorBasicType.getJavaTypeDescriptor();
	}

	public JavaType<Class> getExpressibleJavaType() {
		return ClassJavaType.INSTANCE;
	}

}
