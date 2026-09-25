package org.hibernate.boot.model.relational;

/**
 * Identifies metamodel objects that can produce {@link Exportable} relational stuff.
 *
 * @author Steve Ebersole
 */
public interface ExportableProducer {
	/**
	 * Register the contained exportable things to the {@link Database}
	 *
	 * @param database The database instance
	 */
	void registerExportables(Database database);
}
