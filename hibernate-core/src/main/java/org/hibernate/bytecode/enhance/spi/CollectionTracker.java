package org.hibernate.bytecode.enhance.spi;

/**
 * Interface to be implemented by collection trackers that hold the expected size od collections, a simplified {@code Map<String, int>}.
 *
 * @author Luis Barreiro
 */
public interface CollectionTracker {

	void add(String name, int size);

	int getSize(String name);
}
