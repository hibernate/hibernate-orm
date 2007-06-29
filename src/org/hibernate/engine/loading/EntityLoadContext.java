package org.hibernate.engine.loading;

import java.sql.ResultSet;
import java.util.List;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * {@inheritDoc}
 *
 * @author Steve Ebersole
 */
public class EntityLoadContext {
	private static final Log log = LogFactory.getLog( EntityLoadContext.class );

	private final LoadContexts loadContexts;
	private final ResultSet resultSet;
	private final List hydratingEntities = new ArrayList( 20 ); // todo : need map? the prob is a proper key, right?

	public EntityLoadContext(LoadContexts loadContexts, ResultSet resultSet) {
		this.loadContexts = loadContexts;
		this.resultSet = resultSet;
	}

	void cleanup() {
		if ( !hydratingEntities.isEmpty() ) {
			log.warn( "On CollectionLoadContext#clear, hydratingEntities contained [" + hydratingEntities.size() + "] entries" );
		}
		hydratingEntities.clear();
	}


	public String toString() {
		return super.toString() + "<rs=" + resultSet + ">";
	}

}
