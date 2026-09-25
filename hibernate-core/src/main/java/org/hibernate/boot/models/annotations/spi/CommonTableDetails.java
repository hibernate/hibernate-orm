package org.hibernate.boot.models.annotations.spi;

/**
 * Information which is common across all table annotations
 *
 * @author Steve Ebersole
 */
public interface CommonTableDetails
		extends DatabaseObjectDetails, UniqueConstraintCollector, IndexCollector, Commentable, Optionable {
	/**
	 * The table name
	 */
	String name();

	/**
	 * Setter for {@linkplain #name()}
	 */
	void name(String name);
}
