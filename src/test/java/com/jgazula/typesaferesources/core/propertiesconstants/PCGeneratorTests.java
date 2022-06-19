package com.jgazula.typesaferesources.core.propertiesconstants;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jgazula.typesaferesources.core.internal.classgeneration.ClassGenerator;
import com.jgazula.typesaferesources.core.internal.classgeneration.ClassGeneratorFactory;
import com.jgazula.typesaferesources.core.internal.classgeneration.PoetClassGeneratorConfig;
import com.jgazula.typesaferesources.core.internal.properties.PropertiesParser;
import com.jgazula.typesaferesources.core.internal.properties.PropertiesReader;
import com.jgazula.typesaferesources.core.internal.util.FileUtil;
import com.jgazula.typesaferesources.core.testutil.TestConstants;
import com.jgazula.typesaferesources.core.testutil.TestHelper;
import com.jgazula.typesaferesources.core.util.ValidationException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PCGeneratorTests {

  @Mock private ClassGeneratorFactory generatorFactory;

  @Mock private PropertiesReader propertiesReader;

  @Mock private PropertiesParser propertiesParser;

  @Mock private FileUtil fileUtil;

  @Test
  public void nothingToGenerateWhenNoFileConfigs() throws IOException {
    // given
    Path destinationDir = Paths.get(TestConstants.DESTINATION_DIR);
    PCConfig config = PCConfig.builder().destinationDir(destinationDir).build();

    // when
    new PCGenerator(config, generatorFactory, propertiesReader, propertiesParser, fileUtil)
        .generate();

    // then
    verify(generatorFactory, never()).getGenerator(any(PoetClassGeneratorConfig.class));
  }

  @Test
  public void nothingToGenerateWhenPropertiesFileIsEmpty() throws IOException {
    // given
    Path propertiesPath = Paths.get(TestConstants.TEST_PROPERTIES_FILE);
    Path destinationDir = Paths.get(TestConstants.DESTINATION_DIR);

    when(fileUtil.exists(propertiesPath)).thenReturn(true);

    PCFileConfig fileConfig =
        PCFileConfig.builder()
            .generatedPackageName(TestConstants.TEST_PACKAGE_NAME)
            .generatedClassName(TestConstants.TEST_CLASS_NAME)
            .propertiesPath(propertiesPath)
            .build();

    PCConfig config =
        PCConfig.builder()
            .fileConfigs(Collections.singletonList(fileConfig))
            .destinationDir(destinationDir)
            .build();

    ClassGenerator classGenerator = mock(ClassGenerator.class);
    when(generatorFactory.getGenerator(any(PoetClassGeneratorConfig.class)))
        .thenReturn(classGenerator);

    when(propertiesReader.loadProperties(propertiesPath)).thenReturn(Collections.emptyMap());

    // when
    new PCGenerator(config, generatorFactory, propertiesReader, propertiesParser, fileUtil)
        .generate();

    // then
    verify(classGenerator, never()).addPublicConstantString(anyString(), anyString());
    verify(classGenerator, never()).write(destinationDir);
  }

  @Test
  public void validateNonExistentPropertiesFile() {
    // given
    Path propertiesPath = Paths.get(TestConstants.TEST_PROPERTIES_FILE);
    Path destinationDir = Paths.get(TestConstants.DESTINATION_DIR);

    when(fileUtil.exists(propertiesPath)).thenReturn(false);

    PCFileConfig fileConfig =
        PCFileConfig.builder()
            .generatedPackageName(TestConstants.TEST_PACKAGE_NAME)
            .generatedClassName(TestConstants.TEST_CLASS_NAME)
            .propertiesPath(propertiesPath)
            .build();

    PCConfig config =
        PCConfig.builder()
            .fileConfigs(Collections.singletonList(fileConfig))
            .destinationDir(destinationDir)
            .build();

    // when
    PropertiesConstants generator =
        new PCGenerator(config, generatorFactory, propertiesReader, propertiesParser, fileUtil);

    // then
    assertThatExceptionOfType(ValidationException.class).isThrownBy(generator::generate);
  }

  @Test
  public void successfullyGenerateSingleFile() throws IOException {
    // given
    Path destinationDir = Paths.get(TestConstants.DESTINATION_DIR);

    Path propertiesPath = Paths.get(TestConstants.TEST_PROPERTIES_FILE);
    when(fileUtil.exists(propertiesPath)).thenReturn(true);

    Map<String, String> properties = TestHelper.generateProperties();
    PCFileConfig fileConfig =
        PCFileConfig.builder()
            .generatedPackageName(TestConstants.TEST_PACKAGE_NAME)
            .generatedClassName(TestConstants.TEST_CLASS_NAME)
            .propertiesPath(propertiesPath)
            .build();

    PCConfig config =
        PCConfig.builder()
            .fileConfigs(Collections.singletonList(fileConfig))
            .destinationDir(destinationDir)
            .build();

    ClassGenerator classGenerator = mock(ClassGenerator.class);
    when(generatorFactory.getGenerator(any(PoetClassGeneratorConfig.class)))
        .thenReturn(classGenerator);

    when(propertiesReader.loadProperties(propertiesPath)).thenReturn(properties);
    when(propertiesParser.keyToStaticFinalVariable(anyString())).thenCallRealMethod();

    // when
    new PCGenerator(config, generatorFactory, propertiesReader, propertiesParser, fileUtil)
        .generate();

    // then
    verify(propertiesParser, times(properties.size())).keyToStaticFinalVariable(anyString());
    verify(classGenerator, times(properties.size()))
        .addPublicConstantString(anyString(), anyString());
    verify(classGenerator).write(destinationDir);
  }

  @Test
  public void successfullyGenerateMultipleFiles() throws IOException {
    // given
    Path destinationDir = Paths.get(TestConstants.DESTINATION_DIR);

    Path properties1Path = Paths.get(TestConstants.TEST_PROPERTIES_FILE);
    when(fileUtil.exists(properties1Path)).thenReturn(true);
    Map<String, String> properties1 = TestHelper.generateProperties();
    PCFileConfig fileConfig1 =
        PCFileConfig.builder()
            .generatedPackageName(TestConstants.TEST_PACKAGE_NAME)
            .generatedClassName(TestConstants.TEST_CLASS_NAME)
            .propertiesPath(properties1Path)
            .build();

    Path properties2Path = Paths.get(TestConstants.TEST_PROPERTIES_FILE2);
    when(fileUtil.exists(properties2Path)).thenReturn(true);
    Map<String, String> properties2 = TestHelper.generateProperties();
    PCFileConfig fileConfig2 =
        PCFileConfig.builder()
            .generatedPackageName(TestConstants.TEST_PACKAGE_NAME2)
            .generatedClassName(TestConstants.TEST_CLASS_NAME2)
            .propertiesPath(properties2Path)
            .build();

    Path properties3Path = Paths.get(TestConstants.TEST_PROPERTIES_FILE3);
    when(fileUtil.exists(properties3Path)).thenReturn(true);
    Map<String, String> properties3 = TestHelper.generateProperties();
    PCFileConfig fileConfig3 =
        PCFileConfig.builder()
            .generatedPackageName(TestConstants.TEST_PACKAGE_NAME3)
            .generatedClassName(TestConstants.TEST_CLASS_NAME3)
            .propertiesPath(properties3Path)
            .build();

    List<PCFileConfig> fileConfigs = Arrays.asList(fileConfig1, fileConfig2, fileConfig3);
    PCConfig config =
        PCConfig.builder().fileConfigs(fileConfigs).destinationDir(destinationDir).build();

    ClassGenerator classGenerator = mock(ClassGenerator.class);
    when(generatorFactory.getGenerator(any(PoetClassGeneratorConfig.class)))
        .thenReturn(classGenerator);

    when(propertiesReader.loadProperties(properties1Path)).thenReturn(properties1);
    when(propertiesReader.loadProperties(properties2Path)).thenReturn(properties2);
    when(propertiesReader.loadProperties(properties3Path)).thenReturn(properties3);

    when(propertiesParser.keyToStaticFinalVariable(anyString())).thenCallRealMethod();

    // when
    new PCGenerator(config, generatorFactory, propertiesReader, propertiesParser, fileUtil)
        .generate();

    // then
    int totalNumProperties = properties1.size() + properties2.size() + properties3.size();
    verify(propertiesParser, times(totalNumProperties)).keyToStaticFinalVariable(anyString());
    verify(classGenerator, times(totalNumProperties))
        .addPublicConstantString(anyString(), anyString());
    verify(classGenerator, times(fileConfigs.size())).write(destinationDir);
  }
}