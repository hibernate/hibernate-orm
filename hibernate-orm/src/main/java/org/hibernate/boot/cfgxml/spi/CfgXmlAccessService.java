/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.cfgxml.spi;

import org.hibernate.service.Service;

/**
 * Allows access to any {@code cfg.xml} files specified for bootstrapping.
 *
 * @author Steve Ebersole
 */
public interface CfgXmlAccessService extends Service {
	public static final String LOADED_CONFIG_KEY = "hibernate.boot.CfgXmlAccessService.key";

	LoadedConfig getAggregatedConfig();
}
