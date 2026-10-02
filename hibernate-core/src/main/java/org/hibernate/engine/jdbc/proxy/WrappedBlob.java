package org.hibernate.engine.jdbc.proxy;

import java.sql.Blob;

/**
 * Contract for {@link Blob} wrappers.
 *
 * @author Steve Ebersole
 */
public interface WrappedBlob {
	/**
	 * Retrieve the wrapped {@link Blob} reference
	 *
	 * @return The wrapped {@link Blob} reference
	 */
	Blob getWrappedBlob();
}
