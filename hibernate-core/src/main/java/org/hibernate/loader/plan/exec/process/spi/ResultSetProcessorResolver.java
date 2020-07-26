/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.exec.process.spi;

import org.hibernate.loader.plan.exec.process.internal.ResultSetProcessorImpl;
import org.hibernate.loader.plan.exec.spi.AliasResolutionContext;
import org.hibernate.loader.plan.spi.LoadPlan;

public interface ResultSetProcessorResolver {

	ResultSetProcessor resolveResultSetProcessor(
			LoadPlan loadPlan,
			AliasResolutionContext aliasResolutionContext,
			ReaderCollector readerCollector,
			boolean shouldUseOptionalEntityInstance,
			boolean hadSubselectFetches
	);

	ResultSetProcessorResolver DEFAULT =
			(loadPlan, aliasResolutionContext, readerCollector, shouldUseOptionalEntityInstance, hadSubselectFetches) ->
					new ResultSetProcessorImpl(
							loadPlan,
							aliasResolutionContext,
							readerCollector.buildRowReader(),
							shouldUseOptionalEntityInstance,
							hadSubselectFetches
	);
}
