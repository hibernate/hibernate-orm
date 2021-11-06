package org.hibernate.test.lazyload;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.junit.runner.RunWith;

@TestForIssue(jiraKey = "HHH-14839")
@RunWith(BytecodeEnhancerRunner.class)
public class LazyLoadingIndexing2Test extends LazyLoadingIndexing1Test {

}
