package org.hibernate.hql;

import org.hibernate.type.Type;
import java.util.Set;

/**
 * Defines available information about the parameters encountered during
 * query translation.
 *
 * @author <a href="mailto:steve@hibernate.org">Steve Ebersole </a>
 */
public interface ParameterTranslations {

	public boolean supportsOrdinalParameterMetadata();

	public int getOrdinalParameterCount();

	public int getOrdinalParameterSqlLocation(int ordinalPosition);

	public Type getOrdinalParameterExpectedType(int ordinalPosition);

	public Set getNamedParameterNames();

	public int[] getNamedParameterSqlLocations(String name);

	public Type getNamedParameterExpectedType(String name);
}
