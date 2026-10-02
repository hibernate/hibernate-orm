package org.hibernate.query.named.spi;

import java.util.function.Consumer;

import org.hibernate.query.internal.ResultSetMappingResolutionContext;
import org.hibernate.query.results.spi.ResultBuilderEntityValued;

/**
 * @author Steve Ebersole
 */
public interface ResultMementoEntity extends ResultMemento {
	@Override
	ResultBuilderEntityValued resolve(
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context);
}
