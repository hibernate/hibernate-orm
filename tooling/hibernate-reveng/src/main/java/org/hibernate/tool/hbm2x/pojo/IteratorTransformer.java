package org.hibernate.tool.hbm2x.pojo;

import java.util.Iterator;

public abstract class IteratorTransformer<T> implements Iterator<String> {

	private Iterator<T> delegate;

	public IteratorTransformer(Iterator<T> delegate) {
		this.delegate = delegate;
	}

	public boolean hasNext() {
		return delegate.hasNext();
	}

	public String next() {
		return transform(delegate.next());
	}

	public abstract String transform(T object);

	public void remove() {
		delegate.remove();
	}

}
