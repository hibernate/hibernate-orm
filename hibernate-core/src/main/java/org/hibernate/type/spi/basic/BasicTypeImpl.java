/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi.basic;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;
import javax.persistence.AttributeConverter;
import javax.persistence.metamodel.Type;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.ColumnMapping;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.spi.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.descriptor.java.MutabilityPlan;
import org.hibernate.type.spi.descriptor.sql.SqlTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class BasicTypeImpl<T,D> implements BasicType<T> {
	private final TypeConfiguration typeConfiguration;

	private final ColumnMapping columnMapping;

	private final JavaTypeDescriptor<T> domainJavaType;

	private final MutabilityPlan<T> mutabilityPlan;
	private final Comparator<T> comparator;

	private final AttributeConverter<T,D> converter;
	private final JavaTypeDescriptor intermediateJavaType;

	/**
	 * Constructor form for building a basic type without an AttributeConverter
	 *
	 * @param typeConfiguration The TypeFactory that this Type is scoped to
	 * @param domainJavaType The descriptor for the domain model Java type.
	 * @param sqlType The descriptor for the JDBC type.
	 * @param mutabilityPlan The Type-specific MutabilityPlan.  May be {@code null} indicating to
	 * use the MutabilityPlan as defined by {@link JavaTypeDescriptor#getMutabilityPlan()}
	 * @param comparator The Type-specific Comparator.  May be {@code null} indicating to
	 * use the Comparator as defined by {@link JavaTypeDescriptor#getComparator()} ()}
	 */
	public BasicTypeImpl(
			TypeConfiguration typeConfiguration,
			JavaTypeDescriptor<T> domainJavaType,
			SqlTypeDescriptor sqlType,
			MutabilityPlan<T> mutabilityPlan,
			Comparator<T> comparator) {
		this( typeConfiguration, domainJavaType, sqlType, mutabilityPlan, comparator, null, null );
	}

	/**
	 * Constructor form for building a basic type with an AttributeConverter.
	 * <p/>
	 * Notice that 2 different JavaTypeDescriptor instances are passed in here.  {@code domainJavaType} represents
	 * the Java type in the user's domain model.  {@code intermediateJavaType} represents the Java type expressed
	 * by the AttributeConverter as the "database type".  We will read the database value initially using the
	 * {@code sqlType} + {@code intermediateJavaType}.  We then pass that value along to the AttributeConverter
	 * to convert to the domain Java type.
	 *
	 * @param typeConfiguration The TypeFactory that this Type is scoped to
	 * @param domainJavaType The descriptor for the domain model Java type.
	 * @param sqlType The descriptor for the JDBC type.
	 * @param mutabilityPlan The Type-specific MutabilityPlan.  May be {@code null} indicating to
	 * use the MutabilityPlan as defined by {@link JavaTypeDescriptor#getMutabilityPlan()}
	 * @param comparator The Type-specific Comparator.  May be {@code null} indicating to
	 * use the Comparator as defined by {@link JavaTypeDescriptor#getComparator()} ()}
	 * @param attributeConverter The AttributeConverter to apply
	 * @param intermediateJavaType The Java type we use to talk to JDBC.
	 */
	public BasicTypeImpl(
			TypeConfiguration typeConfiguration,
			JavaTypeDescriptor<T> domainJavaType,
			SqlTypeDescriptor sqlType,
			MutabilityPlan<T> mutabilityPlan,
			Comparator<T> comparator,
			AttributeConverter<T,D> attributeConverter,
			JavaTypeDescriptor intermediateJavaType) {
		this.typeConfiguration = typeConfiguration;

		this.domainJavaType = domainJavaType;

		this.columnMapping = new ColumnMapping( sqlType );

		this.mutabilityPlan = mutabilityPlan == null ? domainJavaType.getMutabilityPlan() : mutabilityPlan;
		this.comparator = comparator == null ? domainJavaType.getComparator() : comparator;

		this.converter = attributeConverter;
		this.intermediateJavaType = intermediateJavaType;
	}

	@Override
	public String getTypeName() {
		// todo : improve this to account for converters, etc
		return getJavaTypeDescriptor().getJavaTypeClass().getName();
	}

	@Override
	public JavaTypeDescriptor<T> getJavaTypeDescriptor() {
		return domainJavaType;
	}

	@Override
	public ColumnMapping getColumnMapping() {
		return columnMapping;
	}

	public AttributeConverter<T, D> getAttributeConverter() {
		return converter;
	}

	@Override
	public MutabilityPlan<T> getMutabilityPlan() {
		return mutabilityPlan;
	}

	@Override
	public Comparator<T> getComparator() {
		return comparator;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object hydrate(
			ResultSet rs,
			String[] names,
			SessionImplementor session,
			Object owner) throws HibernateException, SQLException {
		if ( converter == null ) {
			return getColumnMapping().getSqlTypeDescriptor().getExtractor( domainJavaType ).extract(
					rs,
					names[0],
					session
			);
		}
		else {
			final D databaseValue = (D) getColumnMapping().getSqlTypeDescriptor().getExtractor( intermediateJavaType ).extract(
					rs,
					names[0],
					session
			);

			return converter.convertToEntityAttribute( databaseValue );
		}
	}
}
