/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.enhance.internal.bytebuddy;

import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BridgeMembersClassInfo {
	private final Class<?> clazz;
	private final List<String> propertyNames = new ArrayList<>();
	private final List<Member> getters = new ArrayList<>();
	private final List<Member> setters = new ArrayList<>();

	public BridgeMembersClassInfo(Class<?> clazz) {
		this.clazz = Objects.requireNonNull(clazz);
	}

	public Class<?> getClazz() {
		return clazz;
	}

	public Iterable<Member> gettersIterable() {
		return getters;
	}

	public Iterable<Member> settersIterable() {
		return setters;
	}

	public boolean containsGetter(Member getter) {
		return getters.contains( getter );
	}

	public boolean containsSetter(Member setter) {
		return setters.contains( setter );
	}

	public void addGetter(Member getter) {
		getters.add( getter );
	}

	public void addSetter(Member setter) {
		setters.add( setter );
	}

	public void addProperty(String propertyName) {
		propertyNames.add( propertyName );
	}

	public boolean propertyNamesIsEmpty() {
		return propertyNames.isEmpty();
	}

	public String encodeName() {
		return NameEncodeHelper.encodeName(
				propertyNames.toArray( new String[0] ),
				getters.toArray( new Member[0] ),
				setters.toArray( new Member[0] )
		);
	}
}
