package org.hibernate.persister.filter;

import jakarta.annotation.Nullable;
/**
 *
 * @author Rob Worsnop
 *
 */
public interface FilterAliasGenerator {
	@Nullable
	String getAlias(@Nullable String table);
}
