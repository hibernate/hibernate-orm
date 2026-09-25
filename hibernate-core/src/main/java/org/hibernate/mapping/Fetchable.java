package org.hibernate.mapping;
import org.hibernate.engine.FetchStyle;

/**
 * Any mapping with an outer-join attribute
 *
 * @author Gavin King
 */
public interface Fetchable {
	FetchStyle getFetchStyle();
	void setFetchStyle(FetchStyle fetchStyle);
	boolean isLazy();
	void setLazy(boolean lazy);
}
