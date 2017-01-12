/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.type.spi;

import java.util.Comparator;
import java.util.Map;
import javax.persistence.EnumType;
import javax.persistence.TemporalType;

import org.hibernate.type.converter.spi.AttributeConverterDefinition;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.spi.MutabilityPlan;
import org.hibernate.type.spi.descriptor.sql.SqlTypeDescriptor;

/**
 * Base class for BasicTypeSiteContext implementations.  Implements all
 * methods as default returns ({@code null}, false).
 *
 * @author Steve Ebersole
 */
public abstract class BasicTypeSiteContextSupport implements BasicTypeSiteContext {
	@Override
	public JavaTypeDescriptor getJavaTypeDescriptor() {
		return null;
	}

	@Override
	public SqlTypeDescriptor getSqlTypeDescriptor() {
		return null;
	}

	@Override
	public AttributeConverterDefinition getAttributeConverterDefinition() {
		return null;
	}

	@Override
	public MutabilityPlan getMutabilityPlan() {
		return null;
	}

	@Override
	public Comparator getComparator() {
		return null;
	}

	@Override
	public TemporalType getTemporalPrecision() {
		return null;
	}

	@Override
	public Map getLocalTypeParameters() {
		return null;
	}

	@Override
	public boolean isId() {
		return false;
	}

	@Override
	public boolean isVersion() {
		return false;
	}

	@Override
	public boolean isNationalized() {
		return false;
	}

	@Override
	public boolean isLob() {
		return false;
	}

	@Override
	public EnumType getEnumeratedType() {
		return null;
	}

	@Override
	public TypeConfiguration getTypeConfiguration() {
		return null;
	}
}
