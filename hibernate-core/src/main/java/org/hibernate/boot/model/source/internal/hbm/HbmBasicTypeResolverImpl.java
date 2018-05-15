/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import javax.persistence.EnumType;
import javax.persistence.TemporalType;

import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.domain.ResolutionContext;
import org.hibernate.boot.model.source.spi.HibernateTypeSource;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.BasicTypeResolverSupport;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.BasicType;
import org.hibernate.type.spi.BasicTypeParameters;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Chris Cranford
 */
public class HbmBasicTypeResolverImpl extends BasicTypeResolverSupport
		implements BasicTypeParameters, JdbcRecommendedSqlTypeMappingContext {
	private final BasicJavaDescriptor javaTypeDescriptor;

	public HbmBasicTypeResolverImpl(MetadataBuildingContext mappingDocument, HibernateTypeSource typeSource) {
		super( mappingDocument );
		this.javaTypeDescriptor = (BasicJavaDescriptor) mappingDocument.getBootstrapContext()
				.getTypeConfiguration()
				.getJavaTypeDescriptorRegistry()
				.getDescriptor( typeSource.getName() );
	}

	@Override
	public BasicType resolveBasicType(ResolutionContext context) {
		return getTypeConfiguration().getBasicTypeRegistry().resolveBasicType( this, this );
	}

	@Override
	protected Class getReflectedValueJavaType() {
		return javaTypeDescriptor.getJavaType();
	}

	@Override
	public BasicJavaDescriptor getJavaTypeDescriptor() {
		return javaTypeDescriptor;
	}

	@Override
	public SqlTypeDescriptor getSqlTypeDescriptor() {
		return null;
	}

	@Override
	public ConverterDescriptor getAttributeConverterDescriptor() {
		// not supported
		return null;
	}

	@Override
	public TemporalType getTemporalPrecision() {
		// not supported
		return null;
	}

	@Override
	public EnumType getEnumeratedType() {
		// not supported?
		return null;
	}

	@Override
	public boolean isNationalized() {
		return getBuildingContext().getBuildingOptions().useNationalizedCharacterData();
	}

	@Override
	public boolean isLob() {
		return false;
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return getBuildingContext().getBootstrapContext().getTypeConfiguration();
	}
}
