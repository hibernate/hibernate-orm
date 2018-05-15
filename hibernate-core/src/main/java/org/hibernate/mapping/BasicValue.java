/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.mapping;

import java.util.Map;
import javax.persistence.AttributeConverter;
import javax.persistence.EnumType;
import javax.persistence.TemporalType;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.boot.model.TypeDefinition;
import org.hibernate.boot.model.convert.internal.ClassBasedConverterDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.domain.BasicValueMapping;
import org.hibernate.boot.model.domain.JavaTypeMapping;
import org.hibernate.boot.model.domain.NotYetResolvedException;
import org.hibernate.boot.model.domain.ResolutionContext;
import org.hibernate.boot.model.relational.MappedColumn;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.metamodel.model.convert.internal.NamedEnumValueConverter;
import org.hibernate.metamodel.model.convert.internal.OrdinalEnumValueConverter;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.type.descriptor.java.internal.EnumJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.BasicTypeParameters;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
@SuppressWarnings("unused")
public class BasicValue
		extends SimpleValue
		implements BasicValueMapping, JdbcRecommendedSqlTypeMappingContext {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( BasicValue.class );

	private final TypeConfiguration typeConfiguration;
	private final int preferredJdbcTypeCodeForBoolean;

	private boolean isNationalized;
	private boolean isLob;
	private EnumType enumType;
	private TemporalType temporalPrecision;

	private BasicType basicType;
	private BasicJavaDescriptor javaTypeDescriptor;
	private JavaTypeMapping javaTypeMapping;
	private SqlTypeDescriptor sqlTypeDescriptor;

	private ConverterDescriptor attributeConverterDescriptor;

	private Class resolvedJavaClass;
	private String ownerName;
	private String propertyName;
	private Map explicitLocalTypeParams;

	public BasicValue(MetadataBuildingContext buildingContext, MappedTable table) {
		super( buildingContext, table );

		this.typeConfiguration = buildingContext.getBootstrapContext().getTypeConfiguration();
		this.preferredJdbcTypeCodeForBoolean = buildingContext.getPreferredSqlTypeCodeForBoolean();

		this.javaTypeMapping = new BasicJavaTypeMapping( this );
		buildingContext.getMetadataCollector().registerValueMappingResolver(
				resolutionContext -> resolve( resolutionContext )
		);
	}

	@Override
	public JavaTypeMapping getJavaTypeMapping() {
		return javaTypeMapping;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Boolean resolve(ResolutionContext context) {
		final String name = getTypeName();

		if ( basicType == null && name != null ) {
			// Name could refer to:
			//		1) a registered TypeDef

			//		2) basic type "resolution key"
			//
			final TypeDefinition typeDefinition = getMetadataBuildingContext().resolveTypeDefinition( name );
			if ( typeDefinition != null ) {
				basicType = typeDefinition.resolveTypeResolver( explicitLocalTypeParams ).resolveBasicType( context );
			}
			else {
				basicType = getMetadataBuildingContext()
						.getBootstrapContext()
						.getTypeConfiguration()
						.getBasicTypeRegistry()
						.getBasicType( name );
			}

			this.javaTypeDescriptor = basicType.getJavaTypeDescriptor();
			this.sqlTypeDescriptor = basicType.getSqlTypeDescriptor();
		}
		else {
			if ( javaTypeDescriptor == null && resolvedJavaClass != null ) {
				javaTypeDescriptor = (BasicJavaDescriptor) context.getBootstrapContext()
						.getTypeConfiguration()
						.getJavaTypeDescriptorRegistry()
						.getOrMakeJavaDescriptor( resolvedJavaClass );
			}

			if ( javaTypeDescriptor == null && ownerName != null && propertyName != null ) {
				resolvedJavaClass = ReflectHelper.reflectedPropertyClass(
						ownerName,
						propertyName,
						context.getBootstrapContext().getServiceRegistry().getService( ClassLoaderService.class )
				);
				javaTypeDescriptor = (BasicJavaDescriptor) context.getBootstrapContext()
						.getTypeConfiguration()
						.getJavaTypeDescriptorRegistry()
						.getOrMakeJavaDescriptor( resolvedJavaClass );
			}

			if ( basicType == null ) {
				basicType = context.getBootstrapContext().getTypeConfiguration().getBasicTypeRegistry().resolveBasicType(
						new BasicTypeParameters() {
							@Override
							public BasicJavaDescriptor getJavaTypeDescriptor() {
								return javaTypeDescriptor;
							}

							@Override
							public SqlTypeDescriptor getSqlTypeDescriptor() {
								return sqlTypeDescriptor;
							}

							@Override
							public ConverterDescriptor getAttributeConverterDescriptor() {
								return attributeConverterDescriptor;
							}

							@Override
							public TemporalType getTemporalPrecision() {
								return temporalPrecision;
							}
						},
						new JdbcRecommendedSqlTypeMappingContext() {
							final int preferredSqlTypeCodeForBoolean = context.getBootstrapContext()
									.getTypeConfiguration()
									.getBasicTypeRegistry()
									.getBaseJdbcRecommendedSqlTypeMappingContext()
									.getPreferredSqlTypeCodeForBoolean();

							@Override
							public EnumType getEnumeratedType() {
								return enumType == null
										? context.getBootstrapContext().getMetadataBuildingOptions().getImplicitEnumType()
										: enumType;
							}

							@Override
							public SqlTypeDescriptor getExplicitSqlTypeDescriptor() {
								return sqlTypeDescriptor;
							}

							@Override
							public int getPreferredSqlTypeCodeForBoolean() {
								return preferredSqlTypeCodeForBoolean;
							}

							@Override
							public boolean isNationalized() {
								return isNationalized;
							}

							@Override
							public boolean isLob() {
								return isLob;
							}

							@Override
							public TemporalType getTemporalType() {
								return temporalPrecision;
							}

							@Override
							public TypeConfiguration getTypeConfiguration() {
								return typeConfiguration;
							}
						}
				);
			}
		}

		final MappedColumn column = getMappedColumn();

		final JavaTypeMapping columnJavaTypeMapping;

		if ( basicType.getJavaTypeDescriptor() instanceof EnumJavaDescriptor ) {
			final EnumType enumType = this.enumType == null
					? context.getBootstrapContext().getMetadataBuildingOptions().getImplicitEnumType()
					: this.enumType;

			if ( enumType == EnumType.STRING ) {
				column.setJavaTypeMapping(
						new JavaTypeMapping() {
							@Override
							public String getTypeName() {
								return null;
							}

							@Override
							public JavaTypeDescriptor getJavaTypeDescriptor() throws NotYetResolvedException {
								return context.getBootstrapContext().getTypeConfiguration()
										.getJavaTypeDescriptorRegistry()
										.getDescriptor( String.class );
							}
						}
				);
			}
			else {
				column.setJavaTypeMapping(
						new JavaTypeMapping() {
							@Override
							public String getTypeName() {
								return null;
							}

							@Override
							public JavaTypeDescriptor getJavaTypeDescriptor() throws NotYetResolvedException {
								return context.getBootstrapContext().getTypeConfiguration()
										.getJavaTypeDescriptorRegistry()
										.getDescriptor( Integer.class );
							}
						}
				);
			}
		}
		else if ( attributeConverterDescriptor != null ) {
			column.setJavaTypeMapping(
					new JavaTypeMapping() {
						@Override
						public String getTypeName() {
							return null;
						}

						@Override
						public JavaTypeDescriptor getJavaTypeDescriptor() throws NotYetResolvedException {
							final Class<?> convertedRelationalJavaType = attributeConverterDescriptor.getRelationalValueResolvedType()
									.getErasedType();
							return context.getBootstrapContext().getTypeConfiguration()
									.getJavaTypeDescriptorRegistry()
									.getDescriptor( convertedRelationalJavaType );
						}
					}
			);
		}
		else {
			column.setJavaTypeMapping( javaTypeMapping );
		}

		column.setSqlTypeDescriptorAccess( () -> basicType.getSqlTypeDescriptor() );

		return true;
	}

	@Override
	public ConverterDescriptor getAttributeConverterDescriptor() {
		return attributeConverterDescriptor;
	}

	public boolean isNationalized() {
		return isNationalized;
	}

	public boolean isLob() {
		return isLob;
	}

	public EnumType getEnumType() {
		return enumType;
	}

	public TemporalType getTemporalPrecision() {
		return temporalPrecision;
	}

	public SqlTypeDescriptor getExplicitSqlType() {
		return sqlTypeDescriptor;
	}

	public void setJpaAttributeConverterDescriptor(ConverterDescriptor attributeConverterDescriptor) {
		this.attributeConverterDescriptor = attributeConverterDescriptor;
	}

	public void makeNationalized() {
		this.isNationalized = true;
	}

	public void makeLob() {
		this.isLob = true;
	}

	public void setEnumType(EnumType enumType) {
		this.enumType = enumType;
	}

	public void setTemporalPrecision(TemporalType temporalPrecision) {
		this.temporalPrecision = temporalPrecision;
	}

	public void setSqlType(SqlTypeDescriptor sqlTypeDescriptor) {
		this.sqlTypeDescriptor = sqlTypeDescriptor;
	}

	@Override
	public void addColumn(Column column) {
		if ( getMappedColumns().size() > 0 ) {
			throw new MappingException( "Attempt to add additional MappedColumn to BasicValueMapping " + column.getName() );
		}
		super.addColumn( column );
	}

	@Override
	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}

	public void setJavaClass(Class resolvedJavaClass) {
		this.resolvedJavaClass = resolvedJavaClass;
	}

	@Override
	public void addFormula(Formula formula) {
		if ( getMappedColumns().size() > 0 ) {
			throw new MappingException( "Attempt to add additional MappedColumn to BasicValueMapping" );
		}
		super.addFormula( formula );
	}

	public void setExplicitTypeName(String typeName) {
		if ( typeName != null && typeName.startsWith( ConverterDescriptor.TYPE_NAME_PREFIX ) ) {
			final String converterClassName = typeName.substring( ConverterDescriptor.TYPE_NAME_PREFIX.length() );
			final ClassLoaderService cls = getMetadataBuildingContext()
					.getMetadataCollector()
					.getMetadataBuildingOptions()
					.getServiceRegistry()
					.getService( ClassLoaderService.class );
			try {
				final Class<AttributeConverter> converterClass = cls.classForName( converterClassName );
				attributeConverterDescriptor = new ClassBasedConverterDescriptor(
						converterClass,
						false,
						getMetadataBuildingContext().getBootstrapContext().getClassmateContext()
				);
				return;
			}
			catch (Exception e) {
				log.logBadHbmAttributeConverterType( typeName, e.getMessage() );
			}
		}

		super.setExplicitTypeName( typeName );
	}

	@Override
	public BasicType resolveType() {
		if ( basicType == null ) {
			throw new NotYetResolvedException( "BasicValue has not been resolved yet" );
		}
		return basicType;
	}

	@Override
	public boolean isTypeSpecified() {
		// We mandate a BasicTypeResolver, so this is always true.
		return true;
	}

	@Override
	public void setTypeUsingReflection(String className, String propertyName) throws MappingException {
		this.ownerName = className;
		this.propertyName = propertyName;
	}

	public <T> void setJavaTypeDescriptor(BasicJavaDescriptor<T> javaDescriptor) {
		this.javaTypeDescriptor = javaDescriptor;
	}


	@Override
	@SuppressWarnings("unchecked")
	public BasicValueConverter resolveValueConverter(
			RuntimeModelCreationContext creationContext,
			BasicType basicType) {
		if ( getAttributeConverterDescriptor() != null ) {
			return getAttributeConverterDescriptor().createJpaAttributeConverter( creationContext );
		}

		final JavaTypeDescriptor jtd = basicType.getJavaTypeDescriptor();

		if ( jtd instanceof EnumJavaDescriptor ) {
			final EnumType enumType = this.enumType == null
					? creationContext.getBootstrapContext().getMetadataBuildingOptions().getImplicitEnumType()
					: this.enumType;
			switch ( enumType ) {
				case STRING: {
					return new NamedEnumValueConverter( (EnumJavaDescriptor) jtd, creationContext );
				}
				case ORDINAL: {
					return new OrdinalEnumValueConverter( (EnumJavaDescriptor) jtd, creationContext );
				}
				default: {
					throw new HibernateException( "Unknown EnumType : " + enumType );
				}
			}
		}

		// todo (6.0) : other conversions?
		// 		- how is temporalPrecision going to be handled?  during resolution of BasicType?

		return null;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JdbcRecommendedSqlTypeMappingContext

	@Override
	public EnumType getEnumeratedType() {
		return enumType;
	}

	@Override
	public TemporalType getTemporalType() {
		return temporalPrecision;
	}

	@Override
	public SqlTypeDescriptor getExplicitSqlTypeDescriptor() {
		return sqlTypeDescriptor;
	}

	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return preferredJdbcTypeCodeForBoolean;
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return typeConfiguration;
	}

	public void setExplicitTypeParams(Map explicitLocalTypeParams) {
		this.explicitLocalTypeParams = explicitLocalTypeParams;
	}


	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// JavaTypeMapping

	static class BasicJavaTypeMapping implements JavaTypeMapping {
		private final BasicValue basicValue;

		BasicJavaTypeMapping(BasicValue basicValue) {
			this.basicValue = basicValue;
		}

		public Class getJavaClass() {
			return getJavaTypeDescriptor().getClass();
		}

		@Override
		public String getTypeName() {
			String typeName = basicValue.getTypeName();
			if ( typeName != null ) {
				return typeName;
			}
			return getJavaTypeDescriptor().getTypeName();
		}

		@Override
		public JavaTypeDescriptor getJavaTypeDescriptor() {
			if ( basicValue.javaTypeDescriptor == null ) {
				throw new NotYetResolvedException( "JavaTypeDescriptor not yet resolved" );
			}
			return basicValue.javaTypeDescriptor;
		}
	}

}
