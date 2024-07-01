/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.generator.internal;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.hibernate.dialect.Dialect;
import org.hibernate.generator.CompositeGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.Generator;
import org.hibernate.generator.OnExecutionGenerator;

public class OnExecutionCompositeGenerator implements OnExecutionGenerator, CompositeGenerator {

	final List<Generator> generators;
	final EnumSet<EventType> eventTypes;
	final boolean writePropertyValue;
	final boolean referenceColumnsInSql;
	final String[] referencedColumnValues;

	public OnExecutionCompositeGenerator(
			List<Generator> generators,
			EnumSet<EventType> eventTypes,
			boolean writePropertyValue,
			boolean referenceColumnsInSql, String[] referencedColumnValues) {
		this.generators = generators;
		this.eventTypes = eventTypes;
		this.writePropertyValue = writePropertyValue;
		this.referenceColumnsInSql = referenceColumnsInSql;
		this.referencedColumnValues = referencedColumnValues;
	}

	@Override
	public EnumSet<EventType> getEventTypes() {
		return eventTypes;
	}

	@Override
	public boolean referenceColumnsInSql(Dialect dialect) {
		return referenceColumnsInSql;
	}

	@Override
	public boolean writePropertyValue() {
		return writePropertyValue;
	}

	@Override
	public String[] getReferencedColumnValues(Dialect dialect) {
		return referencedColumnValues;
	}

	@Override
	public boolean generatedOnExecution() {
		return true;
	}

	@Override
	public Generator getGenerator(int attributeStateArrayPosition) {
		return generators.get( attributeStateArrayPosition );
	}
}
