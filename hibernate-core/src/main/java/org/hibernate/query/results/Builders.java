/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results;

import java.util.Locale;
import javax.persistence.AttributeConverter;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.LockMode;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.RuntimeMetamodels;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class Builders {
	public static ScalarResultBuilder scalar(String columnAlias) {
		return new StandardScalarResultBuilder( columnAlias );
	}

	public static ScalarResultBuilder scalar(
			String columnAlias,
			BasicType<?> type) {
		return new StandardScalarResultBuilder( columnAlias, type );
	}

	public static ScalarResultBuilder scalar(
			String columnAlias,
			Class<?> javaType,
			SessionFactoryImplementor factory) {
		final JavaTypeDescriptor<?> javaTypeDescriptor = factory.getTypeConfiguration()
				.getJavaTypeDescriptorRegistry()
				.getDescriptor( javaType );

		return new StandardScalarResultBuilder( columnAlias, javaTypeDescriptor );
	}

	public static <C> ResultBuilder scalar(
			String columnAlias,
			Class<C> relationalJavaType,
			AttributeConverter<?, C> converter,
			SessionFactoryImplementor sessionFactory) {
		return ConvertedResultBuilder.from( columnAlias, relationalJavaType, converter, sessionFactory );
	}

	public static <C> ResultBuilder scalar(
			String columnAlias,
			Class<C> relationalJavaType,
			Class<? extends AttributeConverter<?, C>> converterJavaType,
			SessionFactoryImplementor sessionFactory) {
		return ConvertedResultBuilder.from( columnAlias, relationalJavaType, converterJavaType, sessionFactory );
	}

	public static ScalarResultBuilder scalar(int position) {
		// will be needed for interpreting legacy HBM <resultset/> mappings
		throw new NotYetImplementedFor6Exception();
	}

	public static ScalarResultBuilder scalar(int position, BasicType<?> type) {
		// will be needed for interpreting legacy HBM <resultset/> mappings
		throw new NotYetImplementedFor6Exception();
	}

	public static ResultBuilder attributeResult(
			String columnAlias,
			String entityName,
			String attributePath,
			SessionFactoryImplementor sessionFactory) {
		if ( attributePath.contains( "." ) ) {
			throw new NotYetImplementedFor6Exception(
					"Support for defining a NativeQuery attribute result based on a composite path is not yet implemented"
			);
		}

		final RuntimeMetamodels runtimeMetamodels = sessionFactory.getRuntimeMetamodels();
		final String fullEntityName = runtimeMetamodels.getMappingMetamodel().getImportedName( entityName );
		final EntityPersister entityMapping = runtimeMetamodels.getMappingMetamodel().findEntityDescriptor( fullEntityName );
		if ( entityMapping == null ) {
			throw new IllegalArgumentException( "Could not locate entity mapping : " + fullEntityName );
		}

		final AttributeMapping attributeMapping = entityMapping.findAttributeMapping( attributePath );
		if ( attributeMapping == null ) {
			throw new IllegalArgumentException( "Could not locate attribute mapping : " + fullEntityName + "." + attributePath );
		}

		if ( attributeMapping instanceof SingularAttributeMapping ) {
			final SingularAttributeMapping singularAttributeMapping = (SingularAttributeMapping) attributeMapping;
			return new AttributeResultBuilder( singularAttributeMapping, columnAlias, fullEntityName, attributePath );
		}

		throw new IllegalArgumentException(
				String.format(
						Locale.ROOT,
						"Specified attribute mapping [%s.%s] not a basic attribute: %s",
						fullEntityName,
						attributePath,
						attributeMapping
				)
		);
	}

	public static ResultBuilder attributeResult(String columnAlias, SingularAttribute<?, ?> attribute) {
		if ( ! ( attribute.getDeclaringType() instanceof EntityType ) ) {
			throw new NotYetImplementedFor6Exception(
					"Support for defining a NativeQuery attribute result based on a composite path is not yet implemented"
			);
		}

		throw new NotYetImplementedFor6Exception();
	}

	/**
	 * Creates a EntityResultBuilder allowing for further configuring of the mapping.
	 *
	 * @param tableAlias
	 * @param entityName
	 * @return
	 */
	public static EntityResultBuilder entity(String tableAlias, String entityName) {
		throw new NotYetImplementedFor6Exception( );
	}

	/**
	 * Creates a EntityResultBuilder that does not allow any further configuring of the mapping.
	 *
	 * @see org.hibernate.query.NativeQuery#addEntity(Class)
	 * @see org.hibernate.query.NativeQuery#addEntity(String)
	 * @see org.hibernate.query.NativeQuery#addEntity(String, Class)
	 * @see org.hibernate.query.NativeQuery#addEntity(String, String)
	 */
	public static CalculatedEntityResultBuilder entityCalculated(String tableAlias, String entityName) {
		return entityCalculated( tableAlias, entityName, null );
	}

	/**
	 * Creates a EntityResultBuilder that does not allow any further configuring of the mapping.
	 *
	 * @see #entityCalculated(String, String)
	 * @see org.hibernate.query.NativeQuery#addEntity(String, Class, LockMode)
	 * @see org.hibernate.query.NativeQuery#addEntity(String, String, LockMode)
	 */
	public static CalculatedEntityResultBuilder entityCalculated(
			String tableAlias,
			String entityName,
			LockMode explicitLockMode) {
		return new CalculatedEntityResultBuilder( tableAlias, entityName, explicitLockMode );
	}

	public static LegacyFetchBuilder fetch(String tableAlias, String ownerTableAlias, String joinPropertyName) {
		throw new NotYetImplementedFor6Exception( );
	}
}
