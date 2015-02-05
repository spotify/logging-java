package com.spotify.logging;

import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Order;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;

/**
 * Loads a logging configuration that is used until any of the {@link LoggingConfigurator}
 * configuration methods are called.
 *
 * Note: The @Plugin annotation documentation states that "Larger numbers indicate lower priority"
 * but the opposite seems to be true. Thus we set the @Order of this plugin to 0 in order to be
 * picked up after the standard json/yaml/xml configuration factory plugins.
 */
@Plugin(name = "BootstrapConfigurationFactory", category = "ConfigurationFactory")
@Order(0)
class BootstrapConfigurationFactory extends ConfigurationFactory {

  /**
   * Handle .xml as well to avoid log4j2.xml.default being used instead of log4j2.xml in case the
   * plugin ordering logic changes.
   */
  private static final String[] SUFFIXES = {".xml", ".xml.com.spotify.logging.default"};

  @Override
  public Configuration getConfiguration(final ConfigurationSource source) {
    return new XmlConfiguration(source);
  }

  @Override
  public String[] getSupportedTypes() {
    return SUFFIXES;
  }
}