/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.basic.bitset;

import java.util.BitSet;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;

/**
 * @author Vlad Mihalcea
 */
//tag::basic-custom-type-BitSetType-example[]
public class BitSetType
		extends AbstractSingleColumnStandardBasicType<BitSet> {

	public static final BitSetType INSTANCE = new BitSetType();

	public BitSetType() {
		super( VarcharJdbcType.INSTANCE, BitSetJavaType.INSTANCE );
	}

	@Override
	public String getName() {
		return "bitset";
	}

}
//end::basic-custom-type-BitSetType-example[]
