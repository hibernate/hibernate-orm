/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple;

import org.hibernate.Session;
import org.hibernate.annotations.TenantId;

/**
 * Value generation implementation for {@link TenantId}.
 *
 * @author Gavin King
 */
public class TenantIdGeneration implements AnnotationValueGeneration<TenantId>, ValueGenerator<Object> {

	public static final String FILTER_NAME = "_tenantId";
	public static final String PARAMETER_NAME = "tenantId";

	@Override
	public void initialize(TenantId annotation, Class<?> propertyType) {
	}

	@Override
	public GenerationTiming getGenerationTiming() {
		return GenerationTiming.INSERT;
	}

	@Override
	public ValueGenerator<?> getValueGenerator() {
		return this;
	}

	@Override
	public Object generateValue(Session session, Object owner) {
		return session.getTenantIdentifier();
	}

	@Override
	public boolean referenceColumnInSql() {
		return false;
	}

	@Override
	public String getDatabaseGeneratedReferencedColumnValue() {
		return null;
	}
}
