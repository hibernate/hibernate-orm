/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.generator.values;

import org.hibernate.dialect.Dialect;
import org.hibernate.generator.EventType;
import org.hibernate.generator.values.internal.GeneratedValuesHelper;
import org.hibernate.generator.values.internal.GeneratedValuesMappingProducer;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducer;

/**
 * @author Marco Belladelli
 */
public abstract class AbstractGeneratedValuesMutationDelegate implements GeneratedValuesMutationDelegate {
	protected final EntityPersister persister;
	private final EventType timing;
	private final boolean supportsArbitraryValues;
	private final boolean supportsRowId;
	protected final GeneratedValuesMappingProducer jdbcValuesMappingProducer;

	public AbstractGeneratedValuesMutationDelegate(EntityPersister persister, EventType timing) {
		this( persister, timing, true, true );
	}

	public AbstractGeneratedValuesMutationDelegate(
			EntityPersister persister,
			EventType timing,
			boolean supportsArbitraryValues,
			boolean supportsRowId) {
		this.persister = persister;
		this.timing = timing;
		this.supportsArbitraryValues = supportsArbitraryValues;
		this.supportsRowId = supportsRowId;
		this.jdbcValuesMappingProducer = GeneratedValuesHelper.createMappingProducer(
				persister,
				timing,
				supportsArbitraryValues,
				supportsRowId
		);
	}

	@Override
	public EventType getTiming() {
		return timing;
	}

	@Override
	public final boolean supportsArbitraryValues() {
		return supportsArbitraryValues;
	}

	@Override
	public final boolean supportsRowId() {
		return supportsRowId;
	}

	@Override
	public JdbcValuesMappingProducer getGeneratedValuesMappingProducer() {
		return jdbcValuesMappingProducer;
	}

	protected Dialect dialect() {
		return persister.getFactory().getJdbcServices().getDialect();
	}
}
