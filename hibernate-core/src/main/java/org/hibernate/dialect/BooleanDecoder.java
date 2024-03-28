/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
		switch ( from ) {
			case BOOLEAN:
				return "decode(?1,false,0,true,1,null)";
			case YN_BOOLEAN:
				return "decode(?1,'Y',1,'N',0,null)";
			case TF_BOOLEAN:
				return "decode(?1,'T',1,'F',0,null)";
		}
		return null;
	}

	public static String toBoolean(CastType from) {
		switch ( from ) {
			case STRING:
				return "decode(?1,'T',true,'F',false,'Y',true,'N',false,null)";
			case YN_BOOLEAN:
				return "decode(?1,'Y',true,'N',false,null)";
			case TF_BOOLEAN:
				return "decode(?1,'T',true,'F',false,null)";
			case INTEGER:
			case LONG:
			case INTEGER_BOOLEAN:
				return "decode(abs(sign(?1)),1,true,0,false,null)";
		}
		return null;
	}

	public static String toIntegerBoolean(CastType from) {
		switch ( from ) {
			case STRING:
				return "decode(?1,'T',1,'F',0,'Y',1,'N',0,null)";
			case YN_BOOLEAN:
				return "decode(?1,'Y',1,'N',0,null)";
			case TF_BOOLEAN:
				return "decode(?1,'T',1,'F',0,null)";
			case INTEGER:
			case LONG:
				return "abs(sign(?1))";
		}
		return null;
	}

	public static String toYesNoBoolean(CastType from) {
		switch ( from ) {
			case STRING:
				return "decode(?1,'T','Y','F','N','Y','Y','N','N',null)";
			case INTEGER_BOOLEAN:
				return "decode(?1,1,'Y',0,'N',null)";
			case TF_BOOLEAN:
				return "decode(?1,'T','Y','F','N',null)";
			case BOOLEAN:
				return "decode(?1,true,'Y',false,'N',null)";
			case INTEGER:
			case LONG:
				return "decode(abs(sign(?1)),1,'Y',0,'N',null)";
		}
		return null;
	}

	public static String toTrueFalseBoolean(CastType from) {
		switch ( from ) {
			case STRING:
				return "decode(?1,'T','T','F','F','Y','T','N','F',null)";
			case INTEGER_BOOLEAN:
				return "decode(?1,1,'T',0,'F',null)";
			case YN_BOOLEAN:
				return "decode(?1,'Y','T','N','F',null)";
			case BOOLEAN:
				return "decode(?1,true,'T',false,'F',null)";
			case INTEGER:
			case LONG:
				return "decode(abs(sign(?1)),1,'T',0,'F',null)";
		}
		return null;
	}

	public static String toString(CastType from) {
		switch ( from ) {
			case INTEGER_BOOLEAN:
				return "decode(?1,0,'false',1,'true',null)";
			case TF_BOOLEAN:
				return "decode(?1,'T','true','F','false',null)";
			case YN_BOOLEAN:
				return "decode(?1,'Y','true','N','false',null)";
			case BOOLEAN:
				return "decode(?1,true,'true',false,'false',null)";
		}
		return null;
	}
}
