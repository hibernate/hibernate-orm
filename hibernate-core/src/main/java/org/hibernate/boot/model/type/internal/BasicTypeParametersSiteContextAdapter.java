/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.type.internal;

import java.util.Comparator;
import javax.persistence.TemporalType;

import org.hibernate.boot.model.type.spi.BasicTypeSiteContext;
import org.hibernate.cfg.AttributeConverterDefinition;
import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.MutabilityPlan;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;
import org.hibernate.type.spi.BasicTypeParameters;

/**
 * @author Steve Ebersole
 */
public class BasicTypeParametersSiteContextAdapter implements BasicTypeParameters {
	private BasicTypeSiteContext delegate;

	public BasicTypeParametersSiteContextAdapter(BasicTypeSiteContext delegate) {
		this.delegate = delegate;
	}

	@Override
	public BasicJavaDescriptor getJavaTypeDescriptor() {
		return delegate.getJavaTypeDescriptor();
	}

	@Override
	public SqlTypeDescriptor getSqlTypeDescriptor() {
		return delegate.getSqlTypeDescriptor();
	}

	@Override
	public AttributeConverterDefinition  getAttributeConverterDefinition() {
		return delegate.getAttributeConverterDefinition();
	}

	@Override
	public MutabilityPlan getMutabilityPlan() {
		return delegate.getMutabilityPlan();
	}

	@Override
	public Comparator getComparator() {
		return delegate.getComparator();
	}

	@Override
	public TemporalType getTemporalPrecision() {
		return null;
	}
}
