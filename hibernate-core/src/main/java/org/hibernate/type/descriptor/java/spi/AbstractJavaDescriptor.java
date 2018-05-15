/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java.spi;

import java.util.Comparator;
import java.util.Objects;

import org.hibernate.type.descriptor.java.MutabilityPlan;

import org.jboss.logging.Logger;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractJavaDescriptor<T> implements JavaTypeDescriptor<T> {
	private static final Logger log = Logger.getLogger( AbstractJavaDescriptor.class );

	private final String typeName;
	private final MutabilityPlan mutabilityPlan;
	private final Comparator comparator;

	private Class javaType;

	public AbstractJavaDescriptor(
			String typeName,
			Class javaType,
			MutabilityPlan mutabilityPlan,
			Comparator comparator) {
		this.typeName = typeName;
		this.javaType = javaType;
		this.mutabilityPlan = mutabilityPlan;
		this.comparator = comparator;
	}

	protected void setJavaType(Class javaType) {
		// meant to allow late resolution of the actual Java class (delayed ClassLoader access)
		log.debugf( "setting Java type to [" + javaType + "]; was [" + this.javaType + "]" );
		this.javaType = javaType;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<T> getJavaType() {
		return javaType;
	}

	@Override
	public String getTypeName() {
		return typeName;
	}

	@Override
	@SuppressWarnings("unchecked")
	public MutabilityPlan<T> getMutabilityPlan() {
		return mutabilityPlan;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Comparator<T> getComparator() {
		return comparator;
	}

	@Override
	public int extractHashCode(T value) {
		return value.hashCode();
	}

	@Override
	public boolean areEqual(T one, T another) {
		return Objects.equals( one, another );
	}
}
