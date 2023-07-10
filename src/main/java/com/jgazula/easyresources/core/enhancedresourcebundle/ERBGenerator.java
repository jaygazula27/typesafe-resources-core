package com.jgazula.easyresources.core.enhancedresourcebundle;

import com.jgazula.easyresources.core.internal.classgeneration.ClassGenerator;
import com.jgazula.easyresources.core.internal.classgeneration.ClassGeneratorConfig;
import com.jgazula.easyresources.core.internal.classgeneration.ClassGeneratorFactory;
import com.jgazula.easyresources.core.internal.properties.PropertiesReader;
import com.jgazula.easyresources.core.internal.util.FileUtil;
import com.jgazula.easyresources.core.util.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;
import java.text.ChoiceFormat;
import java.text.DateFormat;
import java.text.Format;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

class ERBGenerator implements EnhancedResourceBundle {

    private static final Logger LOGGER = LoggerFactory.getLogger(ERBGenerator.class);

    private static final String RESOURCE_BUNDLE_VARIABLE_NAME = "resourceBundle";

    private final ERBConfig config;
    private final FileUtil fileUtil;
    private final ClassGeneratorFactory generatorFactory;
    private final PropertiesReader propertiesReader;
    private final MessageFormat messageFormat;

    ERBGenerator(ERBConfig config, FileUtil fileUtil, ClassGeneratorFactory generatorFactory,
                 PropertiesReader propertiesReader, MessageFormat messageFormat) {
        this.config = config;
        this.fileUtil = fileUtil;
        this.generatorFactory = generatorFactory;
        this.propertiesReader = propertiesReader;
        this.messageFormat = messageFormat;
    }

    @Override
    public void generate() throws IOException {
        if (config.bundleConfigs().isEmpty()) {
            LOGGER.warn("No resource bundles have been configured. Skipping constants file generation.");
            return;
        }

        for (ERBBundleConfig bundleConfig : config.bundleConfigs()) {
            generateEnhancedResourceBundle(bundleConfig);
        }
    }

    private void generateEnhancedResourceBundle(ERBBundleConfig bundleConfig) throws IOException {
        LOGGER.debug("Generating enhanced resource bundle for {}", bundleConfig.bundlePath());

        if (!fileUtil.exists(Paths.get(bundleConfig.bundlePath().toString(), bundleConfig.bundleName()))) {
            throw new ValidationException("Resource bundle %s not found in %s", bundleConfig.bundleName(),
                    bundleConfig.bundlePath().toString());
        }

        ResourceBundle bundle = propertiesReader.getBundle(bundleConfig.bundleName(), bundleConfig.bundlePath());
        LOGGER.debug("Successfully loaded {} resource bundle in {}", bundleConfig.bundleName(), bundleConfig.bundlePath());

        Set<String> keys = bundle.keySet();
        if (keys.isEmpty()) {
            LOGGER.warn("The resource bundle {} is empty. Skipping enhancing of resource bundle.", bundleConfig.bundleName());
        } else {
            var poetConfig = ClassGeneratorConfig.builder()
                    .generatedBy(config.generatedBy())
                    .packageName(bundleConfig.generatedPackageName())
                    .className(bundleConfig.generatedClassName())
                    .build();
            ClassGenerator generator = generatorFactory.getGenerator(poetConfig);

            // create the private field to store the resource bundle that'll be initialized in the constructor
            generator.addPrivateFinalField(ResourceBundle.class, RESOURCE_BUNDLE_VARIABLE_NAME);

            // create constructor which takes in ResourceBundle as an argument
            generator.addConstructorWithArgs(Map.of(ResourceBundle.class, RESOURCE_BUNDLE_VARIABLE_NAME));

            for (String key : keys) {
                String value = bundle.getString(key);
                messageFormat.applyPattern(value);

                for (Format format : messageFormat.getFormatsByArgumentIndex()) {
                    if (format instanceof NumberFormat) {
                        // handles number and choice format types
                        // treat it as a long
                    } else if (format instanceof DateFormat) {
                        // handles date and time format types
                        // treat it as a Date
                    } else {
                        // no format type is given
                        // treat it as a String
                    }
                }
            }
        }
    }
}