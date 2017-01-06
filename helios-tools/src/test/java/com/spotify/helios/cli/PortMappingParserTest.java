package com.spotify.helios.cli;

import static com.spotify.helios.common.descriptors.PortMapping.TCP;
import static com.spotify.helios.common.descriptors.PortMapping.UDP;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.spotify.helios.common.descriptors.PortMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class PortMappingParserTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final List<TestData> GOOD_SPECS = ImmutableList.of(
      TestData.of("http=8080:80", "http", PortMapping.of(8080, 80, TCP)),
      TestData.of("foo=4711:80/tcp", "foo", PortMapping.of(4711, 80, TCP)),
      TestData.of("dns=53:53/udp", "dns", PortMapping.of(53, 53, UDP)),
      TestData.of("foo=4711", "foo", PortMapping.of(4711, null, TCP))
//      TestData.of("dns=localhost:53:53/udp", "dns", InetAddresses.forString("localhost"), 53, 53, UDP),
//      TestData.of("mail=127.0.0.1:23:23/tcp", "mail", InetAddresses.forString("127.0.0.1"), 23, 23, TCP),
//      TestData.of("bar=10.99.0.1:1001:1002", "bar", InetAddresses.forString("10.99.0.1"), 1001, 1002, TCP)
  );

  private static final List<TestData> BAD_SPECS = ImmutableList.of(
      TestData.partialFromSpec("http8080:80"),
      TestData.partialFromSpec("foo=80:4711:80/tcp"),
      TestData.partialFromSpec("=53:53/udp"),
      TestData.partialFromSpec("foo")
  );

  @Test
  public void testParsePortMappingGoodSpecs() throws Exception {
    for (final TestData d : GOOD_SPECS) {
      final PortMapping portMapping = PortMappingParser.parsePortMapping(d.getSpec());
//      assertThat(portMapping.getName(), equalTo(d.getPortName()));
//      assertThat(portMapping.getIpAddress(), equalTo(d.getIp()));
      assertThat(portMapping.getInternalPort(), equalTo(d.getPortMapping().getInternalPort()));
      assertThat(portMapping.getExternalPort(), equalTo(d.getPortMapping().getExternalPort()));
      assertThat(portMapping.getProtocol(), equalTo(d.getPortMapping().getProtocol()));
    }
  }

  @Test
  public void testParsePortMappingBadSpecs() throws Exception {
    for (final TestData d : BAD_SPECS) {
      expectedException.expect(IllegalArgumentException.class);
      expectedException.expectMessage("Bad port mapping: " + d.getSpec());
      PortMappingParser.parsePortMapping(d.getSpec());
    }
  }

  @Test
  public void parsePortMappings() throws Exception {
    final Map<String, PortMapping> mappings =
        PortMappingParser.parsePortMappings(testDataToSpecs(GOOD_SPECS));
    final Map<String, PortMapping> expectedMappings = new HashMap<>();
    for (final TestData d : GOOD_SPECS) {
      expectedMappings.put(d.getPortName(), d.getPortMapping());
    }
    assertThat(mappings, equalTo(expectedMappings));
  }

  private List<String> testDataToSpecs(final List<TestData> testData) {
    final ImmutableList.Builder<String> specs = ImmutableList.builder();
    for (final TestData d : testData) {
      specs.add(d.getSpec());
    }
    return specs.build();
  }

  private static class TestData {

    private final String spec;
    private final String portName;
//    private final InetAddress ip;
    private final PortMapping portMapping;

    private TestData(final String spec, final String portName,
                     //final InetAddress ip,
                     final PortMapping portMapping) {
      this.spec = spec;
      this.portName = portName;
//      this.ip = ip;
      this.portMapping = portMapping;
    }

    public static TestData partialFromSpec(final String spec) {
      return new TestData(spec, null, null);
    }

    public static TestData of(final String spec, final String portName,
                              final PortMapping portMapping) {
                              //final InetAddress ip
      return new TestData(spec, portName, portMapping);
    }

    String getSpec() {
      return spec;
    }

    String getPortName() {
      return portName;
    }
//
//    InetAddress getIp() {
//      return ip;
//    }

    PortMapping getPortMapping() {
      return portMapping;
    }
  }
}