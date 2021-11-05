package org.hibernate.test.lazyload;

import org.hibernate.LazyInitializationException;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.persistence.*;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.*;

@TestForIssue(jiraKey = "HHH-14839")
@RunWith(BytecodeEnhancerRunner.class)
public class LazyLoadingIndexing2Test extends LazyLoadingIndexing1Test {

}
