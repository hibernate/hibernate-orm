/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.dialect.Dialect;

import static org.hibernate.annotations.GenerationTime.INSERT_OR_UPDATE;
import static org.hibernate.internal.util.StringHelper.isEmpty;

/**
 * A {@link AnnotationValueGeneration} which marks a property as generated in the database.
 *
 * @author Steve Ebersole
 * @author Gunnar Morling
 */
public class GeneratedValueGeneration implements InDatabaseGenerator {

	private GenerationTime timing;
	private boolean writable;
	private String[] sql;

	public GeneratedValueGeneration() {
	}

	public GeneratedValueGeneration(GenerationTime event) {
		this.timing = event;
	}

	public GeneratedValueGeneration(Generated annotation) {
		timing = annotation.event() == INSERT_OR_UPDATE ? annotation.value() : annotation.event();
		sql = isEmpty( annotation.sql() ) ? null : new String[] { annotation.sql() };
		writable = annotation.writable() || sql != null;
	}

	@Override
	public boolean generatedOnInsert() {
		return timing.includesInsert();
	}

	@Override
	public boolean generatedOnUpdate() {
		return timing.includesUpdate();
	}

	@Override
	public boolean referenceColumnsInSql(Dialect dialect) {
		return writable;
	}

	@Override
	public String[] getReferencedColumnValues(Dialect dialect) {
		return sql;
	}

	@Override
	public boolean writePropertyValue() {
		return writable && sql==null;
	}
}

