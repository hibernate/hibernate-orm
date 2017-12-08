/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.mapping;

import javax.persistence.AttributeConverter;

import org.hibernate.MappingException;
import org.hibernate.boot.internal.AttributeConverterDescriptorNonAutoApplicableImpl;
import org.hibernate.boot.model.domain.BasicValueMapping;
import org.hibernate.boot.model.domain.JavaTypeMapping;
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.boot.model.type.spi.BasicTypeResolver;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.AttributeConverterDescriptor;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.BasicTypeResolverConvertibleSupport;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.type.converter.spi.AttributeConverterDefinition;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.BasicType;

/**
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
public class BasicValue extends SimpleValue implements BasicValueMapping {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( BasicValue.class );

	private boolean isNationalized;
	private boolean isLob;
	private AttributeConverterDescriptor attributeConverterDescriptor;
	private BasicTypeResolver basicTypeResolver;
	private JavaTypeMapping javaTypeMapping;
	private BasicType basicType;

	public BasicValue(MetadataBuildingContext buildingContext, MappedTable table) {
		super( buildingContext, table );
	}

	@Override
	public JavaTypeMapping getJavaTypeMapping() {
		// todo (6.0) - this seems hackish as a replacement for {@link #getJavaTypeDescriptor()}.
		if ( javaTypeMapping == null ) {
			final BasicType basicType = resolveType();
			javaTypeMapping = new JavaTypeMapping() {
				@Override
				public String getTypeName() {
					return basicType.getJavaType().getTypeName();
				}

				@Override
				public JavaTypeDescriptor resolveJavaTypeDescriptor() {
					return basicType.getJavaTypeDescriptor();
				}
			};
		}

		return javaTypeMapping;
	}

	public AttributeConverterDescriptor getAttributeConverterDescriptor() {
		return attributeConverterDescriptor;
	}

	public boolean isNationalized() {
		return isNationalized;
	}

	public boolean isLob() {
		return isLob;
	}

	public void setJpaAttributeConverterDescriptor(AttributeConverterDescriptor attributeConverterDescriptor) {
		this.attributeConverterDescriptor = attributeConverterDescriptor;
	}

	public void setBasicTypeResolver(BasicTypeResolver basicTypeResolver) {
		this.basicTypeResolver = basicTypeResolver;
	}

	public void makeNationalized() {
		this.isNationalized = true;
	}

	public void makeLob() {
		this.isLob = true;
	}

	@Override
	public void addColumn(Column column) {
		if ( getMappedColumns().size() > 0 ) {
			throw new MappingException( "Attempt to add additional MappedColumn to BasicValueMapping " + column.getName() );
		}
		super.addColumn( column );
	}

	@Override
	protected void setTypeDescriptorResolver(Column column) {
		column.setTypeDescriptorResolver( new BasicValueTypeDescriptorResolver( ) );
	}

	@Override
	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}

	public class BasicValueTypeDescriptorResolver implements TypeDescriptorResolver {
		@Override
		public SqlTypeDescriptor resolveSqlTypeDescriptor() {
			return resolveType().getSqlTypeDescriptor();
		}

		@Override
		public JavaTypeDescriptor resolveJavaTypeDescriptor() {
			return resolveType().getJavaTypeDescriptor();
		}
	}

	@Override
	public void addFormula(Formula formula) {
		if ( getMappedColumns().size() > 0 ) {
			throw new MappingException( "Attempt to add additional MappedColumn to BasicValueMapping" );
		}
		super.addFormula( formula );
	}

	public void setTypeName(String typeName) {
		if ( typeName != null && typeName.startsWith( AttributeConverterDescriptor.EXPLICIT_TYPE_NAME_PREFIX ) ) {
			final String converterClassName = typeName.substring( AttributeConverterDescriptor.EXPLICIT_TYPE_NAME_PREFIX.length() );
			final ClassLoaderService cls = getMetadataBuildingContext()
					.getMetadataCollector()
					.getMetadataBuildingOptions()
					.getServiceRegistry()
					.getService( ClassLoaderService.class );
			try {
				final Class<AttributeConverter> converterClass = cls.classForName( converterClassName );
				attributeConverterDescriptor = new AttributeConverterDescriptorNonAutoApplicableImpl(
						converterClass.newInstance(),
						getMetadataBuildingContext().getBootstrapContext().getTypeConfiguration().getJavaTypeDescriptorRegistry()
				);
				return;
			}
			catch (Exception e) {
				log.logBadHbmAttributeConverterType( typeName, e.getMessage() );
			}
		}

		super.setTypeName( typeName );
	}

	@Override
	public BasicType resolveType() {
		if ( basicType == null ) {
			basicType = basicTypeResolver.resolveBasicType();
		}
		return basicType;
	}

	@Override
	public AttributeConverterDefinition getAttributeConverterDefinition() {
		return attributeConverterDescriptor;
	}

	@Override
	public boolean isTypeSpecified() {
		// We mandate a BasicTypeResolver, so this is always true.
		return true;
	}

	@Override
	public void setTypeUsingReflection(String className, String propertyName) throws MappingException {
		// todo (6.0) - this check seems silly
		//		Several places call this method and its possible there are situations where the className
		//		is null because we're dealing with non-pojo cases.  In those cases, we cannot use reflection
		//		to determine type, so we don't overwrite the BasicTypeResolver that is already set.
		//
		//		Ideally can we remove this method call and somehow bake this into `#setType` ?
		if ( className != null ) {
			this.basicTypeResolver = new BasicTypeResolverUsingReflection(
					getMetadataBuildingContext(),
					getAttributeConverterDescriptor(),
					className,
					propertyName,
					isLob(),
					isNationalized()
			);
		}
	}

	public static class BasicTypeResolverUsingReflection extends BasicTypeResolverConvertibleSupport {
		private final JavaTypeDescriptor javaTypeDescriptor;
		private final SqlTypeDescriptor sqlTypeDescriptor;
		private final boolean isLob;
		private final boolean isNationalized;

		public BasicTypeResolverUsingReflection(
				MetadataBuildingContext buildingContext,
				AttributeConverterDescriptor converterDefinition,
				String className,
				String propertyName,
				boolean isLob,
				boolean isNationalized) {
			super( buildingContext, converterDefinition );
			this.isLob = isLob;
			this.isNationalized = isNationalized;

			if ( converterDefinition == null ) {
				final Class attributeType = ReflectHelper.reflectedPropertyClass(
						className,
						propertyName,
						buildingContext.getBootstrapContext().getServiceRegistry().getService( ClassLoaderService.class )
				);
				javaTypeDescriptor = buildingContext.getBootstrapContext()
						.getTypeConfiguration()
						.getJavaTypeDescriptorRegistry()
						.getDescriptor( attributeType );
				sqlTypeDescriptor = javaTypeDescriptor
						.getJdbcRecommendedSqlType(
								buildingContext.getBootstrapContext()
										.getTypeConfiguration()
										.getBasicTypeRegistry()
										.getBaseJdbcRecommendedSqlTypeMappingContext()
						);
			}
			else {
				javaTypeDescriptor = converterDefinition.getDomainType();
				sqlTypeDescriptor = converterDefinition
						.getJdbcType()
						.getJdbcRecommendedSqlType(
								buildingContext.getBootstrapContext()
										.getTypeConfiguration()
										.getBasicTypeRegistry()
										.getBaseJdbcRecommendedSqlTypeMappingContext()
						);
			}
		}

		@Override
		public BasicJavaDescriptor getJavaTypeDescriptor() {
			return (BasicJavaDescriptor) javaTypeDescriptor;
		}

		@Override
		public SqlTypeDescriptor getSqlTypeDescriptor() {
			return sqlTypeDescriptor;
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
		public int getPreferredSqlTypeCodeForBoolean() {
			return ConfigurationHelper
					.getPreferredSqlTypeCodeForBoolean(
							getBuildingContext().getBootstrapContext().getServiceRegistry()
					);
		}
	}

}
