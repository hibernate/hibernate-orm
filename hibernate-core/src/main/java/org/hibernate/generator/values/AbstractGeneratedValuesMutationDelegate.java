/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.generator.values;

import org.hibernate.dialect.Dialect;
import org.hibernate.generator.EventType;
import org.hibernate.generator.values.internal.GeneratedValuesMappingProducer;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMappingProducer;

import static org.hibernate.generator.values.internal.GeneratedValuesHelper.createMappingProducer;

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
		this.jdbcValuesMappingProducer =
				createMappingProducer( persister, timing, supportsArbitraryValues, supportsRowId );
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
