/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.type.spi;

import java.util.Map;

import org.hibernate.type.spi.basic.BasicTypeParameters;
import org.hibernate.type.descriptor.spi.JdbcRecommendedSqlTypeMappingContext;

/**
 * Describes the context in which a BasicType is being defined.  Provides the process
 * of producing a BasicType with site-local information it needs.
 *
 * @author Steve Ebersole
 */
public interface BasicTypeSiteContext extends JdbcRecommendedSqlTypeMappingContext, BasicTypeParameters {
	Map getLocalTypeParameters();

	boolean isId();
	boolean isVersion();
}
