/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal.util;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Mutable object reference.  Mainly useful with anonymous code blocks
 * and lambdas.
 *
 * @author Steve Ebersole
 * @author Andrea Boriero
 */
public class MutableObject<T> {
	private T reference;

	public T get() {
		return reference;
	}

	public boolean isSet() {
		return reference != null;
	}

	public boolean isNotSet() {
		return reference == null;
	}

	public void set(T reference) {
		this.reference = reference;
	}

	public void set(T reference, Consumer<T> existingConsumer) {
		if ( this.reference != null ) {
			existingConsumer.accept( this.reference );
		}

		this.reference = reference;
	}

	public void set(T reference, BiConsumer<T,T> existingConsumer) {
		if ( this.reference != null ) {
			existingConsumer.accept( reference, this.reference );
		}

		this.reference = reference;
	}

	public void setIfNot(T reference) {
		if ( this.reference == null ) {
			this.reference = reference;
		}
	}

	public void setIfNot(T reference, Supplier<RuntimeException> overwriteHandler) {
		if ( this.reference == null ) {
			this.reference = reference;
		}
		else {
			throw overwriteHandler.get();
		}
	}

	public void setIfNot(Supplier<T> referenceSupplier) {
		if ( this.reference == null ) {
			this.reference = referenceSupplier.get();
		}
	}

	public void setIfNot(Supplier<T> referenceSupplier, Supplier<RuntimeException> overwriteHandler) {
		if ( this.reference == null ) {
			this.reference = referenceSupplier.get();
		}
		else {
			throw overwriteHandler.get();
		}
	}
}
