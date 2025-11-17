/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.enhanced;

import java.util.Properties;

import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.id.enhanced.DatabaseStructure;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.type.Type;

/**
 * Revision number generator has to produce values in ascending order (gaps may occur).
 * <p>
 * This generator is only applicable when {@code USE_REVISION_ENTITY_WITH_NATIVE_ID} is {@code false} in the
 * bootstrapping configuration properties.
 *
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 * @author Chris Cranford
 */
public class OrderedSequenceGenerator extends SequenceStyleGenerator {
	@Override
	protected DatabaseStructure buildSequenceStructure(
			Type type,
			Properties params,
			JdbcEnvironment jdbcEnvironment,
			QualifiedName sequenceName,
			int initialValue,
			int incrementSize) {
		final Object noCacheValue = params.get( "nocache" );
		final boolean noCache = Boolean.TRUE.equals( noCacheValue )
				|| noCacheValue instanceof String && Boolean.parseBoolean( noCacheValue.toString() );
		return new OrderedSequenceStructure(
				jdbcEnvironment,
				sequenceName,
				initialValue,
				incrementSize,
				noCache,
				type.getReturnedClass()
		);
	}
}
