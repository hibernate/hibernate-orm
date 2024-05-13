/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.metamodel.mapping.internal.DiscriminatorValueDetailsImpl;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.metamodel.spi.MappingMetamodelImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaType;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Implements the default discriminator assignment strategy defined by JPA,
 * that is, the discriminator value is the JPA (unqualified) entity name.
 * This strategy is used when no explicit discriminator mapping is specified.
 *
 * @author Gavin King
 */
public class DefaultDiscriminatorConverter<O,R> extends DiscriminatorConverter<O,R> {

	public static <O,R> DefaultDiscriminatorConverter<O,R> fromMappingMetamodel(
			NavigableRole role,
			JavaType<O> domainJavaType,
			BasicType<R> underlyingJdbcMapping,
			MappingMetamodelImplementor mappingMetamodel) {
		return new DefaultDiscriminatorConverter<>(
				role,
				domainJavaType,
				underlyingJdbcMapping.getJavaTypeDescriptor(),
				mappingMetamodel
		);
	}

	private final MappingMetamodelImplementor mappingMetamodel;

	public DefaultDiscriminatorConverter(
			NavigableRole discriminatorRole,
			JavaType<O> domainJavaType,
			JavaType<R> relationalJavaType,
			MappingMetamodelImplementor mappingMetamodel) {
		super( discriminatorRole.getFullPath(), domainJavaType, relationalJavaType );
		this.mappingMetamodel = mappingMetamodel;
	}

	@Override
	public DiscriminatorValueDetails getDetailsForRelationalForm(R relationalForm) {
		return getDetailsForDiscriminatorValue( relationalForm );
	}

	@Override
	public DiscriminatorValueDetails getDetailsForEntityName(String entityName) {
		EntityPersister persister = mappingMetamodel.findEntityDescriptor( entityName );
		if ( persister!= null ) {
			return new DiscriminatorValueDetailsImpl( entityName, persister );
		}

		throw new AssertionFailure( "Unrecognized entity name: " + entityName );
	}

	@Override
	public DiscriminatorValueDetails getDetailsForDiscriminatorValue(Object value) {
		if ( value instanceof String ) {
			String entityName = mappingMetamodel.getImportedName( (String) value );
			EntityPersister persister = mappingMetamodel.findEntityDescriptor( entityName );
			if ( persister!= null ) {
				return new DiscriminatorValueDetailsImpl( entityName, persister );
			}
		}

		throw new HibernateException( "Unrecognized discriminator value: " + value );
	}

	@Override
	public void forEachValueDetail(Consumer<DiscriminatorValueDetails> consumer) {
	}

	@Override
	public <X> X fromValueDetails(Function<DiscriminatorValueDetails,X> handler) {
		return null;
	}
}
