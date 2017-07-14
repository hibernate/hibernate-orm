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
import org.hibernate.boot.model.relational.MappedTable;
import org.hibernate.boot.model.type.spi.BasicTypeResolver;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.spi.AttributeConverterDescriptor;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.type.converter.spi.AttributeConverterDefinition;
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

	public BasicValue(MetadataBuildingContext buildingContext, MappedTable table) {
		super( buildingContext, table );
	}

	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return resolveType().getJavaTypeDescriptor();
	}

	public SqlTypeDescriptor getColumnsSqlTypeDescriptors() {
		return resolveType().getColumnDescriptor().getSqlTypeDescriptor();
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
			throw new MappingException( "Attempt to add additional MappedColumn to BasicValueMapping" );
		}
		super.addColumn( column );
	}

	@Override
	protected void setSqlTypeDescriptorResolver(Column column) {
		column.setSqlTypeDescriptorResolver( new BasicValueSqlTypeDescriptorResolver( ) );
	}

	@Override
	public Object accept(ValueVisitor visitor) {
		return visitor.accept(this);
	}

	public class BasicValueSqlTypeDescriptorResolver implements SqlTypeDescriptorResolver {
		@Override
		public SqlTypeDescriptor resolveSqlTypeDescriptor() {
			return resolveType().getColumnDescriptor().getSqlTypeDescriptor();
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
		return basicTypeResolver.resolveBasicType();
	}

	@Override
	public AttributeConverterDefinition getAttributeConverterDefinition() {
		return attributeConverterDescriptor;
	}
}
