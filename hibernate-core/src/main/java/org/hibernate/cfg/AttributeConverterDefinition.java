/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

import java.util.List;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.hibernate.AnnotationException;
import org.hibernate.boot.internal.ClassmateContext;

import com.fasterxml.classmate.ResolvedType;

/**
 * Representation of an {@link AttributeConverter} from externalized sources.  Generally
 * speaking these are contributed from:<ul>
 *     <li>converters discovered via {@link Converter} discovery</li>
 *     <li>application / integration contributions - {@link org.hibernate.boot.MetadataBuilder#applyAttributeConverter}</li>
 * </ul>
 * <p/>
 * Regardless of how they are known, the set of AttributeConverterDefinition instances
 * as known to {@link org.hibernate.boot.spi.MetadataBuildingOptions#getAttributeConverters()}
 * represents the complete set of "a priori converters".  After that point the only additional
 * converters recognized would come from local {@link javax.persistence.Convert} annotations.
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.boot.spi.AttributeConverterDescriptor
 *
 * @param <O> The Java type in the application's domain model.  Labelled "O" to
 * refer to the "O" portion of "ORM"
 * @param <R> The Java type used to represent these converted values on the JDBC
 * side. Labelled "R" to refer to the "R" portion of "ORM".
 */
public class AttributeConverterDefinition<O,R> {

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Static factory methods

	/**
	 * Build an AttributeConverterDefinition from the AttributeConverter Class reference and
	 * whether or not to auto-apply it.
	 * <p/>
	 * Skips looking for any {@link Converter} annotation to check {@link Converter#autoApply()}.
	 * The assumption is that check already having been done by the caller and the appropriate
	 * value being passed as the {@code autoApply} param.
	 *
	 * @param classmateContext Classmate type resolution context
	 * @param converterClass The AttributeConverter Class
	 *
	 * @return The constructed definition
	 */
	public static <O,R> AttributeConverterDefinition<O,R> from(
			ClassmateContext classmateContext,
			Class<? extends AttributeConverter<O,R>> converterClass) {
		return from( classmateContext, converterClass, determineAutoApply( converterClass ) );
	}

	@SuppressWarnings("SimplifiableIfStatement")
	private static boolean determineAutoApply(Class<? extends AttributeConverter> converterClass) {
		// look for the converter's @Converter annotation to resolve its Converter#autoApply value
		final Converter converterAnnotation = converterClass.getAnnotation( Converter.class );
		if ( converterAnnotation == null ) {
			// assume false in keeping with its defined default
			return false;
		}
		else {
			return converterAnnotation.autoApply();
		}
	}


	/**
	 * Build an AttributeConverterDefinition from the AttributeConverter Class reference and
	 * whether or not to auto-apply it.
	 * <p/>
	 * Skips looking for any {@link Converter} annotation to check {@link Converter#autoApply()}.
	 * The assumption is that check already having been done by the caller and the appropriate
	 * value being passed as the {@code autoApply} param.
	 *
	 * @param classmateContext Classmate type resolution context
	 * @param converterClass The AttributeConverter Class
	 * @param autoApply Should the AttributeConverter be auto-applied?
	 *
	 * @return The constructed definition
	 */
	public static <O,R> AttributeConverterDefinition<O,R> from(
			ClassmateContext classmateContext,
			Class<? extends AttributeConverter<O,R>> converterClass,
			boolean autoApply) {
		return from(
				classmateContext,
				instantiateAttributeConverter( converterClass ),
				autoApply
		);
	}

	private static <O,R> AttributeConverter<O,R> instantiateAttributeConverter(Class<? extends AttributeConverter<O,R>> attributeConverterClass) {
		try {
			return attributeConverterClass.newInstance();
		}
		catch (Exception e) {
			throw new AnnotationException(
					"Unable to instantiate AttributeConverter [" + attributeConverterClass.getName() + "]",
					e
			);
		}
	}

	/**
	 * Build an AttributeConverterDefinition from an AttributeConverter instance.  The
	 * converter is searched for a {@link Converter} annotation	 to determine whether it should
	 * be treated as auto-apply.  If the annotation is present, {@link Converter#autoApply()} is
	 * used to make that determination.  If the annotation is not present, {@code false} is assumed.
	 *
	 * @param classmateContext Classmate type resolution context
	 * @param attributeConverter The AttributeConverter instance
	 *
	 * @return The constructed definition
	 */
	public static <O,R> AttributeConverterDefinition<O,R> from(
			ClassmateContext classmateContext,
			AttributeConverter<O,R> attributeConverter) {
		return from( classmateContext, attributeConverter, determineAutoApply( attributeConverter.getClass() ) );
	}

	/**
	 * Build an AttributeConverterDefinition from the AttributeConverter instance and
	 * whether or not to auto-apply it.
	 *
	 * @param classmateContext Classmate type resolution context
	 * @param attributeConverter The AttributeConverter instance
	 * @param autoApply Should the AttributeConverter be auto-applied?
	 *
	 * @return The constructed definition
	 */
	public static <O,R> AttributeConverterDefinition<O,R> from(
			ClassmateContext classmateContext,
			AttributeConverter<O,R> attributeConverter,
			boolean autoApply) {
		final Class converterClass = attributeConverter.getClass();

		final ResolvedType converterType = classmateContext.getTypeResolver().resolve( converterClass );
		final List<ResolvedType> converterParamTypes = converterType.typeParametersFor( AttributeConverter.class );
		if ( converterParamTypes == null ) {
			throw new AnnotationException(
					"Could not extract type parameter information from AttributeConverter implementation ["
							+ converterClass.getName() + "]"
			);
		}
		else if ( converterParamTypes.size() != 2 ) {
			throw new AnnotationException(
					"Unexpected type parameter information for AttributeConverter implementation [" +
							converterClass.getName() + "]; expected 2 parameter types, but found " + converterParamTypes.size()
			);
		}

		return new AttributeConverterDefinition<>(
				attributeConverter,
				autoApply,
				converterParamTypes.get( 0 ),
				converterParamTypes.get( 1 )
		);
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Instance definition

	private final AttributeConverter<O,R> attributeConverter;
	private final boolean autoApply;
	private final ResolvedType domainType;
	private final ResolvedType jdbcType;

	public AttributeConverterDefinition(
			AttributeConverter<O, R> attributeConverter,
			boolean autoApply,
			ResolvedType domainType,
			ResolvedType jdbcType) {
		this.attributeConverter = attributeConverter;
		this.autoApply = autoApply;
		this.domainType = domainType;
		this.jdbcType = jdbcType;
	}

	public AttributeConverter getAttributeConverter() {
		return attributeConverter;
	}

	public boolean isAutoApply() {
		return autoApply;
	}

	public ResolvedType getDomainClassmateType() {
		return domainType;
	}

	@SuppressWarnings("unchecked")
	public Class<O> getDomainJavaType() {
		return (Class<O>) getDomainClassmateType().getErasedType();
	}

	public ResolvedType getJdbcClassmateType() {
		return jdbcType;
	}

	@SuppressWarnings("unchecked")
	public Class<R> getJdbcJavaType() {
		return (Class<R>) getJdbcClassmateType().getErasedType();
	}

	@Override
	public String toString() {
		return String.format(
				"%s[converterClass=%s, domainJavaType=%s, jdbcJavaType=%s]",
				this.getClass().getName(),
				getAttributeConverter().getClass().getName(),
				getDomainJavaType().getName(),
				getJdbcJavaType().getName()
		);
	}

	/**
	 * @deprecated Use {@link #getDomainJavaType()} instead
	 */
	@Deprecated
	public Class getEntityAttributeType() {
		return getDomainJavaType();
	}

	/**
	 * @deprecated Use {@link #getJdbcJavaType()} instead
	 */
	@Deprecated
	public Class getDatabaseColumnType() {
		return getJdbcJavaType();
	}
}