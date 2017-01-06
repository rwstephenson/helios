package com.spotify.helios.cli;

import static com.google.common.base.Optional.fromNullable;
import static com.spotify.helios.common.descriptors.PortMapping.TCP;
import static java.util.regex.Pattern.compile;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.spotify.helios.common.descriptors.PortMapping;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


class PortMappingParser {

  private static final Pattern PATTERN =
      compile("(?<n>[_\\-\\w]+)=(?<i>\\d+)(:(?<e>\\d+))?(/(?<p>\\w+))?");
//  private static final Pattern PATTERN =
//      compile("(?<n>[_\\-\\w]+)=((?<ip>(localhost|([0-9]{1,3}.){3}[0-9]{1,3})):)?(?<i>\\d+)(:(?<e>\\d+))?(/(?<p>\\w+))?");

  static PortMapping parsePortMapping(final String portSpec) {
    final Matcher matcher = PATTERN.matcher(portSpec);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Bad port mapping: " + portSpec);
    }

    final String portName = matcher.group("n");
//    final InetAddress ipAddress = matcher.group("ip") == null
//                                  ? null : InetAddresses.forString(matcher.group("ip"));
    final int internal = Integer.parseInt(matcher.group("i"));
    final Integer external = nullOrInteger(matcher.group("e"));
    final String protocol = fromNullable(matcher.group("p")).or(TCP);

    return PortMapping.of(portName, internal, external, protocol);
  }

  static Map<String, PortMapping> parsePortMappings(final List<String> portSpecs) {
    final Map<String, PortMapping> explicitPorts = Maps.newHashMap();

    for (final String spec : portSpecs) {
      final PortMapping portMapping = parsePortMapping(spec);

      final String portName = portMapping.getName();
      if (explicitPorts.containsKey(spec)) {
        throw new IllegalArgumentException("Duplicate port mapping: " + spec);
      }

      explicitPorts.put(portName, portMapping);
    }

    return ImmutableMap.copyOf(explicitPorts);
  }

  private static Integer nullOrInteger(final String str) {
    return str == null ? null : Integer.valueOf(str);
  }
}
