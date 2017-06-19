/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.source.internal.hbm;

import java.util.Comparator;
import javax.persistence.EnumType;
import javax.persistence.TemporalType;

import org.hibernate.boot.model.source.spi.HibernateTypeSource;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.BasicTypeResolverSupport;
import org.hibernate.type.converter.spi.AttributeConverterDefinition;
import org.hibernate.type.descriptor.java.MutabilityPlan;
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
	public BasicType resolveBasicType() {
		return getTypeConfiguration().getBasicTypeRegistry().resolveBasicType( this, this );
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
	public AttributeConverterDefinition getAttributeConverterDefinition() {
		// not supported
		return null;
	}

	@Override
	public Comparator getComparator() {
		// not supported
		return null;
	}

	@Override
	public TemporalType getTemporalPrecision() {
		// not supported
		return null;
	}

	@Override
	public MutabilityPlan getMutabilityPlan() {
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