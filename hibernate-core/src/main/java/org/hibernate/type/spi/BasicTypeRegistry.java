/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.spi;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.spi.BasicValueMapper;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.internal.BasicTypeImpl;

import org.jboss.logging.Logger;

/**
 * Registry for BasicType instances.  Lookup is primarily done by Java type
 * (Class), but can be adjusted by JDBC type-code and/or MutabilityPlan.
 * <p/>
 * It is important to understand that all basic types have a Java type.  We
 * do not support alternate EntityModes for basic-types.
 * <p/>
 * The ability
 *
 * @author Steve Ebersole
 * @author Chris Cranford
 */
@Incubating
public class BasicTypeRegistry {
	private static final Logger log = Logger.getLogger( BasicTypeRegistry.class );

	private final TypeConfiguration typeConfiguration;
	private final Map<SqlTypeDescriptor,Map<BasicJavaDescriptor,BasicType>> registryValues = new ConcurrentHashMap<>();

	/**
	 * HQL: ... where cast(c.name as uuid) ...
	 *
	 * The target type (uuid, e.g.) is
	 */

	//
	private final Map<String,BasicType> typesByName = new ConcurrentHashMap<>();

	// todo (6.0) : ? Map<o.h.t.BasicType,o.h.t.spi.BasicType>
	//		similar in concept to what I was trying to achieve with:
	//
	//		class TypeConfiguration ... {
	//			...
	//			private final ConcurrentHashMap<StandardBasicType<?>,BasicValueMapper<?>> standardBasicTypeResolutionCache = new ConcurrentHashMap<>();
	//			...
	//		}
	//
	// ^^ essentially it is the scoped[1], and therefore "resolved"[2], form of one of the static `o.h.type.StandardBasicTypes` references.
	//		1) The scoping is relative to the TypeConfiguration ("for this TypeConfiguration, this is the SPI BasicType to use for a given API BasicType")
	//		2) the "resolution" is the scoped SPI BasicType form
	//
	//		The idea is to avoid having to link the 2 contracts via extension.  The gain is that it lets us
	// 		interpret the specific JavaTypeDescriptor and SqlTypeDescriptor to use via the registries as opposed
	// 		to having to use the ones statically defined on the BasicTypeImpl.  E.g.,
	// 		`StandardBasicTypes#STRING` statically says to use `StringJavaDescriptor#INSTANCE` and
	//		`VarcharSqlDescriptor#INSTANCE` - so it will use those regardless whether:
	//				1) Something (user, Dialect, integration, ..) registered an override for either the JTD or STD
	//				2) nationalization was globally requested (NVARCHAR versus VARCHAR)
	//				3) LOBs were globally requested (aka "materialized" LOBs)
	//				...



	public BasicTypeRegistry(TypeConfiguration typeConfiguration) {
		this.typeConfiguration = typeConfiguration;
	}

	public TypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
	}


	/**
	 * Find an existing BasicType registration for the given JavaTypeDescriptor and
	 * SqlTypeDescriptor combo or create (and register) one.
	 */
	public BasicType<?> resolve(BasicJavaDescriptor<?> jtdToUse, SqlTypeDescriptor stdToUse) {
		final Map<BasicJavaDescriptor, BasicType> mappingsForStdToUse = registryValues.computeIfAbsent(
				stdToUse,
				sqlTypeDescriptor -> new ConcurrentHashMap<>()
		);

		return mappingsForStdToUse.computeIfAbsent(
				jtdToUse,
				javaDescriptor -> new BasicTypeImpl(
						javaDescriptor,
						stdToUse,
						stdToUse.getSqlExpressableType( jtdToUse, typeConfiguration )
				)
		);
	}

	// todo (6.0) : we want to consolidate many of these methods below based on typesByName vs. registryValues lookups


	public BasicType getBasicTypeByName(String key) {
		return typesByName.get( key );
	}

	/**
	 * Returns the default BasicType for the given Java type
	 *
	 * @param javaType The Java type (Class) for which we want the BasicType.
	 *
	 * @return The linked BasicType.  May return {@code null}
	 */
	@SuppressWarnings({"unchecked", "RedundantClassCall"})
	public <T> BasicType<T> getBasicType(Class<T> javaType) {
		final JavaTypeDescriptor<T> jtd = typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( javaType );

		if ( jtd == null ) {
			return null;
		}
		else if ( !(jtd instanceof BasicJavaDescriptor) ) {
			throw new HibernateException(
					String.format(
							Locale.ROOT,
							"Previously registered non-basic JavaTypeDescriptor [%s] found for Class [%s]; cannot create BasicType",
							jtd,
							javaType.getName()
					)
			);
		}

		final SqlTypeDescriptor recommendedStd = jtd.getJdbcRecommendedSqlType( typeConfiguration.getCurrentBaseSqlTypeIndicators() );

		return (BasicType) resolve( (BasicJavaDescriptor) jtd, recommendedStd );
	}



	// BasicValueBinder really ought to just track the explicit selections
//
//	@SuppressWarnings("unchecked")
//	public <T> BasicType<T> resolveBasicType(
//			BasicTypeParameters<T> parameters,
//			SqlTypeDescriptorIndicators jdbcTypeResolutionContext) {
//		if ( parameters == null ) {
//			throw new IllegalArgumentException( "BasicTypeParameters must not be null" );
//		}
//
//		// IMPL NOTE : resolving a BasicType follows very different algorithms based on what
//		// specific information is available (non-null) from the BasicTypeParameters.  To help
//		// facilitate that, we try to break this down into a number of sub-methods for some
//		// high-level differences
//
//		// todo implement this.  But the intention has changed.  Here we simply have another potential
//		//		hint as to the SqlTypeDescriptor to use.
//
////		if ( parameters.getAttributeConverterDefinition() != null ) {
////			return resolveConvertedBasicType( parameters, jdbcTypeResolutionContext );
////		}
//
//
//		BasicJavaDescriptor<T> javaTypeDescriptor = parameters.getJavaTypeDescriptor();
//		SqlTypeDescriptor sqlTypeDescriptor = parameters.getSqlTypeDescriptor();
//
//		// todo (6.0) - should no longer handle "attribute converter here.
//		// 		see note on `BasicTypeParameters#getAttributeConverterDescriptor`
//		if ( parameters.getAttributeConverterDescriptor() != null ) {
//			// we have an attribute converter, use that to either:
//			//		1) validate the BasicJavaDescriptor/SqlTypeDescriptor defined on parameters
//			//		2) use the converter param types as hints to the missing BasicJavaDescriptor/SqlTypeDescriptor
//
//			final Class<?> converterDomainJavaType = parameters.getAttributeConverterDescriptor()
//					.getDomainValueResolvedType()
//					.getErasedType();
//
//			final JavaTypeDescriptor<?> converterDomainJavaDescriptor = typeConfiguration.getJavaTypeDescriptorRegistry()
//					.getDescriptor( converterDomainJavaType );
//
//			if ( javaTypeDescriptor == null ) {
//				javaTypeDescriptor = (BasicJavaDescriptor<T> ) converterDomainJavaDescriptor;
//			}
//			else {
//				if ( !javaTypeDescriptor.equals( converterDomainJavaDescriptor ) ) {
//					throw new HibernateException(
//							"JavaTypeDescriptors did not match between BasicTypeParameters#getJavaTypeDescriptor and " +
//									"BasicTypeParameters#getAttributeConverterDefinition#getDomainType"
//					);
//				}
//			}
//
//			final Class<?> converterRelationalJavaType = parameters.getAttributeConverterDescriptor()
//					.getRelationalValueResolvedType()
//					.getErasedType();
//			final JavaTypeDescriptor<?> converterRelationalJavaDescriptor = typeConfiguration.getJavaTypeDescriptorRegistry()
//					.getDescriptor( converterRelationalJavaType );
//
//			final SqlTypeDescriptor resolvedConverterHintedSqlTypeDescriptor = converterRelationalJavaDescriptor
//					.getJdbcRecommendedSqlType( jdbcTypeResolutionContext );
//
//			if ( sqlTypeDescriptor == null ) {
//				sqlTypeDescriptor = resolvedConverterHintedSqlTypeDescriptor;
//			}
//			else {
//				if ( !sqlTypeDescriptor.equals( resolvedConverterHintedSqlTypeDescriptor ) ) {
//					throw new HibernateException(
//							"SqlTypeDescriptors did not match between BasicTypeParameters#getSqlTypeDescriptor and " +
//									"BasicTypeParameters#getAttributeConverterDefinition#getJdbcType"
//					);
//				}
//			}
//		}
//		else if ( parameters.getTemporalPrecision() != null ) {
//			// we have a specified temporal precision, which is another hint as to types...
//			if ( javaTypeDescriptor == null ) {
//				javaTypeDescriptor = determineJavaDescriptorForTemporalPrecision( parameters.getTemporalPrecision() );
//			}
//			// else verify that javaTypeDescriptor is "compatible" with parameters.getTemporalPrecision() ?
//
//			if ( sqlTypeDescriptor == null ) {
//				sqlTypeDescriptor = javaTypeDescriptor.getJdbcRecommendedSqlType( jdbcTypeResolutionContext );
//			}
//		}
//
//		if ( javaTypeDescriptor == null ) {
//			if ( sqlTypeDescriptor == null ) {
//				throw new IllegalArgumentException(
//						"BasicTypeParameters must define either a JavaTypeDescriptor or a SqlTypeDescriptor, " +
//								"or provide AttributeConverter or JPA temporal precision (javax.persistence.TemporalType)"
//				);
//			}
//			javaTypeDescriptor = sqlTypeDescriptor.getJdbcRecommendedJavaTypeMapping( jdbcTypeResolutionContext.getTypeConfiguration() );
//		}
//
//		if ( sqlTypeDescriptor == null ) {
//			sqlTypeDescriptor = javaTypeDescriptor.getJdbcRecommendedSqlType( jdbcTypeResolutionContext );
//		}
//
//		return createBasicType( javaTypeDescriptor, sqlTypeDescriptor );
//	}

//	private <T> BasicType<T> createBasicType(
//			BasicJavaDescriptor<T> javaTypeDescriptor,
//			SqlTypeDescriptor sqlTypeDescriptor) {
//		final BasicTypeImpl<T> basicType = new BasicTypeImpl<>( javaTypeDescriptor, sqlTypeDescriptor );
//		registry.put( javaTypeDescriptor.getJavaType().getName(), basicType );
//		return basicType;
//	}
//
//	private <T> TemporalJavaDescriptor<T> determineJavaDescriptorForTemporalPrecision(javax.persistence.TemporalType temporalPrecision) {
//		switch ( temporalPrecision ) {
//			case TIMESTAMP: {
//				return (TemporalJavaDescriptor<T>) typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( java.sql.Timestamp.class );
//			}
//			case DATE: {
//				return (TemporalJavaDescriptor<T>) typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( java.sql.Date.class );
//			}
//			case TIME: {
//				return (TemporalJavaDescriptor<T>) typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( java.sql.Time.class );
//			}
//			default: {
//				throw new HibernateException( "Unrecognized JPA temporal precision : " + temporalPrecision );
//			}
//		}
//	}

	public void register(BasicType type) {
		typesByName.put( type.getJavaTypeDescriptor().getJavaType().getName(), type );
	}

	public void register(BasicType type, String key) {
		typesByName.put( key, type );
	}

	public void register(BasicType type, String... keys) {
		for ( String key : keys ) {
			typesByName.put( key, type );
		}
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// BasicTypeRegistry as a `ValueMapperRegistry`

	private final Map<String,BasicValueMapper> mapperMap = new ConcurrentHashMap<>();

	public void register(BasicValueMapper valueMapper, String... keys) {
		for ( String key : keys ) {
			final BasicValueMapper old = mapperMap.put( key, valueMapper );
			if ( old != null && old != valueMapper ) {
				log.debugf(
						"BasicValueMapper registration [%s] overrode previous : %s -> %s",
						key,
						old,
						valueMapper
				);
			}
		}
	}
}
