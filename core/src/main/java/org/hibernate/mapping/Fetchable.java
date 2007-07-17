//$Id: Fetchable.java 6586 2005-04-28 08:35:10Z oneovthafew $
package org.hibernate.mapping;

import org.hibernate.FetchMode;

/**
 * Any mapping with an outer-join attribute
 * @author Gavin King
 */
public interface Fetchable {
	public FetchMode getFetchMode();
	public void setFetchMode(FetchMode joinedFetch);
	public boolean isLazy();
	public void setLazy(boolean lazy);
}
