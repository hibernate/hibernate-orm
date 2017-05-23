/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
 * <p/>
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
		return new OrderedSequenceStructure( jdbcEnvironment, sequenceName, initialValue, incrementSize, type.getReturnedClass() );
	}
}
