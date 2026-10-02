package org.hibernate.processor;

/**
 * {@code RuntimeException} used for errors during meta model generation.
 *
 * @author Hardy Ferentschik
 */
public class MetaModelGenerationException extends RuntimeException {
	public MetaModelGenerationException() {
		super();
	}

	public MetaModelGenerationException(String message) {
		super( message );
	}
}
