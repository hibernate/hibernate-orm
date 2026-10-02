package org.hibernate.query.named.spi;

import java.util.function.Consumer;

import org.hibernate.query.internal.ResultSetMappingResolutionContext;
import org.hibernate.query.results.spi.FetchBuilder;

/**
 * @author Steve Ebersole
 */
public interface FetchMementoBasic extends FetchMemento {
	@Override
	FetchBuilder resolve(
			Parent parent,
			Consumer<String> querySpaceConsumer,
			ResultSetMappingResolutionContext context);
}
