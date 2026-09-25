package org.hibernate.boot.model.source.spi;

import org.hibernate.Remove;

import org.hibernate.boot.model.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
@Remove
public interface JavaTypeDescriptorResolvable {
	void resolveJavaTypeDescriptor(JavaTypeDescriptor descriptor);
}
