/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.generated.temporals;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.tuple.AnnotationValueGeneration;
import org.hibernate.tuple.GenerationTiming;
import org.hibernate.tuple.ValueGenerator;

/**
 * Proposal for making `@GeneratedValueGeneration` work for update (they don't work in 5.x either)
 *
 * @see ProposedGenerated
 *
 * @author Steve Ebersole
 */
public class ProposedGeneratedValueGeneration implements AnnotationValueGeneration<ProposedGenerated> {
	private GenerationTiming timing;
	private String defaultValue;

	@Override
	public void initialize(ProposedGenerated annotation, Class propertyType) {
		timing = annotation.timing();

		final String defaultValue = annotation.sqlDefaultValue();
		this.defaultValue = StringHelper.isEmpty( defaultValue )
				? null
				: defaultValue;
	}

	@Override
	public GenerationTiming getGenerationTiming() {
		return timing;
	}

	@Override
	public ValueGenerator<?> getValueGenerator() {
		return null;
	}

	@Override
	public boolean referenceColumnInSql() {
		return defaultValue != null;
	}

	@Override
	public String getDatabaseGeneratedReferencedColumnValue() {
		return defaultValue;
	}
}
