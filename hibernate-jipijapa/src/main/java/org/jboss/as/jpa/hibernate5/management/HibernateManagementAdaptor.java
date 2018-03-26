/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.jboss.as.jpa.hibernate5.management;

import org.jipijapa.management.spi.Statistics;
import org.jipijapa.plugin.spi.ManagementAdaptor;

/**
 * Contains management support for Hibernate
 *
 * @author Scott Marlow
 */
public class HibernateManagementAdaptor implements ManagementAdaptor {

	// shared (per classloader) instance for all Hibernate 4.3 JPA deployments
	private static final HibernateManagementAdaptor INSTANCE = new HibernateManagementAdaptor();

	private final Statistics statistics = new HibernateStatistics();

	private static final String PROVIDER_LABEL = "hibernate-persistence-unit";
	private static final String VERSION = "Hibernate ORM 4.3.x";

	/**
	 * The management statistics are shared across all Hibernate 4 JPA deployments
	 *
	 * @return shared instance for all Hibernate 4 JPA deployments
	 */
	public static HibernateManagementAdaptor getInstance() {
		return INSTANCE;
	}

	@Override
	public String getIdentificationLabel() {
		return PROVIDER_LABEL;
	}

	@Override
	public String getVersion() {
		return VERSION;
	}

	@Override
	public Statistics getStatistics() {
		return statistics;
	}


}
