package cm.domeni.authentis_users.e2e;

import static io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

import org.junit.jupiter.api.Tag;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "cm.domeni.authentis_users.e2e")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "@e2e")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty,summary")
@Tag("e2e")
public class CucumberE2ETest {}
