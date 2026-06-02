/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.nullaway;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.uber.nullaway.LibraryModels;

/**
 * Custom NullAway LibraryModels for javax.lang.model APIs.
 * <p>
 * This class provides nullability annotations for third-party library methods
 * that are not annotated. It is used during compilation only and excluded from
 * the final packaged artifact.
 *
 * @author Hibernate Team
 */
public class JavaxLangModelLibraryModels implements LibraryModels {

	@Override
	public ImmutableSetMultimap<MethodRef, Integer> failIfNullParameters() {
		return ImmutableSetMultimap.of();
	}

	@Override
	public ImmutableSetMultimap<MethodRef, Integer> explicitlyNullableParameters() {
		return ImmutableSetMultimap.of();
	}

	@Override
	public ImmutableSetMultimap<MethodRef, Integer> nonNullParameters() {
		return ImmutableSetMultimap.of();
	}

	@Override
	public ImmutableSetMultimap<MethodRef, Integer> nullImpliesTrueParameters() {
		return ImmutableSetMultimap.of();
	}

	@Override
	public ImmutableSetMultimap<MethodRef, Integer> nullImpliesFalseParameters() {
		return ImmutableSetMultimap.of();
	}

	@Override
	public ImmutableSetMultimap<MethodRef, Integer> nullImpliesNullParameters() {
		return ImmutableSetMultimap.of();
	}

	@Override
	public ImmutableSet<MethodRef> nullableReturns() {
		return ImmutableSet.of();
	}

	@Override
	public ImmutableSet<MethodRef> nonNullReturns() {
		return ImmutableSet.<MethodRef>builder()
				// These element types always have an enclosing element in valid Java programs
				.add(MethodRef.methodRef("javax.lang.model.element.TypeElement", "getEnclosingElement()"))
				// TODO this entry, and only this one, seems to be ignored by NullAway for some reason.
				.add(MethodRef.methodRef("javax.lang.model.element.ExecutableElement", "getEnclosingElement()"))
				.add(MethodRef.methodRef("javax.lang.model.element.VariableElement", "getEnclosingElement()"))
				.add(MethodRef.methodRef("javax.lang.model.element.TypeParameterElement", "getEnclosingElement()"))
				.build();
	}

	@Override
	public ImmutableSetMultimap<MethodRef, Integer> castToNonNullMethods() {
		return ImmutableSetMultimap.of();
	}
}
