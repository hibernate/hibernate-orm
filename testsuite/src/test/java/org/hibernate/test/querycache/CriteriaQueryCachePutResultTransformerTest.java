/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.test.querycache;

import junit.framework.Test;

import org.hibernate.CacheMode;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;

/**
 * @author Gail Badner
 */
public class CriteriaQueryCachePutResultTransformerTest extends CriteriaQueryCacheIgnoreResultTransformerTest {

	public CriteriaQueryCachePutResultTransformerTest(String str) {
		super( str );
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( CriteriaQueryCachePutResultTransformerTest.class );
	}

	protected CacheMode getQueryCacheMode() {
		return CacheMode.PUT;
	}

	protected boolean areDynamicNonLazyAssociationsChecked() {
		return false;
	}	

	public void testAliasToEntityMapNoProjectionList() {
		reportSkip( "Transformers.ALIAS_TO_ENTITY_MAP with Criteria fails when try put in cache",
				"Cache results using Transformers.ALIAS_TO_ENTITY_MAP with Criteria" );
	}

	public void testAliasToEntityMapNoProjectionListFailureExpected() throws Exception {
		super.testAliasToEntityMapNoProjectionList();
	}

	public void testAliasToEntityMapNoProjectionMultiAndNullList() {
		reportSkip( "Transformers.ALIAS_TO_ENTITY_MAP with Criteria fails when try put in cache",
				"Cache results using Transformers.ALIAS_TO_ENTITY_MAP with Criteria" );
	}
	public void testAliasToEntityMapNoProjectionMultiAndNullListFailureExpected() throws Exception {
		super.testAliasToEntityMapNoProjectionMultiAndNullList();
	}

	public void testAliasToEntityMapOneProjectionList() {
		reportSkip( "Transformers.ALIAS_TO_ENTITY_MAP with Criteria fails when try put in cache",
				"Cache results using Transformers.ALIAS_TO_ENTITY_MAP with Criteria" );
	}
	public void testAliasToEntityMapOneProjectionListFailureExpected() throws Exception {
		super.testAliasToEntityMapOneProjectionList();
	}

	public void testAliasToEntityMapMultiProjectionList() {
		reportSkip( "Transformers.ALIAS_TO_ENTITY_MAP with Criteria fails when try put in cache",
				"Cache results using Transformers.ALIAS_TO_ENTITY_MAP with Criteria" );
	}
	public void testAliasToEntityMapMultiProjectionListFailureExpected() throws Exception {
		super.testAliasToEntityMapMultiProjectionList();
	}

	public void testAliasToEntityMapMultiProjectionWithNullAliasList() {
		reportSkip( "Transformers.ALIAS_TO_ENTITY_MAP with Criteria fails when try put in cache",
				"Cache results using Transformers.ALIAS_TO_ENTITY_MAP with Criteria" );
	}
	public void testAliasToEntityMapMultiProjectionWithNullAliasListFailureExpected() throws Exception {
		super.testAliasToEntityMapMultiProjectionWithNullAliasList();
	}

	public void testAliasToEntityMapMultiAggregatedPropProjectionSingleResult() {
		reportSkip( "Transformers.ALIAS_TO_ENTITY_MAP with Criteria fails when try put in cache",
				"Cache results using Transformers.ALIAS_TO_ENTITY_MAP with Criteria" );
	}
	public void testAliasToEntityMapMultiAggregatedPropProjectionSingleResultFailureExpected() throws Exception {
		super.testAliasToEntityMapMultiAggregatedPropProjectionSingleResult();
	}

	public void testAliasToBeanDtoOneArgList() {
		reportSkip( "Transformers.aliasToBean with Criteria fails when try put in cache",
				"Cache results using Transformers.aliasToBean with Criteria" );
	}
	public void testAliasToBeanDtoOneArgListFailureExpected() throws Exception {
		super.testAliasToBeanDtoOneArgList();
	}

	public void testAliasToBeanDtoMultiArgList() {
		reportSkip( "Transformers.aliasToBean with Criteria fails when try put in cache",
				"Cache results using Transformers.aliasToBean with Criteria" );
	}
	public void testAliasToBeanDtoMultiArgListFailureExpected() throws Exception {
		super.testAliasToBeanDtoMultiArgList();
	}

	public void testAliasToBeanDtoLiteralArgList() {
		reportSkip( "Transformers.aliasToBean with Criteria fails when try put in cache",
				"Cache results using Transformers.aliasToBean with Criteria" );
	}
	public void testAliasToBeanDtoLiteralArgListFailureExpected() throws Exception {
		super.testAliasToBeanDtoLiteralArgList();
	}

	public void testAliasToBeanDtoWithNullAliasList() {
		reportSkip( "Transformers.aliasToBean with Criteria fails when try put in cache",
				"Cache results using Transformers.aliasToBean with Criteria" );
	}
	public void testAliasToBeanDtoWithNullAliasListFailureExpected() throws Exception {
		super.testAliasToBeanDtoWithNullAliasList();
	}

	public void testOneSelectNewList() {
		reportSkip( "Transformers.aliasToBean with Criteria fails when try put in cache",
				"Cache results using Transformers.aliasToBean with Criteria" );
	}
	public void testOneSelectNewListFailureExpected() throws Exception {
		super.testOneSelectNewList();
	}

	public void testMultiSelectNewList() {
		reportSkip( "Transformers.aliasToBean with Criteria fails when try put in cache",
				"Cache results using Transformers.aliasToBean with Criteria" );
	}
	public void testMultiSelectNewListFailureExpected() throws Exception {
		super.testMultiSelectNewList();
	}

	public void testMultiSelectNewWithLiteralList() {
		reportSkip( "Transformers.aliasToBean with Criteria fails when try put in cache",
				"Cache results using Transformers.aliasToBean with Criteria" );
	}
	public void testMultiSelectNewWithLiteralListFailureExpected() throws Exception {
		super.testMultiSelectNewWithLiteralList();
	}

	public void testMultiSelectNewListList() {
		reportSkip( "Transformers.aliasToBean with Criteria fails when try put in cache",
				"Cache results using Transformers.aliasToBean with Criteria" );
	}
	public void testMultiSelectNewListListFailureExpected() throws Exception {
		super.testMultiSelectNewListList();
	}

	public void testMultiSelectNewMapList() {
		reportSkip( "Transformers.aliasToBean with Criteria fails when try put in cache",
				"Cache results using Transformers.aliasToBean with Criteria" );
	}
	public void testMultiSelectNewMapListFailureExpected() throws Exception {
		super.testMultiSelectNewMapList();
	}

	public void testSelectNewEntityConstructorList() {
		reportSkip( "Transformers.aliasToBean with Criteria fails when try put in cache",
				"Cache results using Transformers.aliasToBean with Criteria" );
	}
	public void testSelectNewEntityConstructorListFailureExpected() throws Exception {
		super.testMultiSelectNewMapList();
	}
}
