/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import org.hibernate.query.sqm.CastType;

/**
 * Utility for decoding boolean representations.
 *
 * @author Christian Beikov
 */
public final class BooleanDecoder {

	public static String toInteger(CastType from) {
		return switch ( from ) {
			case BOOLEAN -> "decode(?1,false,0,true,1,null)";
			case YN_BOOLEAN -> "decode(?1,'Y',1,'N',0,null)";
			case TF_BOOLEAN -> "decode(?1,'T',1,'F',0,null)";
			default -> null;
		};
	}

	public static String toBoolean(CastType from) {
		return switch ( from ) {
			case STRING -> "decode(?1,'T',true,'F',false,'Y',true,'N',false,null)";
			case YN_BOOLEAN -> "decode(?1,'Y',true,'N',false,null)";
			case TF_BOOLEAN -> "decode(?1,'T',true,'F',false,null)";
			case INTEGER, LONG, INTEGER_BOOLEAN -> "decode(abs(sign(?1)),1,true,0,false,null)";
			default -> null;
		};
	}

	public static String toIntegerBoolean(CastType from) {
		return switch ( from ) {
			case STRING -> "decode(?1,'T',1,'F',0,'Y',1,'N',0,null)";
			case YN_BOOLEAN -> "decode(?1,'Y',1,'N',0,null)";
			case TF_BOOLEAN -> "decode(?1,'T',1,'F',0,null)";
			case INTEGER, LONG -> "abs(sign(?1))";
			default -> null;
		};
	}

	public static String toYesNoBoolean(CastType from) {
		return switch ( from ) {
			case STRING -> "decode(?1,'T','Y','F','N','Y','Y','N','N',null)";
			case INTEGER_BOOLEAN -> "decode(?1,1,'Y',0,'N',null)";
			case TF_BOOLEAN -> "decode(?1,'T','Y','F','N',null)";
			case BOOLEAN -> "decode(?1,true,'Y',false,'N',null)";
			case INTEGER, LONG -> "decode(abs(sign(?1)),1,'Y',0,'N',null)";
			default -> null;
		};
	}

	public static String toTrueFalseBoolean(CastType from) {
		return switch ( from ) {
			case STRING -> "decode(?1,'T','T','F','F','Y','T','N','F',null)";
			case INTEGER_BOOLEAN -> "decode(?1,1,'T',0,'F',null)";
			case YN_BOOLEAN -> "decode(?1,'Y','T','N','F',null)";
			case BOOLEAN -> "decode(?1,true,'T',false,'F',null)";
			case INTEGER, LONG -> "decode(abs(sign(?1)),1,'T',0,'F',null)";
			default -> null;
		};
	}

	public static String toString(CastType from) {
		return switch ( from ) {
			case INTEGER_BOOLEAN -> "decode(?1,0,'false',1,'true',null)";
			case TF_BOOLEAN -> "decode(?1,'T','true','F','false',null)";
			case YN_BOOLEAN -> "decode(?1,'Y','true','N','false',null)";
			case BOOLEAN -> "decode(?1,true,'true',false,'false',null)";
			default -> null;
		};
	}
}
