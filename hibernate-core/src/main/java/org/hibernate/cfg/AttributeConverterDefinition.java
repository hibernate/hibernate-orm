/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.boot.AttributeConverterInfo;
import org.hibernate.boot.model.convert.internal.ConverterHelper;
import org.hibernate.boot.model.convert.internal.InstanceBasedConverterDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.util.GenericsHelper;

/**
 * Externalized representation of an AttributeConverter
 *
 * @author Steve Ebersole
 *
 * @deprecated forces the converter instance to be built too early,
 * which precludes the ability to resolve them from CDI, etc.  See
 * {@link ConverterDescriptor} instead
 */
@Deprecated(since = "5.3")
public class AttributeConverterDefinition implements AttributeConverterInfo {
	private final AttributeConverter attributeConverter;
	private final boolean autoApply;
	private final Class entityAttributeType;
	private final Class databaseColumnType;

	/**
	 * Build an AttributeConverterDefinition from the AttributeConverter Class reference and
	 * whether or not to auto-apply it.
	 *
	 * @param attributeConverterClass The AttributeConverter Class
	 * @param autoApply Should the AttributeConverter be auto-applied?
	 *
	 * @return The constructed definition
	 */
	public static AttributeConverterDefinition from(Class<? extends AttributeConverter> attributeConverterClass, boolean autoApply) {
		return new AttributeConverterDefinition(
				instantiateAttributeConverter( attributeConverterClass ),
				autoApply
		);
	}

	private static AttributeConverter instantiateAttributeConverter(Class<? extends AttributeConverter> attributeConverterClass) {
		try {
			Constructor<? extends AttributeConverter> constructor = attributeConverterClass.getDeclaredConstructor();
			constructor.setAccessible( true );
			return constructor.newInstance();
		}
		catch (Exception e) {
			throw new AnnotationException(
					"Unable to instantiate AttributeConverter [" + attributeConverterClass.getName() + "]",
					e
			);
		}
	}

	/**
	 * Build an AttributeConverterDefinition from the AttributeConverter Class reference.  The
	 * converter is searched for a {@link Converter} annotation	 to determine whether it should
	 * be treated as auto-apply.  If the annotation is present, {@link Converter#autoApply()} is
	 * used to make that determination.  If the annotation is not present, {@code false} is assumed.
	 *
	 * @param attributeConverterClass The converter class
	 *
	 * @return The constructed definition
	 */
	public static AttributeConverterDefinition from(Class<? extends AttributeConverter> attributeConverterClass) {
		return from( instantiateAttributeConverter( attributeConverterClass ) );
	}

	/**
	 * Build an AttributeConverterDefinition from an AttributeConverter instance.  The
	 * converter is searched for a {@link Converter} annotation	 to determine whether it should
	 * be treated as auto-apply.  If the annotation is present, {@link Converter#autoApply()} is
	 * used to make that determination.  If the annotation is not present, {@code false} is assumed.
	 *
	 * @param attributeConverter The AttributeConverter instance
	 *
	 * @return The constructed definition
	 */
	public static AttributeConverterDefinition from(AttributeConverter attributeConverter) {
		boolean autoApply = false;
		Converter converterAnnotation = attributeConverter.getClass().getAnnotation( Converter.class );
		if ( converterAnnotation != null ) {
			autoApply = converterAnnotation.autoApply();
		}

		return new AttributeConverterDefinition( attributeConverter, autoApply );
	}

	/**
	 * Build an AttributeConverterDefinition from the AttributeConverter instance and
	 * whether or not to auto-apply it.
	 *
	 * @param attributeConverter The AttributeConverter instance
	 * @param autoApply Should the AttributeConverter be auto-applied?
	 *
	 * @return The constructed definition
	 */
	public static AttributeConverterDefinition from(AttributeConverter attributeConverter, boolean autoApply) {
		return new AttributeConverterDefinition( attributeConverter, autoApply );
	}

	public AttributeConverterDefinition(AttributeConverter attributeConverter, boolean autoApply) {
		this.attributeConverter = attributeConverter;
		this.autoApply = autoApply;

		final Class attributeConverterClass = attributeConverter.getClass();
		final ParameterizedType attributeConverterSignature = ConverterHelper.extractAttributeConverterParameterizedType( attributeConverterClass );
		if ( attributeConverterSignature == null ) {
			throw new AssertionFailure(
					"Could not extract ParameterizedType representation of AttributeConverter definition " +
							"from AttributeConverter implementation class [" + attributeConverterClass.getName() + "]"
			);
		}

		if ( attributeConverterSignature.getActualTypeArguments().length < 2 ) {
			throw new AnnotationException(
					"AttributeConverter [" + attributeConverterClass.getName()
							+ "] did not retain parameterized type information"
			);
		}

		if ( attributeConverterSignature.getActualTypeArguments().length > 2 ) {
			throw new AnnotationException(
					"AttributeConverter [" + attributeConverterClass.getName()
							+ "] specified more than 2 parameterized types"
			);
		}
		entityAttributeType = GenericsHelper.extractClass( attributeConverterSignature.getActualTypeArguments()[0] );
		if ( entityAttributeType == null ) {
			throw new AnnotationException(
					"Could not determine 'entity attribute' type from given AttributeConverter [" +
							attributeConverterClass.getName() + "]"
			);
		}

		databaseColumnType = GenericsHelper.extractClass(attributeConverterSignature.getActualTypeArguments()[1]);
		if ( databaseColumnType == null ) {
			throw new AnnotationException(
					"Could not determine 'database column' type from given AttributeConverter [" +
							attributeConverterClass.getName() + "]"
			);
		}
	}

	public AttributeConverter getAttributeConverter() {
		return attributeConverter;
	}

	public boolean isAutoApply() {
		return autoApply;
	}

	public Class getEntityAttributeType() {
		return entityAttributeType;
	}

	public Class getDatabaseColumnType() {
		return databaseColumnType;
	}

	@Override
	public Class<? extends AttributeConverter> getConverterClass() {
		return attributeConverter.getClass();
	}

	@Override
	public String toString() {
		return String.format(
				"%s[converterClass=%s, domainType=%s, jdbcType=%s]",
				this.getClass().getName(),
				attributeConverter.getClass().getName(),
				entityAttributeType.getName(),
				databaseColumnType.getName()
		);
	}

	@Override
	public ConverterDescriptor toConverterDescriptor(MetadataBuildingContext context) {
		return new InstanceBasedConverterDescriptor(
				getAttributeConverter(),
				isAutoApply(),
				context.getBootstrapContext().getClassmateContext()
		);
	}
}
