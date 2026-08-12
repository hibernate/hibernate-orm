/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations.processing;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.lock.internal.NoLockingSupport;
import org.hibernate.dialect.lock.spi.LockingSupport;

/**
 * A generic {@linkplain Dialect dialect} for ANSI-like SQL.
 * Used by default in the HQL Query Validator.
 *
 * @author Gavin King
 *
 * @see CheckHQL#dialect
 */
public class GenericDialect extends Dialect {
	public GenericDialect() {
		super( (DatabaseVersion) null );
	}

	@Override
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry( functionContributions );
		var functionFactory = new CommonFunctionFactory( functionContributions );

		functionFactory.cot();
		functionFactory.log2();
		functionFactory.radians();
		functionFactory.degrees();
		functionFactory.sinh();
		functionFactory.cosh();
		functionFactory.tanh();
		functionFactory.moreHyperbolic();

		functionFactory.trunc();
		functionFactory.rand();
		functionFactory.median();
		functionFactory.stddev();
		functionFactory.variance();
		functionFactory.stddevPopSamp();
		functionFactory.varPopSamp();
		functionFactory.covarPopSamp();
		functionFactory.corr();
		functionFactory.regrLinearRegressionAggregates();
		functionFactory.varianceSamp();
		functionFactory.pi();

		functionFactory.soundex();
		functionFactory.trim2();
		functionFactory.reverse();
		functionFactory.space();
		functionFactory.repeat();
		functionFactory.md5();
		functionFactory.initcap();
		functionFactory.instr();
		functionFactory.substr();
		functionFactory.translate();

		functionFactory.bitand();
		functionFactory.bitor();
		functionFactory.bitxor();
		functionFactory.bitnot();
		functionFactory.bitAndOr();
		functionFactory.everyAny();

		functionFactory.yearMonthDay();
		functionFactory.hourMinuteSecond();
		functionFactory.dayofweekmonthyear();
		functionFactory.dayOfWeekMonthYear();
		functionFactory.daynameMonthname();
		functionFactory.weekQuarter();
		functionFactory.lastDay_eomonth();
		functionFactory.ceiling_ceil();
		functionFactory.toCharNumberDateTimestamp();
		functionFactory.dateTimeTimestamp();
		functionFactory.utcDateTimeTimestamp();
		functionFactory.currentUtcdatetimetimestamp();
		functionFactory.week_weekofyear();

		functionFactory.rownumRowid();
		functionFactory.rownumInstOrderbyGroupbyNum();
		functionFactory.makedateMaketime();
		functionFactory.makeDateTimeTimestamp();
		functionFactory.sysdate();
		functionFactory.systimestamp();
		functionFactory.localtimeLocaltimestamp();

		functionFactory.octetLength();
		functionFactory.bitLength();
		functionFactory.position();
		functionFactory.insert();
		functionFactory.overlay();
		functionFactory.ascii();
		functionFactory.char_chr();
		functionFactory.datepartDatename();
		functionFactory.nowCurdateCurtime();

		functionFactory.listagg(null);
		functionFactory.inverseDistributionOrderedSetAggregates();
		functionFactory.hypotheticalOrderedSetAggregates();
		functionFactory.windowFunctions();

		functionFactory.square();
		functionFactory.cbrt();
		functionFactory.crc32();
		functionFactory.hex("hex(?1)");
		functionFactory.sha();
		functionFactory.sha1();
		functionFactory.sha2();

		functionFactory.timestampaddAndDiff( this );
		functionFactory.datediff();
		functionFactory.adddateSubdateAddtimeSubtime();
		functionFactory.addMonths();
		functionFactory.monthsBetween();
		functionFactory.daysBetween();
		functionFactory.secondsBetween();
		functionFactory.yearsMonthsDaysHoursMinutesSecondsBetween();
		functionFactory.addYearsMonthsDaysHoursMinutesSeconds();
		functionFactory.format_formatdatetime();
		functionFactory.collate();
		functionFactory.dateTrunc_datetrunc();
		functionFactory.regexpLike();

		functionFactory.array();
		functionFactory.arrayAggregate();
		functionFactory.arrayContains_postgresql();
		functionFactory.arrayIntersects_postgresql();
		functionFactory.arrayPosition_postgresql();
		functionFactory.arrayPositions_postgresql();
		functionFactory.arrayLength_cardinality();
		functionFactory.arrayConcat_postgresql();
		functionFactory.arrayPrepend_postgresql();
		functionFactory.arrayAppend_postgresql();
		functionFactory.arrayGet_bracket();
		functionFactory.arraySet_unnest();
		functionFactory.arrayRemove();
		functionFactory.arrayRemoveIndex_unnest( false );
		functionFactory.arraySlice_operator();
		functionFactory.arrayReplace();
		functionFactory.arrayTrim_trim_array();
		functionFactory.arrayReverse();
		functionFactory.arraySort();
		functionFactory.arrayFill_postgresql();
		functionFactory.arrayToString_postgresql();

		functionFactory.jsonValue_postgresql( false );
		functionFactory.jsonQuery_postgresql();
		functionFactory.jsonExists_postgresql();
		functionFactory.jsonObject_postgresql( false );
		functionFactory.jsonArray_postgresql( false );
		functionFactory.jsonArrayAgg_postgresql( false );
		functionFactory.jsonObjectAgg_postgresql( false );
		functionFactory.jsonSet_postgresql();
		functionFactory.jsonRemove_postgresql();
		functionFactory.jsonReplace_postgresql();
		functionFactory.jsonInsert_postgresql();
		functionFactory.jsonMergepatch_postgresql();
		functionFactory.jsonArrayAppend_postgresql( false );
		functionFactory.jsonArrayInsert_postgresql();

		functionFactory.xmlelement();
		functionFactory.xmlcomment();
		functionFactory.xmlforest();
		functionFactory.xmlconcat();
		functionFactory.xmlpi();
		functionFactory.xmlquery_postgresql();
		functionFactory.xmlexists();
		functionFactory.xmlagg();

		functionFactory.unnest_postgresql( true );
		functionFactory.generateSeries( null, "ordinality", false );
		functionFactory.jsonTable_postgresql();
		functionFactory.xmltable( false );
	}

	@Override
	public LockingSupport getLockingSupport() {
		return NoLockingSupport.NO_LOCKING_SUPPORT;
	}
}
