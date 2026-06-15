/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.jakarta.data.tck.runner.test;

import ee.jakarta.tck.data.framework.read.only.AsciiCharacter;
import ee.jakarta.tck.data.framework.read.only.NaturalNumber;
import ee.jakarta.tck.data.framework.read.only._AsciiCharacters;
import ee.jakarta.tck.data.framework.read.only._CustomRepository;
import ee.jakarta.tck.data.framework.read.only._NaturalNumbers;
import ee.jakarta.tck.data.framework.read.only._PositiveIntegers;
import ee.jakarta.tck.data.standalone.entity.Box;
import ee.jakarta.tck.data.standalone.entity.Coordinate;
import ee.jakarta.tck.data.standalone.entity.EntityTests;
import ee.jakarta.tck.data.standalone.entity._Boxes;
import ee.jakarta.tck.data.standalone.entity._MultipleEntityRepo;

import org.hibernate.orm.jakarta.data.tck.runner.DataTck;

@DataTck(
		domainClasses = {Box.class, Coordinate.class, AsciiCharacter.class, NaturalNumber.class},
		repositoryClasses = {_Boxes.class, _MultipleEntityRepo.class, _CustomRepository.class,
				_AsciiCharacters.class, _NaturalNumbers.class, _PositiveIntegers.class}
)
public class StandaloneEntityTests extends EntityTests {
}
