package com.lloyds.transaction;

import static io.cucumber.core.options.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.core.options.Constants.PLUGIN_PROPERTY_NAME;

import io.cucumber.junit.CucumberOptions;
import io.cucumber.junit.platform.engine.Cucumber;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;
import org.springframework.boot.test.context.SpringBootTest;

@Cucumber
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@CucumberOptions(
        features = "src/test/resources/features/transaction.feature",
        glue = "com.lloyds.transaction"
)
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("com/lloyds/transaction")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.lloyds.transaction")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "io.cucumber.core.plugin.SerenityReporterParallel, pretty, timeline:target/test-results/timeline")
public class RunCucumberTest {
}
